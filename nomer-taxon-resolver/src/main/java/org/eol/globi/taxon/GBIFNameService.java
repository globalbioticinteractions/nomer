package org.eol.globi.taxon;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.LanguageCodeLookup;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.HttpUtil;
import org.globalbioticinteractions.nomer.util.PropertyEnricherInfo;

@PropertyEnricherInfo(name = "gbif-taxon-name", description = "Lookup taxon in GBIF by name")
public class GBIFNameService extends PropertyEnricherSimple {

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        // see http://www.gbif.org/developer/species
        Map<String, String> enrichedProperties = new HashMap<String, String>(properties);
        
        final String taxonName = properties.get(PropertyAndValueDictionary.NAME);               
        enrichWithTaxonName(enrichedProperties, taxonName);
                        
        return enrichedProperties;
    }      
	
    private void enrichWithTaxonName(Map<String, String> enriched, String taxonName) throws PropertyEnricherException {
        try {
            JsonNode jsonNode = getSpeciesInfoByName(taxonName);
            if (jsonNode == null) {
            	return;
            }
            
            if (jsonNode.has("acceptedKey")) {            	
                jsonNode = getSpeciesInfo(jsonNode.get("acceptedKey").asText());
            }
            addTaxonNode(enriched, jsonNode);

            LanguageCodeLookup languageCodeLookup = new LanguageCodeLookup();
            List<String> commonNames = new ArrayList<String>();
            final String gbifSpeciesId = jsonNode.get("key").asText();
            
            int limit = 20;
            boolean askForMore = true;
            for (int offset = 0; askForMore && offset < 10*limit; offset += limit) {
                String vernaculars = getVernaculars(gbifSpeciesId, offset, limit);
                jsonNode = new ObjectMapper().readTree(vernaculars);
                commonNames.addAll(parseVernaculars(new ObjectMapper().readTree(vernaculars), languageCodeLookup));
                askForMore = jsonNode.has("endOfRecords") && !jsonNode.get("endOfRecords").asBoolean();
            }
            enriched.put(PropertyAndValueDictionary.COMMON_NAMES, StringUtils.join(commonNames, CharsetConstant.SEPARATOR));
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to lookup [" + taxonName + "]", e);
        }

    }
    
    private JsonNode getSpeciesInfoByName(String taxonName) throws IOException {
    	String response = HttpUtil.getContent("http://api.gbif.org/v1/species?name=" + URLEncoder.encode(taxonName, "UTF-8"));
        JsonNode jsonNode = new ObjectMapper().readTree(response); 
        if (jsonNode.has("results")) {
        	JsonNode results = jsonNode.get("results");
        	if (results.isArray() && results.size() > 0) {
        		return results.get(0);
        	}
        }
        
        return null;
    }
    
    private JsonNode getSpeciesInfo(String gbifSpeciesId) throws IOException {
        String response = HttpUtil.getContent("http://api.gbif.org/v1/species/" + gbifSpeciesId);
        return new ObjectMapper().readTree(response);
    }   

    private String getVernaculars(String gbifSpeciesId, int offset, int limit) throws IOException {
        return HttpUtil.getContent("http://api.gbif.org/v1/species/" + gbifSpeciesId + "/vernacularNames?limit=" + limit + "&offset=" + offset);
    }

    private List<String> parseVernaculars(JsonNode jsonNode, LanguageCodeLookup languageCodeLookup) {
        JsonNode results = jsonNode.get("results");
        List<String> commonNames = new ArrayList<String>();
        if (results != null && results.isArray()) {
            for (JsonNode result : results) {
                if (result.has("vernacularName") && result.has("language")) {
                    JsonNode preferred = result.get("preferred");
                    if (preferred == null || (preferred.isBoolean() && preferred.asBoolean())) {
                        String commonName = result.get("vernacularName").asText();
                        String language = result.get("language").asText();
                        String shortCode = languageCodeLookup.lookupLanguageCodeFor(language);
                        if (StringUtils.isNotBlank(commonName) && StringUtils.isNotBlank(shortCode)) {
                            commonNames.add(commonName + " @" + shortCode);
                        }

                    }
                }
            }
        }
        return commonNames;
    }

    private void addTaxonNode(Map<String, String> enriched, JsonNode jsonNode) {
        String externalId;
        externalId = jsonNode.has("key") ? (TaxonomyProvider.GBIF.getIdPrefix() + jsonNode.get("key").asText()) : "";
        String name = jsonNode.has("canonicalName") ? jsonNode.get("canonicalName").asText() : "";
        String rank = jsonNode.has("rank") ? jsonNode.get("rank").asText().toLowerCase() : "";

        enriched.put(PropertyAndValueDictionary.NAME, name);
        enriched.put(PropertyAndValueDictionary.RANK, rank);
        enriched.put(PropertyAndValueDictionary.EXTERNAL_ID, externalId);

        String[] pathLabels = new String[]{
                "kingdom",
                "phylum",
                "class",
                "order",
                "family",
                "genus",
                "species"};

        String[] pathIdLabels = new String[]{
                "kingdomKey",
                "phylumKey",
                "classKey",
                "orderKey",
                "familyKey",
                "genusKey",
                "speciesKey"};
        List<String> ids = collect(jsonNode, pathIdLabels, TaxonomyProvider.GBIF.getIdPrefix());
        enriched.put(PropertyAndValueDictionary.PATH_IDS, toString(ids));

        List<String> path = collect(jsonNode, pathLabels);
        enriched.put(PropertyAndValueDictionary.PATH, toString(path));

        List<String> pathNames = Arrays.asList(pathLabels);
        enriched.put(PropertyAndValueDictionary.PATH_NAMES, toString(pathNames));
    }

    private List<String> collect(JsonNode jsonNode, String[] pathIdLabels) {
        return collect(jsonNode, pathIdLabels, "");
    }

    private List<String> collect(JsonNode jsonNode, String[] pathIdLabels, String prefix) {
        List<String> ids = new ArrayList<String>();
        for (String pathIdLabel : pathIdLabels) {
            ids.add(jsonNode.has(pathIdLabel) ? (prefix + jsonNode.get(pathIdLabel).asText()) : "");
        }
        return ids;
    }

    protected String toString(List<String> ids) {
        return StringUtils.join(ids, CharsetConstant.SEPARATOR);
    }

    public void shutdown() {

    }
}
