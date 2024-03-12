package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.TaxonCacheListener;
import org.eol.globi.taxon.XmlUtil;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class PlaziTreatmentXMLLoader implements PlaziTreatmentLoader {


    @Override
    public void loadTreatment(InputStream is, TaxonCacheListener listener) {
        try {
            NodeList taxonElem = (NodeList) XmlUtil.applyXPath(is, "//treatment//taxonomicName", XPathConstants.NODESET);
            for (int i = 0; i < taxonElem.getLength() && i < 1; i++) {
                Node item = taxonElem.item(i);

                Node treatment = null;
                Node parent = item;
                while ((parent = parent.getParentNode()) != null) {
                    if (StringUtils.equals("treatment", parent.getNodeName())) {
                        treatment = parent;
                    }
                }

                Set<String> taxonIds = new TreeSet<>();
                if (treatment != null) {
                    addNonBlank(treatment, taxonIds, "ID-GBIF-Taxon", TaxonomyProvider.ID_PREFIX_GBIF);
                    addNonBlank(treatment, taxonIds, "LSID");
                    addNonBlank(treatment, taxonIds, "httpUri");
                }

                Taxon taxon = new TaxonImpl();
                String authorityName = getAttributeValueOrEmpty(item, "authorityName");
                String authorityYear = getAttributeValueOrEmpty(item, "authorityYear");
                taxon.setAuthorship(authorityName + ", " +  authorityYear);

                taxon.setName(item.getTextContent());

                for (String taxonId : taxonIds) {
                    Taxon copy = TaxonUtil.copy(taxon);
                    copy.setExternalId(taxonId);
                    listener.addTaxon(copy);
                }

            }

        } catch (SAXException | IOException | ParserConfigurationException | XPathExpressionException e) {
            e.printStackTrace();
        }


    }

    private void addNonBlank(Node treatment, Set<String> taxonIds, String attributeName) {
        addNonBlank(treatment, taxonIds, attributeName, "");
    }


        private void addNonBlank(Node treatment, Set<String> taxonIds, String attributeName, String prefix) {
        String gbifId = getAttributeValueOrEmpty(treatment, attributeName);
        if (StringUtils.isNotBlank(gbifId)) {
            taxonIds.add(prefix + gbifId);
        }
    }

    private String getAttributeValueOrEmpty(Node item, String attributeName) {
        NamedNodeMap attr = item.getAttributes();
        return attr.getNamedItem(attributeName) == null
                ? ""
                : attr.getNamedItem(attributeName).getTextContent();
    }
}
