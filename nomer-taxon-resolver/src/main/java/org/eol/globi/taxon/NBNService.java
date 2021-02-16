package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.HttpUtil;
import org.globalbioticinteractions.nomer.util.PropertyEnricherInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eol.globi.util.ExternalIdUtil.taxonomyProviderFor;

@PropertyEnricherInfo(name= "nbn-taxon-id", description = "Lookup taxon of National Biodiversity Network by id with NBN:* prefix.")
public class NBNService extends PropertyEnricherSimple {

    private static final Logger LOG = LoggerFactory.getLogger(NBNService.class);

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        // see https://data.nbn.org.uk/Documentation/Web_Services/Web_Services-REST/Getting_Records/
        Map<String, String> enriched = new HashMap<String, String>(properties);
        String externalId = properties.get(PropertyAndValueDictionary.EXTERNAL_ID);
        if (TaxonomyProvider.NBN.equals(taxonomyProviderFor(externalId))) {
            enrichWithExternalId(enriched, externalId);
        }
        return enriched;
    }

    private void enrichWithExternalId(Map<String, String> enriched, String externalId) throws PropertyEnricherException {
        List<String> ids = new ArrayList<String>();
        List<String> names = new ArrayList<String>();
        List<String> ranks = new ArrayList<String>();

        try {
            String nbnId = ExternalIdUtil.stripPrefix(TaxonomyProvider.NBN, externalId);
            String response = HttpUtil.getContent("https://species-ws.nbnatlas.org/classification/" + nbnId);
            JsonNode taxa = new ObjectMapper().readTree(response);
            for (JsonNode jsonNode : taxa) {
                String externalId1;
                externalId1 = jsonNode.has("guid") ? (TaxonomyProvider.NBN.getIdPrefix() + jsonNode.get("guid").asText()) : "";
                String name = jsonNode.has("scientificName") ? jsonNode.get("scientificName").asText() : "";
                String rank = jsonNode.has("rank") ? jsonNode.get("rank").asText() : "";
                enriched.put(PropertyAndValueDictionary.NAME, name);
                enriched.put(PropertyAndValueDictionary.RANK, rank);
                enriched.put(PropertyAndValueDictionary.EXTERNAL_ID, externalId1);

                ids.add(externalId1);
                names.add(name);
                ranks.add(rank);
            }

            response = HttpUtil.getContent("https://species-ws.nbnatlas.org/species/" + nbnId);
            if (StringUtils.isNotBlank(response)) {
                JsonNode jsonRootNode = new ObjectMapper().readTree(response);
                if (jsonRootNode.has("commonNames")) {
                    List<String> commonNames = new ArrayList<>();
                    for (JsonNode commonName : jsonRootNode.get("commonNames")) {
                        String name = commonName.has("nameString") ? commonName.get("nameString").asText() : "";
                        String language = commonName.has("language") ? commonName.get("language").asText() : "en";
                        commonNames.add(name + " @" + language);
                    }
                    if (commonNames.size() > 0) {
                        enriched.put(PropertyAndValueDictionary.COMMON_NAMES, StringUtils.join(commonNames, CharsetConstant.SEPARATOR));
                    }
                }
            } else {
                LOG.warn("empty response for nbn taxon lookup with id [" + nbnId + "]");
            }
        } catch (IOException e) {

            throw new PropertyEnricherException("failed to lookup [" + externalId + "]", e);
        }
        enriched.put(PropertyAndValueDictionary.PATH_IDS, toString(ids));
        enriched.put(PropertyAndValueDictionary.PATH, toString(names));
        enriched.put(PropertyAndValueDictionary.PATH_NAMES, toString(ranks));
    }

    protected String toString(List<String> ids) {
        return StringUtils.join(ids, CharsetConstant.SEPARATOR);
    }

    public void shutdown() {

    }
}
