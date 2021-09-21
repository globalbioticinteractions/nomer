package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.ExternalIdUtil;
import org.globalbioticinteractions.nomer.util.PropertyEnricherInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@PropertyEnricherInfo(name = "gbif-taxon-web", description = "Web-based taxon id/name lookup using GBIF backbone API and GBIF:* prefix.")
public class GBIFService extends PropertyEnricherSimple {

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        // see http://www.gbif.org/developer/species
        Map<String, String> enriched = new HashMap<>(properties);
        String gbifSpeciesId = getGBIFTaxonId(enriched);

        if (StringUtils.isNotBlank(gbifSpeciesId)) {
            enrichWithExternalId(enriched, gbifSpeciesId);
        } else {
            final String taxonName = enriched.get(PropertyAndValueDictionary.NAME);
            if (StringUtils.isNotBlank(taxonName)) {
                GBIFUtil.enrichWithTaxonName(enriched, taxonName);
            }
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
            JsonNode jsonNode = GBIFUtil.getSpeciesInfo(gbifSpeciesId);
            if (jsonNode.has("acceptedKey")) {
                jsonNode = GBIFUtil.getSpeciesInfo(jsonNode.get("acceptedKey").asText());
            }
            GBIFUtil.addTaxonNode(enriched, jsonNode);

            GBIFUtil.appendCommonNames(enriched, jsonNode);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to lookup GBIF Species Id [" + gbifSpeciesId + "]", e);
        }

    }

    public void shutdown() {

    }
}
