package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.HttpTimedUtil;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.HttpUtil;
import org.globalbioticinteractions.nomer.util.PropertyEnricherInfo;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@PropertyEnricherInfo(name = "itis-taxon-id-web", description = "Use itis webservice to lookup taxa by id using ITIS:* prefix.")
public class ITISService extends PropertyEnricherSimple {

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enriched = new HashMap<String, String>(properties);
        String externalId = properties.get(PropertyAndValueDictionary.EXTERNAL_ID);
        if (isNumericITISTsn(externalId)) {
            enrichWithId(enriched, stripPrefix(externalId));
        }
        return enriched;
    }

    private void enrichWithId(Map<String, String> enriched, String tsn) throws PropertyEnricherException {
        String acceptedResponse = getResponse("getAcceptedNamesFromTSN", "tsn=" + tsn);
        String[] split = StringUtils.splitByWholeSeparator(acceptedResponse, "acceptedTsn>");
        if (split != null && split.length > 1) {
            tsn = split[1].split("<")[0];
        }
        String fullHierarchy = getResponse("getFullHierarchyFromTSN", "tsn=" + tsn);
        final String taxonId = TaxonomyProvider.ID_PREFIX_ITIS + tsn;
        enriched.put(PropertyAndValueDictionary.EXTERNAL_ID, taxonId);
        final List<String> pathIds = XmlUtil.extractPathNoJoin(fullHierarchy, "tsn", "ITIS:");
        List<String> pathIdsTail = pathIds;
        if (pathIdsTail.size() > 1) {
            pathIdsTail = pathIds.subList(1, pathIds.size());
        }
        final int taxonIdIndex = pathIdsTail.lastIndexOf(taxonId);
        enriched.put(PropertyAndValueDictionary.PATH_IDS, subJoin(taxonIdIndex, pathIdsTail));

        String taxonNames = subJoin(taxonIdIndex, XmlUtil.extractPathNoJoin(fullHierarchy, "taxonName", ""));
        enriched.put(PropertyAndValueDictionary.PATH, taxonNames);

        String rankNames = subJoin(taxonIdIndex, XmlUtil.extractPathNoJoin(fullHierarchy, "rankName", ""));
        enriched.put(PropertyAndValueDictionary.PATH_NAMES, rankNames);

        setPropertyToLastValue(PropertyAndValueDictionary.NAME, taxonNames, enriched);
        setPropertyToLastValue(PropertyAndValueDictionary.RANK, rankNames, enriched);
    }

    private boolean isNumericITISTsn(String externalId) {
        return (TaxonomyProvider.ITIS.equals(ExternalIdUtil.taxonomyProviderFor(externalId)))
                && StringUtils.isNumeric(stripPrefix(externalId));
    }

    private String stripPrefix(String externalId) {
        return ExternalIdUtil.stripPrefix(TaxonomyProvider.ITIS, externalId);
    }

    private String subJoin(int taxonIdIndex, List<String> taxonNames) {
        List<String> subList = taxonNames;
        if (taxonIdIndex != -1 && taxonNames.size() > taxonIdIndex) {
            subList = taxonNames.subList(0, taxonIdIndex + 1);
        }
        return StringUtils.join(subList, CharsetConstant.SEPARATOR);
    }

    static void setPropertyToLastValue(String propertyName, String taxonNames, Map<String, String> enriched) {
        if (taxonNames != null) {
            String[] split1 = CSVTSVUtil.splitPipes(taxonNames);
            if (split1.length > 0) {
                enriched.put(propertyName, StringUtils.trim(split1[split1.length - 1]));
            }
        }
    }

    private String getResponse(String methodName, String queryString) throws PropertyEnricherException {
        URI uri;
        try {
            uri = new URI("https", null, "www.itis.gov", 443, "/ITISWebService/services/ITISService/" + methodName, queryString, null);
        } catch (URISyntaxException e) {
            throw new PropertyEnricherException("failed to create uri", e);
        }
        HttpGet get = new HttpGet(uri);

        BasicResponseHandler responseHandler = new BasicResponseHandler();
        String response;
        try {
            response = HttpTimedUtil.executeWithTimer(get, responseHandler);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to execute query to [" + uri.toString() + "]", e);
        }
        return response;
    }

    public void shutdown() {

    }
}
