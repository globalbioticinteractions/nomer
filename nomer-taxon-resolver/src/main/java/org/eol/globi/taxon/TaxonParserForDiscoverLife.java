package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.PropertyAndValueDictionary;
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

public class TaxonParserForDiscoverLife implements TaxonParser {

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

            focalTaxon.setExternalId(StringUtils.prependIfMissing(id, DiscoverLifeService.URL_ENDPOINT_DISCOVER_LIFE));
            listener.foundTaxonForTerm(null, focalTaxon, focalTaxon, NameType.SAME_AS);
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
                    relatedTaxon.setExternalId(DiscoverLifeService.URL_ENDPOINT_DISCOVER_LIFE_SEARCH
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

    @Override
    public void parse(InputStream is, TaxonImportListener listener) throws IOException {
        listener.start();
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
                            (requestId, providedTerm, resolvedTaxon, nameType) -> listener.addTerm(resolvedTaxon)
                    );
                }

            }

        } catch (SAXException | ParserConfigurationException | XPathExpressionException e) {
            throw new IOException(e);
        }

        listener.finish();

    }

    private static Taxon getTaxonForNode(Node familyNode, Taxon t) {
        TaxonImpl targetTaxon = new TaxonImpl();
        String familyId = StringUtils.trim(familyNode.getAttributes().getNamedItem("href").getTextContent());
        String familyName = StringUtils.trim(familyNode.getTextContent());
        TaxonUtil.copy(t, targetTaxon);

        List<String> path = new ArrayList<String>(DiscoverLifeService.PATH_STATIC) {{
            addAll(Arrays.asList(familyName, t.getName()));
        }};

        targetTaxon.setPath(StringUtils.join(path, CharsetConstant.SEPARATOR));

        List<String> pathIds = new ArrayList<String>(DiscoverLifeService.PATH_STATIC_IDS) {{
            addAll(Arrays.asList(DiscoverLifeService.URL_ENDPOINT_DISCOVER_LIFE + familyId, DiscoverLifeService.URL_ENDPOINT_DISCOVER_LIFE + t.getId()));
        }};

        targetTaxon.setPathIds(StringUtils.join(pathIds, CharsetConstant.SEPARATOR));

        targetTaxon.setPathNames(StringUtils.join(DiscoverLifeService.PATH_NAMES_STATIC, CharsetConstant.SEPARATOR));
        return targetTaxon;
    }

}
