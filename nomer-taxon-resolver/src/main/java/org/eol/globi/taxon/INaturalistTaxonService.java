package org.eol.globi.taxon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.HttpUtil;
import org.globalbioticinteractions.nomer.util.PropertyEnricherInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;

@PropertyEnricherInfo(name = "inaturalist-taxon-id", description = "Lookup taxon in iNaturalist by id with INAT_TAXON:* prefix.")
public class INaturalistTaxonService extends PropertyEnricherSimple {

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enriched = new HashMap<String, String>(properties);
        String externalId = properties.get(PropertyAndValueDictionary.EXTERNAL_ID);
        if (TaxonomyProvider.INATURALIST_TAXON
                .equals(ExternalIdUtil.taxonomyProviderFor(externalId))) {
            enrichWithExternalId(enriched, externalId);
        }
        return enriched;
    }

    protected void enrichWithExternalId(Map<String, String> enriched, String externalId) throws PropertyEnricherException {
        try {
            String taxonId = ExternalIdUtil.stripPrefix(TaxonomyProvider.INATURALIST_TAXON, externalId);
            JsonNode jsonNode = getSpeciesInfo(taxonId);
            if (jsonNode.has("results")) {
                JsonNode results = jsonNode.get("results");
                for (JsonNode result : results) {
                    TaxonImpl taxon = parseTaxon(result);
                    if (result.has("ancestors")) {
                        List<Taxon> path = new ArrayList<Taxon>();
                        for (JsonNode ancestor : result.get("ancestors")) {
                            Taxon ancestorTaxon = parseTaxon(ancestor);
                            path.add(ancestorTaxon);
                        }
                        path.add(taxon);
                        BinaryOperator<String> pathConcat = (a, e) -> StringUtils.isBlank(a) ? e : a + CharsetConstant.SEPARATOR + e;
                        taxon.setPath(path.stream().map(Taxon::getName).reduce("", pathConcat));
                        taxon.setPathIds(path.stream().map(Taxon::getExternalId).reduce("", pathConcat));
                        taxon.setPathNames(path.stream().map(Taxon::getRank).reduce("", pathConcat));
                    }

                    enriched.putAll(TaxonUtil.taxonToMap(taxon));
                }
            }

        } catch (IOException e) {
            throw new PropertyEnricherException("failed to lookup [" + externalId + "]", e);
        }

    }

    private TaxonImpl parseTaxon(JsonNode result) {
        TaxonImpl taxon = new TaxonImpl();
        if (result.has("name")) {
            taxon.setName(result.get("name").asText());
        }
        if (result.has("rank")) {
            taxon.setRank(result.get("rank").asText());
        }
        if (result.has("id")) {
            taxon.setExternalId(TaxonomyProvider.INATURALIST_TAXON.getIdPrefix() + result.get("id").asText());
        }
        if (result.has("preferred_common_name")) {
            taxon.setCommonNames(result.get("preferred_common_name").asText() + " @en");
        }
        return taxon;
    }

    private JsonNode getSpeciesInfo(String taxonId) throws IOException {
        String response = HttpUtil.getContent("https://api.inaturalist.org/v1/taxa/" + taxonId);
        return new ObjectMapper().readTree(response);
    }

    public void shutdown() {

    }
}
