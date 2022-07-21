package org.eol.globi.taxon;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.util.DateUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BatNamesUtil {

    private static final Logger LOG = LoggerFactory.getLogger(BatNamesUtil.class);


    public static final char NON_BREAKING_SPACE = '\u00A0';
    public static final Pattern TAXON_NAME_PATTERN = Pattern.compile("(.*</span>)(?<authorship>.*)(<br/>)(.*<br/>)(?<commonNames>.*)(</p>)");


    public static String toPatchedXmlString(String htmlAsXmlString) {
        Map<String, String> patches = new TreeMap<>();
        patches.put("'target=\"_blank'\"", "target=\"_blank\"");
        patches.put("'target=\"_blank\"", "target=\"_blank\"");
        patches.put("\"target=\"_blank\"","target=\"_blank\"");
        patches.put("target=_blank","target=\"_blank\"");
        patches.put(" target=\"_self\" '=\"\""," target=\"_blank\"");
        patches.put("8 target=\" _blank\"=\"\"","8\" target=\"_blank\"");
        patches.put("<a href=\"https://pure.mpg.de/pubman/faces/ViewItemOverviewPage.jsp?itemId=item_2214140' target=\" _blank\"=\"\">", "<a href=\"https://pure.mpg.de/pubman/faces/ViewItemOverviewPage.jsp?itemId=item_2214140\" target=\"_blank\">");

        String patchedXml = htmlAsXmlString;

        for (Map.Entry<String, String> replaceEntry : patches.entrySet()) {
            patchedXml = StringUtils.replace(patchedXml, replaceEntry.getKey(), replaceEntry.getValue());
        }
        return patchedXml.replace(NON_BREAKING_SPACE, ' ');
    }

    public static String getGenusXml(String genusName) throws IOException {
        String url = "https://batnames.org/genera/" + genusName;
        String htmlAsXmlString = HtmlUtil.getHtmlAsXmlString
                (url);
        LOG.info("indexing [" + url + "]");
        return toPatchedXmlString(htmlAsXmlString);
    }

    public static Collection<String> extractGenera(InputStream is) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
        NodeList linkElem = (NodeList) XmlUtil.applyXPath(is, "//a", XPathConstants.NODESET);

        Set<String> genera = new TreeSet<>();

        for (int i = 0; i < linkElem.getLength(); i++) {
            Node item = linkElem.item(i);
            Node href = item.getAttributes().getNamedItem("href");
            if (href != null && StringUtils.startsWith(href.getNodeValue(), "/genera")) {
                genera.add(StringUtils.trim(item.getTextContent()));
            }
        }
        return genera;
    }

    public static void parseTaxaForGenus(InputStream is, TermMatchListener listener) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
        NodeList linkElem = (NodeList) XmlUtil.applyXPath(is, "//div[@class='taxon' and @id='species']", XPathConstants.NODESET);

        String citation = "Simmons, N.B. and A.L. Cirranello. " + new DateTime().getYear() + ". Bat Species of the World: A taxonomic and geographic database</i>. Accessed on ." + DateUtil.nowDateString();
        if (linkElem.getLength() > 0) {
            NodeList citeElem = (NodeList) XmlUtil.applyXPath(linkElem.item(0), "//div[@class='cite']", XPathConstants.NODESET);
            if (citeElem.getLength() > 0) {
                citation = StringUtils.trim(RegExUtils.replaceAll(
                        StringUtils.replace(citeElem.item(0).getTextContent(), "Cite the database: ", ""), "\\s+", " ")
                );
            }

        }

        for (int i = 0; i < linkElem.getLength(); i++) {
            Node taxonNode = linkElem.item(i);

            NodeList names = (NodeList) XmlUtil.applyXPath(taxonNode, "p/span/b/i", XPathConstants.NODESET);

            Taxon taxon = new TaxonImpl();
            if (names.getLength() > 0) {
                Node nameNode = names.item(0);
                String trim = StringUtils.trim(nameNode.getTextContent());
                taxon.setName(trim);

                try {
                    URI speciesPage = new URI("https", "batnames.org", "/species/" + taxon.getName(), null);
                    String speciesUrl = speciesPage.toString();
                    taxon.setExternalId(speciesUrl);
                    taxon.setExternalUrl(speciesUrl);
                    taxon.setNameSourceAccessedAt(DateUtil.nowDateString());
                    taxon.setNameSourceURL(speciesUrl);
                    taxon.setNameSource(citation);
                } catch (URISyntaxException e) {
                    // ignore
                }

                Node parentNode = nameNode.getParentNode();
                if (parentNode != null) {
                    Node parentOfParent = parentNode.getParentNode();
                    if (parentOfParent != null) {
                        Node paragraphNode = parentOfParent.getParentNode();

                        try {
                            StringWriter writer = new StringWriter();
                            Transformer transformer = TransformerFactory.newInstance().newTransformer();
                            transformer.transform(new DOMSource(paragraphNode), new StreamResult(writer));
                            String rawXmlSnippet = writer.toString().replaceAll("\n", "");

                            Matcher matcher = TAXON_NAME_PATTERN.matcher(rawXmlSnippet);

                            if (matcher.matches()) {
                                taxon.setAuthorship(StringUtils.trim(matcher.group("authorship")));
                                String commonNames = StringUtils.trim(matcher.group("commonNames"));
                                if (StringUtils.isNoneBlank(commonNames)) {
                                    taxon.setCommonNames(commonNames + " @en");
                                }
                            }
                        } catch (TransformerException e) {
                            // opportunistic
                        }
                    }
                }

            }


            listener.foundTaxonForTerm(0L, taxon, NameType.HAS_ACCEPTED_NAME, taxon);
        }
    }
}
