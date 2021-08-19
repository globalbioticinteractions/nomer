package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.LanguageCodeLookup;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.HttpUtil;
import org.globalbioticinteractions.nomer.match.GBIFNameRelationType;
import org.globalbioticinteractions.nomer.match.GBIFRank;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

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

    public static Taxon resolveTaxonId1(Map<Long, Pair<String, GBIFRank>> idToNameAndRank,
                                        Map<Long, Pair<GBIFNameRelationType, Long>> idRelation,
                                        Long requestedTaxonId) {
        return requestedTaxonId == null
                ? null
                : resolveTaxonId(
                idToNameAndRank,
                idRelation,
                requestedTaxonId);
    }

    public static Taxon resolveTaxonId(Map<Long, Pair<String, GBIFRank>> idToNameAndRank,
                                       Map<Long, Pair<GBIFNameRelationType, Long>> idRelation,
                                       Long taxonId) {
        Long childId = taxonId;

        LinkedList<Long> ancestorIds = new LinkedList<>();

        LinkedList<GBIFRank> ancestorRanks = new LinkedList<>();
        LinkedList<String> ancestorNames = new LinkedList<>();
        appendById(idToNameAndRank, ancestorIds, ancestorRanks, ancestorNames, childId);

        Pair<GBIFNameRelationType, Long> relation;
        while ((relation = idRelation.get(childId)) != null) {

            // add only accepted names
            if (GBIFNameRelationType.SYNONYM.equals(relation.getLeft())) {
                ancestorIds.clear();
                ancestorNames.clear();
                ancestorRanks.clear();
            }

            if (ancestorIds.contains(relation.getRight())) {
                throw new IllegalStateException("found circular reference: parentId [" + relation.getRight() + "] already present in taxon path");
            }
            appendById(idToNameAndRank, ancestorIds, ancestorRanks, ancestorNames, relation.getRight());
            childId = relation.getRight();
        }


        String path = String.join(CharsetConstant.SEPARATOR, ancestorNames);
        String pathNames = ancestorRanks
                .stream()
                .map(Object::toString)
                .map(StringUtils::lowerCase)
                .map(x -> StringUtils.replace(x, "unranked", ""))
                .collect(Collectors.joining(CharsetConstant.SEPARATOR));
        String pathIds = ancestorIds
                .stream()
                .map(Object::toString)
                .map(x -> TaxonomyProvider.GBIF.getIdPrefix() + x)
                .collect(Collectors.joining(CharsetConstant.SEPARATOR));

        return ancestorNames.isEmpty()
                ? null
                : populateTaxon(ancestorIds, ancestorRanks, ancestorNames, path, pathNames, pathIds);
    }

    public static Taxon populateTaxon(LinkedList<Long> ancestorIds, LinkedList<GBIFRank> ancestorRanks, LinkedList<String> ancestorNames, String path, String pathNames, String pathIds) {
        Taxon taxon;
        taxon = new TaxonImpl(ancestorNames.getLast(), TaxonomyProvider.GBIF.getIdPrefix() + ancestorIds.getLast());
        if (ancestorRanks.size() > 0) {
            GBIFRank lastRank = ancestorRanks.getLast();
            if (!GBIFRank.UNRANKED.equals(lastRank)) {
                taxon.setRank(lastRank.name().toLowerCase());
            }
        }
        taxon.setPathIds(pathIds);
        taxon.setPathNames(pathNames);
        taxon.setPath(path);
        return taxon;
    }

    public static Pair<String, GBIFRank> appendById(Map<Long, Pair<String, GBIFRank>> idToNameAndRank,
                                                    LinkedList<Long> ancestorIds,
                                                    LinkedList<GBIFRank> ancestorRanks,
                                                    LinkedList<String> ancestorNames,
                                                    Long taxonId) {
        Pair<String, GBIFRank> nameAndRank = idToNameAndRank.get(taxonId);
        if (nameAndRank != null) {
            ancestorIds.push(taxonId);
            ancestorNames.push(nameAndRank.getLeft());
            ancestorRanks.push(nameAndRank.getRight());
        }
        return nameAndRank;
    }

    public static Pair<Long, Pair<GBIFNameRelationType, Long>> parseIdRelation(String line) {
        String[] rowValues = CSVTSVUtil.splitTSV(line);
        return parsePair(rowValues, isSynonym(rowValues) ? GBIFNameRelationType.SYNONYM : GBIFNameRelationType.PARENT);
    }

    public static Pair<Long, Pair<GBIFNameRelationType, Long>> parsePair(String[] rowValues, GBIFNameRelationType nameStatus) {
        Pair<Long, Pair<GBIFNameRelationType, Long>> idRelation = null;
        if (rowValues != null) {
            String nameId = rowValues[0];
            String relatedNameId = rowValues[1];
            if (StringUtils.isNotBlank(nameId)
                    && StringUtils.isNotBlank(relatedNameId)
                    && !StringUtils.equals(relatedNameId, "\\N")) {
                idRelation = Pair.of(Long.parseLong(rowValues[0]),
                        Pair.of(nameStatus, Long.parseLong(rowValues[1])));
            }
        }
        return idRelation;
    }

    public static Pair<String, Long> parseNameId(String line) {
        Pair<String, Long> nameId = null;
        String[] rowValues = CSVTSVUtil.splitTSV(line);
        if (rowValues.length > 1) {
            String canonicalName = rowValues[0];
            String taxId = rowValues[1];
            nameId = Pair.of(canonicalName, Long.parseLong(taxId));
        }
        return nameId;
    }

    public static Triple<Long, String, GBIFRank> parseIdNameRank(String line) {
        Triple<Long, String, GBIFRank> taxon = null;
        String[] rowValues = CSVTSVUtil.splitTSV(line);

        if (rowValues.length > 19) {
            String taxId = rowValues[0];
            String rank = StringUtils.trim(rowValues[5]);


            String canonicalName = rowValues[19];
            taxon = Triple.of(Long.parseLong(taxId), canonicalName, GBIFRank.valueOf(rank));
        }
        return taxon;
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
