package org.eol.globi.taxon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.LanguageCodeLookup;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class GBIFUtil {

    static void appendCommonNames(Map<String, String> enriched, JsonNode jsonNode) throws IOException {
        LanguageCodeLookup languageCodeLookup = new LanguageCodeLookup();
        Collection<String> commonNamesSet = new TreeSet<>();
        final String gbifSpeciesId = jsonNode.get("key").asText();

        int limit = 20;
        boolean askForMore = true;
        for (int offset = 0; askForMore && offset < 10 * limit; offset += limit) {
            String vernaculars = getVernaculars(gbifSpeciesId, offset, limit);
            jsonNode = new ObjectMapper().readTree(vernaculars);
            commonNamesSet.addAll(parseVernaculars(new ObjectMapper().readTree(vernaculars), languageCodeLookup));
            askForMore = jsonNode.has("endOfRecords") && !jsonNode.get("endOfRecords").asBoolean();
        }
        enriched.put(PropertyAndValueDictionary.COMMON_NAMES, StringUtils.join(commonNamesSet, CharsetConstant.SEPARATOR));
    }

    private static String getVernaculars(String gbifSpeciesId, int offset, int limit) throws IOException {
        return HttpUtil.getContent("http://api.gbif.org/v1/species/" + gbifSpeciesId + "/vernacularNames?limit=" + limit + "&offset=" + offset);
    }

    private static List<String> parseVernaculars(JsonNode jsonNode, LanguageCodeLookup languageCodeLookup) {
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

    static JsonNode getFirstResult(String response) throws IOException {
        JsonNode firstResult = null;
        JsonNode jsonNode = new ObjectMapper().readTree(response);
        if (jsonNode.has("results")) {
            JsonNode results = jsonNode.get("results");
            if (results.isArray() && results.size() > 0) {
                firstResult = results.get(0);
            }
        }
        return firstResult;
    }

    static void addTaxonNode(Map<String, String> enriched, JsonNode jsonNode) {
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
        enriched.put(PropertyAndValueDictionary.PATH_IDS, join(ids));

        List<String> path = collect(jsonNode, pathLabels);
        enriched.put(PropertyAndValueDictionary.PATH, join(path));

        List<String> pathNames = Arrays.asList(pathLabels);
        enriched.put(PropertyAndValueDictionary.PATH_NAMES, join(pathNames));
    }

    private static List<String> collect(JsonNode jsonNode, String[] pathIdLabels) {
        return collect(jsonNode, pathIdLabels, "");
    }

    private static List<String> collect(JsonNode jsonNode, String[] pathIdLabels, String prefix) {
        List<String> ids = new ArrayList<>();
        for (String pathIdLabel : pathIdLabels) {
            ids.add(jsonNode.has(pathIdLabel) ? (prefix + jsonNode.get(pathIdLabel).asText()) : "");
        }
        return ids;
    }

    static String join(List<String> ids) {
        return StringUtils.join(ids, CharsetConstant.SEPARATOR);
    }



    public static String compileAuthorshipString(String author, String year) {
        String author1 = removeNull(author);
        String year1 = removeNull(year);
        String authorship = null;
        if (StringUtils.isNotBlank(author1) && StringUtils.isNotBlank(year1)) {
            authorship = author1 + ", " + year1;
        }
        return authorship;
    }

    private static String removeNull(String rowValue) {
        return StringUtils.remove(rowValue, "\\N");
    }

    public static boolean isSynonym(String[] rowValues) {
        return rowValues != null
                && StringUtils.equals("t", rowValues[3]);
    }

    static JsonNode getSpeciesInfo(String gbifSpeciesId) throws IOException {
        String response = HttpUtil.getContent("http://api.gbif.org/v1/species/" + gbifSpeciesId);
        return new ObjectMapper().readTree(response);
    }

    public static void enrichWithTaxonName(Map<String, String> enriched, String taxonName) throws PropertyEnricherException {
        try {
            JsonNode jsonNode = getSpeciesInfoByName(taxonName);
            if (jsonNode != null) {
                if (jsonNode.has("acceptedKey")) {
                    jsonNode = getSpeciesInfo(jsonNode.get("acceptedKey").asText());
                }
                addTaxonNode(enriched, jsonNode);

                appendCommonNames(enriched, jsonNode);
            }

        } catch (IOException e) {
            throw new PropertyEnricherException("failed to lookup [" + taxonName + "]", e);
        }

    }

    public static JsonNode getSpeciesInfoByName(String taxonName) throws IOException {
        String response = HttpUtil.getContent("http://api.gbif.org/v1/species?name=" + URLEncoder.encode(taxonName, "UTF-8"));
        return getFirstResult(response);
    }
}
