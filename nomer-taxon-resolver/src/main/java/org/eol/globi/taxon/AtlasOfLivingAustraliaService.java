package org.eol.globi.taxon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.HttpTimedUtil;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.HttpUtil;
import org.globalbioticinteractions.nomer.util.PropertyEnricherInfo;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@PropertyEnricherInfo(name = "ala-taxon", description = "Lookup taxon in Atlas of Living Australia by name or by id using ALATaxon:* prefix.")
public class AtlasOfLivingAustraliaService extends PropertyEnricherSimple {

    private static final String AFD_TSN_PREFIX = "urn:lsid:biodiversity.org.au:afd.taxon:";
    private static final String ATLAS_OF_LIVING_AUSTRALIA_TAXON = TaxonomyProvider.ATLAS_OF_LIVING_AUSTRALIA.getIdPrefix();

    @Override
    public Map<String, String> enrich(final Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enrichedProperties = new HashMap<>(properties);
        String externalId = properties.get(PropertyAndValueDictionary.EXTERNAL_ID);
        if (StringUtils.isBlank(externalId) || hasSupportedExternalId(externalId)) {
            if (needsEnrichment(properties)) {
                String guid = StringUtils.replace(externalId, TaxonomyProvider.ID_PREFIX_AUSTRALIAN_FAUNAL_DIRECTORY, AFD_TSN_PREFIX);
                guid = StringUtils.replace(guid, ATLAS_OF_LIVING_AUSTRALIA_TAXON, "");
                String taxonName = properties.get(PropertyAndValueDictionary.NAME);
                if (StringUtils.isBlank(guid) && StringUtils.length(taxonName) > 2) {
                    guid = findTaxonGUIDByName(taxonName);
                }
                if (StringUtils.isNotBlank(guid)) {
                    Map<String, String> taxonInfo = findTaxonInfoByGUID(guid);
                    enrichedProperties.putAll(taxonInfo);
                }
            }
        }
        return enrichedProperties;
    }

    private boolean hasSupportedExternalId(String externalId) throws PropertyEnricherException {
        return StringUtils.startsWith(externalId, TaxonomyProvider.ID_PREFIX_AUSTRALIAN_FAUNAL_DIRECTORY)
                || StringUtils.startsWith(externalId, AFD_TSN_PREFIX)
                || StringUtils.startsWith(externalId, ATLAS_OF_LIVING_AUSTRALIA_TAXON);
    }

    private boolean needsEnrichment(Map<String, String> properties) {
        return StringUtils.isBlank(properties.get(PropertyAndValueDictionary.PATH))
                || StringUtils.isBlank(properties.get(PropertyAndValueDictionary.COMMON_NAMES));
    }

    private URI taxonInfoByGUID(String taxonGUID) throws URISyntaxException {
        return new URI("http", null, "bie.ala.org.au", 80, "/ws/species/" + taxonGUID + ".json", null, null);
    }

    private URI taxonInfoByName(String taxonName) throws URISyntaxException {
        return new URI("http", null, "bie.ala.org.au", 80, "/ws/search.json", "q=" + taxonName, null);
    }

    private String findTaxonGUIDByName(String taxonName) throws PropertyEnricherException {
        String guid = null;
        try {
            URI uri = taxonInfoByName(taxonName);
            String response = getResponse(uri);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            if (node.has("searchResults")) {
                JsonNode searchResults = node.get("searchResults");
                if (searchResults.has("results")) {
                    JsonNode results = searchResults.get("results");
                    for (JsonNode result : results) {
                        if (result.has("name")
                                && result.has("idxtype")
                                && result.has("guid")) {
                            if (StringUtils.equals(taxonName, result.get("name").asText())
                                    && StringUtils.equals("TAXON", result.get("idxtype").asText())) {
                                if (result.has("acceptedConceptID")) {
                                    guid = result.get("acceptedConceptID").asText();
                                } else {
                                    guid = result.get("guid").asText();
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } catch (URISyntaxException e) {
            throw new PropertyEnricherException("failed to create uri", e);
        } catch (JsonProcessingException e) {
            throw new PropertyEnricherException("failed to parse response", e);
        }
        return guid;
    }

    private Map<String, String> findTaxonInfoByGUID(String taxonGUID) throws PropertyEnricherException {
        Map<String, String> info = Collections.emptyMap();
        try {
            URI uri = taxonInfoByGUID(taxonGUID);
            String response = getResponse(uri);
            if (StringUtils.isNotBlank(response)) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(response);
                info = new HashMap<>();
                if (node.has("taxonConcept")) {
                    info.putAll(parseTaxonConcept(node.get("taxonConcept")));
                }
                if (node.has("classification")) {
                    info.putAll(parseClassification(node.get("classification")));
                    final Taxon taxon = TaxonUtil.mapToTaxon(info);
                    taxon.setPath(taxon.getPath());
                    taxon.setPathIds(taxon.getPathIds());
                    taxon.setPathNames(taxon.getPathNames());
                    info.putAll(TaxonUtil.taxonToMap(taxon));
                }
                if (node.has("commonNames")) {
                    info.putAll(parseCommonName(node.get("commonNames")));
                }
            }
        } catch (URISyntaxException e) {
            throw new PropertyEnricherException("failed to create uri", e);
        } catch (JsonProcessingException e) {
            throw new PropertyEnricherException("failed to parse response", e);
        }
        return info;
    }

    private Map<String, String> parseCommonName(JsonNode commonNames) {
        final List<String> commonNameList = new ArrayList<String>();
        for (final JsonNode commonName : commonNames) {
            if (commonName.has("nameString") && commonName.has("language")) {
                commonNameList.add(commonName.get("nameString").asText() + " @" + commonName.get("language").asText().split("-")[0]);
            }
        }
        return new HashMap<String, String>() {{
            if (commonNameList.size() > 0) {
                put(PropertyAndValueDictionary.COMMON_NAMES,
                        StringUtils.join(commonNameList, CharsetConstant.SEPARATOR));
            }
        }};
    }

    private Map<String, String> parseClassification(JsonNode classification) {
        Map<String, String> info = new HashMap<String, String>();

        String[] ranks = new String[]{
                "kingdom", "phylum", "subphylum", "class", "subclass", "order", "suborder", "superfamily", "family", "subfamily", "genus", "species"
        };
        List<String> path = new ArrayList<String>();
        List<String> pathIds = new ArrayList<String>();
        List<String> pathNames = new ArrayList<String>();

        for (String rank : ranks) {
            if (classification.has(rank)) {
                String textValue = classification.get(rank).asText();
                path.add(StringUtils.capitalize(StringUtils.lowerCase(textValue)));
                pathNames.add(getRankString(rank));
                String guid = "";
                String guidName = rank + "Guid";
                if (classification.has(guidName)) {
                    guid = ATLAS_OF_LIVING_AUSTRALIA_TAXON + StringUtils.trim(classification.get(guidName).asText());
                }
                pathIds.add(guid);
            }
        }

        info.put(PropertyAndValueDictionary.PATH, StringUtils.join(path, CharsetConstant.SEPARATOR));
        info.put(PropertyAndValueDictionary.PATH_IDS, StringUtils.join(pathIds, CharsetConstant.SEPARATOR));
        info.put(PropertyAndValueDictionary.PATH_NAMES, StringUtils.join(pathNames, CharsetConstant.SEPARATOR));
        return info;
    }

    private Map<String, String> parseTaxonConcept(JsonNode classification) {
        Map<String, String> info = new HashMap<String, String>();
        if (classification.has("nameString")) {
            info.put(PropertyAndValueDictionary.NAME, classification.get("nameString").asText());
        }

        if (classification.has("rankString")) {
            String rank = classification.get("rankString").asText();
            info.put(PropertyAndValueDictionary.RANK, getRankString(rank));
        }

        if (classification.has("guid")) {
            String guid = classification.get("guid").asText();
            String externalId = ATLAS_OF_LIVING_AUSTRALIA_TAXON + guid;
            info.put(PropertyAndValueDictionary.EXTERNAL_ID, externalId);
        }

        return info;
    }

    private String getRankString(String rank) {
        return StringUtils.equals(rank, "clazz") ? "class" : rank;
    }

    private String getResponse(URI uri) throws PropertyEnricherException {
        HttpGet get = new HttpGet(uri);
        BasicResponseHandler responseHandler = new BasicResponseHandler();
        String response;
        try {
            response = HttpTimedUtil.executeWithTimer(get, responseHandler);
        } catch (HttpResponseException e) {
            if (HttpStatus.SC_NOT_FOUND == e.getStatusCode()) {
                response = "{}";
            } else {
                throw new PropertyEnricherException("failed to lookup [" + uri.toString() + "]", e);
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to lookup [" + uri.toString() + "]", e);
        }
        return response;
    }

    public void shutdown() {

    }
}
