package org.eol.globi.taxon;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.HttpUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NCBIService implements PropertyEnricher {

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        // see http://www.ncbi.nlm.nih.gov/books/NBK25500/
        Map<String, String> enriched = new HashMap<String, String>(properties);
        String externalId = properties.get(PropertyAndValueDictionary.EXTERNAL_ID);
        String prefixAlt = TaxonomyProvider.NCBITaxon.getIdPrefix();
        if (StringUtils.startsWith(externalId, TaxonomyProvider.NCBI.getIdPrefix())
                || StringUtils.startsWith(externalId, prefixAlt)) {
            String tsn = externalId
                    .replace(prefixAlt, "")
                    .replace(TaxonomyProvider.ID_PREFIX_NCBI, "");
            if (tsn.matches("\\d+")) {
                String fullHierarchy = getResponse("db=taxonomy&id=" + tsn);
                if (fullHierarchy.contains("<Taxon>")) {
                    parseAndPopulate(enriched, tsn, fullHierarchy);
                }
            }
        }
        return enriched;
    }

    protected void parseAndPopulate(Map<String, String> enriched, String tsn, String fullHierarchy) throws PropertyEnricherException {
        enriched.put(PropertyAndValueDictionary.EXTERNAL_ID, TaxonomyProvider.ID_PREFIX_NCBI + tsn);

        List<String> taxonNames = getPathElems(fullHierarchy, "ScientificName", "");
        String taxonNamesString = StringUtils.join(taxonNames, CharsetConstant.SEPARATOR);
        enriched.put(PropertyAndValueDictionary.PATH, taxonNamesString);

        List<String> rankNames = getPathElems(fullHierarchy, "Rank", "");
        String rankNamesString = StringUtils.join(rankNames, CharsetConstant.SEPARATOR);
        enriched.put(PropertyAndValueDictionary.PATH_NAMES, rankNamesString.replaceAll("no rank", ""));

        List<String> taxonPathIds = getPathElems(fullHierarchy, "TaxId", TaxonomyProvider.ID_PREFIX_NCBI);
        String taxonPathIdsString = StringUtils.join(taxonPathIds, CharsetConstant.SEPARATOR);
        enriched.put(PropertyAndValueDictionary.PATH_IDS, taxonPathIdsString);

        String genBankCommonName = XmlUtil.extractPath(fullHierarchy, "GenbankCommonName", "", " @en");
        String commonName = XmlUtil.extractPath(fullHierarchy, "CommonName", "", " @en");
        enriched.put(PropertyAndValueDictionary.COMMON_NAMES, commonName + CharsetConstant.SEPARATOR + genBankCommonName);

        setPropertyToFirstValue(PropertyAndValueDictionary.NAME, taxonNames, enriched);
        setPropertyToFirstValue(PropertyAndValueDictionary.RANK, rankNames, enriched);
    }

    private List<String> getPathElems(String fullHierarchy, String elementName, String valuePrefix) throws PropertyEnricherException {
        List<String> taxonNames = extractTaxonPath(fullHierarchy, elementName, valuePrefix, "//LineageEx");
        List<String> focalName = extractTaxonPath(fullHierarchy, elementName, valuePrefix, "/TaxaSet");
        taxonNames.addAll(focalName);
        return taxonNames;
    }

    protected void setPropertyToFirstValue(String propertyName, List<String> pathElems, Map<String, String> enriched) {
        if (pathElems != null && pathElems.size() > 0) {
            enriched.put(propertyName, pathElems.get(pathElems.size()-1));
        }
    }

    private static List<String> extractTaxonPath(String xmlContent, String elementName, String valuePrefix, String xpathPrefix) throws PropertyEnricherException {
        List<String> pathElem = new ArrayList<>();

        try {
            InputStream is = IOUtils.toInputStream(xmlContent, "UTF-8");
            String xpathExpr = xpathPrefix + "/Taxon/*[local-name() = '" + elementName + "']";
            NodeList nodes = (NodeList) XmlUtil.applyXPath(is, xpathExpr, XPathConstants.NODESET);

            for (int i = 0; i < nodes.getLength(); ++i) {
                Node item = nodes.item(i);
                Node firstChild = item.getFirstChild();
                if (null != firstChild) {
                    String nodeValue = firstChild.getNodeValue();
                    if (StringUtils.isNotBlank(nodeValue)) {
                        pathElem.add(valuePrefix + nodeValue + "");
                    }
                }
            }

            return pathElem;
        } catch (Exception var12) {
            throw new PropertyEnricherException("failed to handle response [" + xmlContent + "]", var12);
        }
    }


    protected String firstWillBeLast(String taxonNames) {
        String transformedNames = taxonNames;
        if (taxonNames != null) {
            String[] split1 = CSVTSVUtil.splitPipes(taxonNames);
            List<String> list1 = Arrays.asList(split1);
            Collections.rotate(list1, -1);
            transformedNames = StringUtils.join(list1, CharsetConstant.SEPARATOR);
            transformedNames = transformedNames.replaceAll("\\s+", " ").trim();
        }
        return transformedNames;
    }

    private String getResponse(String queryString) throws PropertyEnricherException {
        URI uri;
        try {
            uri = new URI("https", null, "eutils.ncbi.nlm.nih.gov", 443, "/entrez/eutils/efetch.fcgi", queryString, null);
        } catch (URISyntaxException e) {
            throw new PropertyEnricherException("failed to create uri", e);
        }
        HttpGet get = new HttpGet(uri);

        BasicResponseHandler responseHandler = new BasicResponseHandler();
        String response;
        try {
            response = HttpUtil.executeWithTimer(get, responseHandler);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to execute query to [" + uri.toString() + "]", e);
        }
        return response;
    }

    public void shutdown() {

    }
}
