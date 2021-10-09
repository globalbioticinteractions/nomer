package org.eol.globi.taxon;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.TaxonUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class DiscoverLifeUtil {

    private static final List<String> PATH_STATIC = Arrays.asList("Animalia", "Arthropoda", "Insecta", "Hymenoptera");
    private static final String URL_ENDPOINT_DISCOVER_LIFE = "https://www.discoverlife.org";
    private static final String URL_ENDPOINT_DISCOVER_LIFE_SEARCH = URL_ENDPOINT_DISCOVER_LIFE +
            "/mp/20q?search=";
    private static final List<String> PATH_STATIC_IDS = PATH_STATIC
            .stream()
            .map(x -> StringUtils.prependIfMissing(x, URL_ENDPOINT_DISCOVER_LIFE_SEARCH))
            .collect(Collectors.toList());
    private static final List<String> PATH_NAMES_STATIC = Arrays.asList("kingdom", "phylum", "class", "order", "family", "species");
    static final String DISCOVER_LIFE_URL
            = URL_ENDPOINT_DISCOVER_LIFE +
            "/mp/20q" +
            "?act=x_checklist" +
            "&guide=Apoidea_species" +
            "&flags=HAS";
    private static final String BEE_NAMES = "/org/globalbioticinteractions/nomer/match/discoverlife/bees.xml.gz";

    public static String getBeeNamesAsXmlString() throws IOException {
        final WebClient webClient = new WebClient();

        final DomNode page = getBeePage(webClient);
        return page.asXml();
    }

    private static DomNode getBeePage(WebClient webClient) throws IOException {
        webClient
                .getOptions()
                .setUseInsecureSSL(true);

        return webClient.getPage(DISCOVER_LIFE_URL);
    }

    public static InputStream getStreamOfBees() throws IOException {
        return new GZIPInputStream(DiscoverLifeUtil.class
                .getResourceAsStream(BEE_NAMES)
        );
    }

    public static void parseNames(Node familyNameNode, Node nameNodeCandidate, TermMatchListener listener) throws XPathExpressionException {

        Taxon focalTaxon = null;

        Node currentNode = nameNodeCandidate.getParentNode();
        if (StringUtils.equals("i", currentNode.getNodeName())) {
            Map<String, String> taxonMap = new TreeMap<>();
            taxonMap.put("name", StringUtils.trim(nameNodeCandidate.getTextContent()));

            Node expectedTextNode = currentNode.getNextSibling();
            Node authorshipNode = expectedTextNode == null ? null : expectedTextNode.getNextSibling();

            if (authorshipNode != null) {
                enrichFromAuthorString(authorshipNode, taxonMap);
                taxonMap.put("status", "accepted");
            }

            String id = nameNodeCandidate.getAttributes().getNamedItem("href") == null
                    ? null
                    : StringUtils.trim(nameNodeCandidate
                    .getAttributes()
                    .getNamedItem("href")
                    .getTextContent());


            taxonMap.put("rank", "species");

            Taxon parsedTaxon = TaxonUtil.mapToTaxon(taxonMap);
            parsedTaxon.setExternalId(id);

            focalTaxon = familyNameNode == null
                    ? parsedTaxon
                    : getTaxonForNode(familyNameNode, parsedTaxon);

            currentNode = authorshipNode;

            focalTaxon.setExternalId(StringUtils.prependIfMissing(id, URL_ENDPOINT_DISCOVER_LIFE));
            listener.foundTaxonForTerm(null, focalTaxon, focalTaxon, NameType.HAS_ACCEPTED_NAME);
        }


        if (focalTaxon != null) {

            while ((currentNode = currentNode == null ? null : currentNode.getNextSibling()) != null) {

                if ("i".equals(currentNode.getNodeName())) {

                    Map<String, String> relatedName = new TreeMap<>();

                    enrichFromNameString(currentNode, relatedName);

                    currentNode = currentNode.getNextSibling();

                    enrichFromAuthorString(currentNode, relatedName);

                    String status = relatedName.get("status");
                    NameType nameType = NameType.SYNONYM_OF;
                    if (StringUtils.equals(status, "homonym")) {
                        nameType = NameType.NONE;
                    }


                    Taxon relatedTaxon = TaxonUtil.mapToTaxon(relatedName);
                    relatedTaxon.setExternalId(URL_ENDPOINT_DISCOVER_LIFE_SEARCH
                                    + StringUtils.replace(relatedTaxon.getName(), " ", "+"));
                    listener.foundTaxonForTerm(
                            null,
                            focalTaxon,
                            relatedTaxon,
                            nameType
                    );
                }
            }
        }
    }

    private static void enrichFromAuthorString(Node currentNode, Map<String, String> relatedName) {
        String scrubbedName = StringUtils.replace(
                StringUtils.trim(currentNode.getTextContent()), ";", "");

        String[] authorParts = StringUtils.split(scrubbedName, ",");

        if (authorParts.length > 1) {
            String authorName = StringUtils.trim(authorParts[0]);
            String authorYear = StringUtils.trim(authorParts[1]);
            relatedName.put("authorship", StringUtils.join(Arrays.asList(authorName, authorYear), ", "));
        }
    }

    public static void enrichFromNameString(Node currentNode, Map<String, String> relatedName) {
        String altName = StringUtils.trim(currentNode.getTextContent());

        String[] nameAndStatus = StringUtils.splitByWholeSeparatorPreserveAllTokens(altName, "_homonym");
        if (nameAndStatus.length == 2) {
            relatedName.put("status", "homonym");
        } else {
            relatedName.put("status", "synonym");
        }

        relatedName.put("name", nameAndStatus[0]);
    }

    public static void parse(InputStream is, TermMatchListener termMatchListener) throws IOException {
        NodeList o;
        try {
            o = (NodeList) XmlUtil.applyXPath(is, "//tr/td/b/a | //tr/td/i/a", XPathConstants.NODESET);

            Node currentFamilyNode = null;
            for (int i = 0; i < o.getLength(); i++) {
                Node node = o.item(i);

                boolean isSpeciesNode = StringUtils.equals("i", node.getParentNode().getNodeName());

                if (!isSpeciesNode) {
                    currentFamilyNode = node;
                }

                if (isSpeciesNode && currentFamilyNode != null) {
                    parseNames(
                            currentFamilyNode,
                            node,
                            termMatchListener
                    );
                }

            }

        } catch (SAXException | ParserConfigurationException | XPathExpressionException e) {
            throw new IOException(e);
        }
    }

    private static Taxon getTaxonForNode(Node familyNode, Taxon t) {
        TaxonImpl targetTaxon = new TaxonImpl();
        String familyId = StringUtils.trim(familyNode.getAttributes().getNamedItem("href").getTextContent());
        String familyName = StringUtils.trim(familyNode.getTextContent());
        TaxonUtil.copy(t, targetTaxon);

        List<String> path = new ArrayList<String>(PATH_STATIC) {{
            addAll(Arrays.asList(familyName, t.getName()));
        }};

        targetTaxon.setPath(StringUtils.join(path, CharsetConstant.SEPARATOR));

        List<String> pathIds = new ArrayList<String>(PATH_STATIC_IDS) {{
            addAll(Arrays.asList(URL_ENDPOINT_DISCOVER_LIFE + familyId, URL_ENDPOINT_DISCOVER_LIFE + t.getId()));
        }};

        targetTaxon.setPathIds(StringUtils.join(pathIds, CharsetConstant.SEPARATOR));

        targetTaxon.setPathNames(StringUtils.join(PATH_NAMES_STATIC, CharsetConstant.SEPARATOR));
        return targetTaxon;
    }
}
