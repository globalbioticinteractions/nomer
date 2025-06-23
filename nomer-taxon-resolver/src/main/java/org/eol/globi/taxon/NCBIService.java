package org.eol.globi.taxon;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.ResourceServiceHTTP;
import org.globalbioticinteractions.nomer.util.PropertyEnricherInfo;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@PropertyEnricherInfo(name = "ncbi-taxon-id-web", description = "Lookup NCBI taxon by id with NCBI:* prefix using web apis.")
public class NCBIService extends PropertyEnricherSimple {

    private final ResourceService resourceService;

    public NCBIService(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        // see http://www.ncbi.nlm.nih.gov/books/NBK25500/
        Map<String, String> enriched = new HashMap<String, String>(properties);

        String tsn = getNCBITaxonId(properties);

        if (StringUtils.isNotBlank(tsn) && tsn.matches("^\\d+$")) {
            URI uri = getRequestURI("db=taxonomy&id=" + tsn + "&email=info%40globalbioticinteractions.org&tool=10.5281%2Fzenodo.1145474");
            try {
                InputStream retrieve = resourceService.retrieve(uri);
                String fullHierarchy = IOUtils.toString(retrieve, StandardCharsets.UTF_8);
                if (fullHierarchy.contains("<Taxon>")) {
                    parseAndPopulate(enriched, tsn, fullHierarchy);
                }
            } catch (IOException e) {
                throw new PropertyEnricherException("failed to retrieve [" + uri + "]", e);
            }
        }
        return enriched;
    }


    public static String getNCBITaxonId(Map<String, String> properties) {
        String externalId = properties.get(PropertyAndValueDictionary.EXTERNAL_ID);
        return getNCBIId(externalId);
    }

    public static String getNCBIId(String externalId) {
        final TaxonomyProvider taxonomyProvider = ExternalIdUtil.taxonomyProviderFor(externalId);
        return TaxonomyProvider.NCBI.equals(taxonomyProvider)
                ? ExternalIdUtil.stripPrefix(TaxonomyProvider.NCBI, externalId)
                : null;
    }


    void parseAndPopulate(Map<String, String> enriched, String tsn, String fullHierarchy) throws PropertyEnricherException {
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
        List<String> commonNames = XmlUtil.extractPathNoJoin(fullHierarchy, "CommonName", "", " @en");
        ArrayList<String> commonNamesAll = new ArrayList<String>(commonNames) {{
            add(genBankCommonName);
        }};
        String commonNamesJoined = StringUtils.join(commonNamesAll.stream().filter(StringUtils::isNotBlank).collect(Collectors.joining(CharsetConstant.SEPARATOR)));
        enriched.put(PropertyAndValueDictionary.COMMON_NAMES, commonNamesJoined);

        setPropertyToFirstValue(PropertyAndValueDictionary.NAME, taxonNames, enriched);
        setPropertyToFirstValue(PropertyAndValueDictionary.RANK, rankNames, enriched);
    }

    private List<String> getPathElems(String fullHierarchy, String elementName, String valuePrefix) throws PropertyEnricherException {
        List<String> taxonNames = extractTaxonPath(fullHierarchy, elementName, valuePrefix, "//LineageEx");
        List<String> focalName = extractTaxonPath(fullHierarchy, elementName, valuePrefix, "/TaxaSet");
        taxonNames.addAll(focalName);
        return taxonNames;
    }

    private void setPropertyToFirstValue(String propertyName, List<String> pathElems, Map<String, String> enriched) {
        if (pathElems != null && pathElems.size() > 0) {
            enriched.put(propertyName, pathElems.get(pathElems.size() - 1));
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

    private URI getRequestURI(String queryString) throws PropertyEnricherException {
        URI uri;
        try {
            uri = new URI("https", null, "eutils.ncbi.nlm.nih.gov", 443, "/entrez/eutils/efetch.fcgi", queryString, null);
        } catch (URISyntaxException e) {
            throw new PropertyEnricherException("failed to create uri", e);
        }
        return uri;
    }

    public void shutdown() {

    }
}
