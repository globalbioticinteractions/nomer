package org.eol.globi.taxon;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.util.DateUtil;
import org.hamcrest.core.Is;
import org.joda.time.DateTime;
import org.junit.Test;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

public class BatNamesUtilTest {


    public static final Pattern TAXON_NAME_PATTERN = Pattern.compile("(.*</span>)(?<authorship>.*)(<br/>)(.*<br/>)(?<commonNames>.*)(</p>)");

    @Test
    public void getExplorePageSaved() throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
        String htmlAsXmlString = IOUtils.toString(
                getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/batnames/explore.xml"),
                StandardCharsets.UTF_8
        );


        NodeList linkElem = (NodeList) XmlUtil.applyXPath(IOUtils.toInputStream(htmlAsXmlString, StandardCharsets.UTF_8)
                , "//a", XPathConstants.NODESET);

        assertThat(linkElem.getLength(), Is.is(1797));

    }

    @Test
    public void extractGenera() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        InputStream is = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/batnames/explore.xml");


        NodeList linkElem = (NodeList) XmlUtil.applyXPath(is, "//a", XPathConstants.NODESET);

        List<String> genera = new ArrayList<>();

        for (int i = 0; i < linkElem.getLength(); i++) {
            Node item = linkElem.item(i);
            Node href = item.getAttributes().getNamedItem("href");
            if (href != null && StringUtils.startsWith(href.getNodeValue(), "/genera")) {
                genera.add(StringUtils.trim(item.getTextContent()));
            }
        }

        assertThat(genera.size(), Is.is(1239));
        assertThat(genera, hasItem("Rhinolophus"));

    }

    @Test
    public void extractTaxonInfo() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        InputStream is = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/batnames/rhinolophus.xml");


        NodeList linkElem = (NodeList) XmlUtil.applyXPath(is, "//div[@class='taxon' and @id='species']", XPathConstants.NODESET);

        assertThat(linkElem.getLength(), Is.is(110));
        List<Taxon> taxa = new ArrayList<>();
        TermMatchListener listener = new TermMatchListener() {

            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                taxa.add(resolvedTaxon);
            }
        };

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

        assertThat(taxa.size(), Is.is(110));

        Taxon firstTaxon = taxa.get(0);
        assertThat(firstTaxon.getExternalId(), Is.is("https://batnames.org/species/Rhinolophus%20acuminatus"));
        assertThat(firstTaxon.getExternalUrl(), Is.is("https://batnames.org/species/Rhinolophus%20acuminatus"));
        assertThat(firstTaxon.getName(), Is.is("Rhinolophus acuminatus"));
        assertThat(firstTaxon.getAuthorship(), Is.is("Peters, 1871."));
        assertThat(firstTaxon.getCommonNames(), Is.is("Accuminate Horseshoe Bat @en"));
        assertThat(firstTaxon.getNameSource(), Is.is("Simmons, N.B. and A.L. Cirranello. 2022B. Bat Species of the World: A taxonomic and geographic database . Accessed on 07/21/2022."));
        assertThat(firstTaxon.getNameSourceURL(), Is.is("https://batnames.org/species/Rhinolophus%20acuminatus"));
        assertThat(firstTaxon.getNameSourceAccessedAt(), Is.is(notNullValue()));

    }

}

