package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.HttpUtil;
import org.globalbioticinteractions.nomer.util.PropertyEnricherInfo;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@PropertyEnricherInfo(name = "gbif-taxon-name", description = "Lookup taxon in GBIF by name")
public class GBIFNameService extends PropertyEnricherSimple {

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        // see http://www.gbif.org/developer/species
        Map<String, String> enrichedProperties = new HashMap<>(properties);

        final String taxonName = enrichedProperties.get(PropertyAndValueDictionary.NAME);
        if (StringUtils.isNotBlank(taxonName)) {
            enrichWithTaxonName(enrichedProperties, taxonName);
        }

        return enrichedProperties;
    }

    private void enrichWithTaxonName(Map<String, String> enriched, String taxonName) throws PropertyEnricherException {
        try {
            JsonNode jsonNode = getSpeciesInfoByName(taxonName);
            if (jsonNode != null) {
                if (jsonNode.has("acceptedKey")) {
                    jsonNode = getSpeciesInfo(jsonNode.get("acceptedKey").asText());
                }
                GBIFUtil.addTaxonNode(enriched, jsonNode);

                GBIFUtil.appendCommonNames(enriched, jsonNode);
            }

        } catch (IOException e) {
            throw new PropertyEnricherException("failed to lookup [" + taxonName + "]", e);
        }

    }

    private JsonNode getSpeciesInfoByName(String taxonName) throws IOException {
        String response = HttpUtil.getContent("http://api.gbif.org/v1/species?name=" + URLEncoder.encode(taxonName, "UTF-8"));
        return GBIFUtil.getFirstResult(response);
    }

    private JsonNode getSpeciesInfo(String gbifSpeciesId) throws IOException {
        String response = HttpUtil.getContent("http://api.gbif.org/v1/species/" + gbifSpeciesId);
        return new ObjectMapper().readTree(response);
    }


    public void shutdown() {

    }
}
