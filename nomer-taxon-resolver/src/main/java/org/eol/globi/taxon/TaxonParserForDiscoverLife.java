package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
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

public class TaxonParserForDiscoverLife implements TaxonParser {

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
                    String name = StringUtils.trim(node.getTextContent());

                    String id = node.getAttributes().getNamedItem("href") == null
                            ? null
                            : StringUtils.trim(node
                            .getAttributes()
                            .getNamedItem("href")
                            .getTextContent()
                    );

                    TaxonImpl taxon = new TaxonImpl(
                            name,
                            id);

                    taxon.setRank("species");
                    TaxonImpl taxonForNode = getTaxonForNode(currentFamilyNode, taxon);
                    taxonForNode.setExternalId(StringUtils.prependIfMissing(id, DiscoverLifeService.URL_ENDPOINT_DISCOVER_LIFE));
                    listener.addTerm(taxonForNode);
                }

            }

        } catch (SAXException | ParserConfigurationException | XPathExpressionException e) {
            throw new IOException(e);
        }

        listener.finish();

    }

    private TaxonImpl getTaxonForNode(Node familyNode, Taxon t) {
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
