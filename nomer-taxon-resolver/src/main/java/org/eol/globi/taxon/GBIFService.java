package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.HttpUtil;
import org.globalbioticinteractions.nomer.util.PropertyEnricherInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@PropertyEnricherInfo(name = "gbif-taxon-id", description = "Web-based taxon lookup by id using GBIF backbone API and GBIF:* prefix.")
public class GBIFService extends PropertyEnricherSimple {

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        // see http://www.gbif.org/developer/species
        Map<String, String> enriched = new HashMap<>(properties);
        String gbifSpeciesId = getGBIFTaxonId(enriched);

        if (StringUtils.isNotBlank(gbifSpeciesId)) {
            enrichWithExternalId(enriched, gbifSpeciesId);
        }
        return enriched;
    }

    public static String getGBIFTaxonId(Map<String, String> properties) {
        String externalId = properties.get(PropertyAndValueDictionary.EXTERNAL_ID);
        return getGBIFId(externalId);
    }

    public static String getGBIFId(String externalId) {
        final TaxonomyProvider taxonomyProvider = ExternalIdUtil.taxonomyProviderFor(externalId);
        String gbifSpeciesId = null;
        if (TaxonomyProvider.GBIF.equals(taxonomyProvider)) {
            gbifSpeciesId = ExternalIdUtil.stripPrefix(TaxonomyProvider.GBIF, externalId);
        }
        return gbifSpeciesId;
    }

    private void enrichWithExternalId(Map<String, String> enriched, String gbifSpeciesId) throws PropertyEnricherException {
        try {
            JsonNode jsonNode = getSpeciesInfo(gbifSpeciesId);
            if (jsonNode.has("acceptedKey")) {
                jsonNode = getSpeciesInfo(jsonNode.get("acceptedKey").asText());
            }
            GBIFUtil.addTaxonNode(enriched, jsonNode);

            GBIFUtil.appendCommonNames(enriched, jsonNode);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to lookup GBIF Species Id [" + gbifSpeciesId + "]", e);
        }

    }

    private JsonNode getSpeciesInfo(String gbifSpeciesId) throws IOException {
        String response = HttpUtil.getContent("http://api.gbif.org/v1/species/" + gbifSpeciesId);
        return new ObjectMapper().readTree(response);
    }

    public void shutdown() {

    }
}
