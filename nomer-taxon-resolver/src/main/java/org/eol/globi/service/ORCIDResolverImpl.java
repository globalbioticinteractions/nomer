package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class ORCIDResolverImpl implements AuthorIdResolver {
    private static final Logger LOG = LoggerFactory.getLogger(ORCIDResolverImpl.class);

    private String baseUrl = "https://pub.orcid.org/v2.0/";

    @Override
    public Map<String, String> findAuthor(final String authorURI) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String orcId = authorURI.replaceAll("http[s]*://orcid.org/", "");
        HttpGet get = new HttpGet(baseUrl + orcId);
        get.setHeader("Accept", "application/orcid+json");

        Map<String, String> treeMap = new TreeMap<>();

        BasicResponseHandler handler = new BasicResponseHandler();
        String response = HttpUtil.getHttpClient().execute(get, handler);
        JsonNode jsonNode = mapper.readTree(response);
        JsonNode person = jsonNode.get("person");
        if (person != null) {
            JsonNode name = person.get("name");
            if (name != null) {
                String givenNames = getValue(name, "given-names");
                String familyName = getValue(name, "family-name");
                String fullName = givenNames + " " + familyName;
                String externalId = "http://orcid.org/" + orcId;
                treeMap.put(PropertyAndValueDictionary.EXTERNAL_ID, externalId);
                treeMap.put(PropertyAndValueDictionary.NAME, fullName);
                treeMap.put(PropertyAndValueDictionary.PATH, StringUtils.join(Arrays.asList(givenNames, familyName, fullName), CharsetConstant.SEPARATOR));
                treeMap.put(PropertyAndValueDictionary.PATH_NAMES, StringUtils.join(Arrays.asList("given-names", "family-name", "name"), CharsetConstant.SEPARATOR));
                treeMap.put(PropertyAndValueDictionary.PATH_IDS, StringUtils.join(Arrays.asList("", "", externalId), CharsetConstant.SEPARATOR));
            }
        }
        return treeMap;
    }

    protected String getValue(JsonNode details, String fieldName) {
        JsonNode givenNames = details.get(fieldName);
        String givenNamesValue = "";
        if (givenNames != null) {
            givenNamesValue = givenNames.get("value").asText();
        }
        return givenNamesValue;
    }


    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

}
