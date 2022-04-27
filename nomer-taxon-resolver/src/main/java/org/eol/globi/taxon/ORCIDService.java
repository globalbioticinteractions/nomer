package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.LanguageCodeLookup;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.HttpUtil;
import org.globalbioticinteractions.nomer.util.PropertyEnricherInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@PropertyEnricherInfo(name = "orcid-web", description = "Lookup ORCID by id with ORCID:* prefix.")
public class ORCIDService extends PropertyEnricherSimple {
    public static final String RESPONSE_PREFIX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAP-ENV:Envelope SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\"><SOAP-ENV:Body><ns1:getAphiaIDResponse xmlns:ns1=\"http://tempuri.org/\"><return xsi:type=\"xsd:int\">";
    public static final String RESPONSE_SUFFIX = "</return></ns1:getAphiaIDResponse></SOAP-ENV:Body></SOAP-ENV:Envelope>";

    private final LanguageCodeLookup languageLookup;

    public ORCIDService() {
        languageLookup = new LanguageCodeLookup();
    }

    String lookupIdByName(String taxonName) throws PropertyEnricherException {
        String response = getResponse("getAphiaID", "scientificname", taxonName);
        String id = null;
        if (response.startsWith(RESPONSE_PREFIX) && response.endsWith(RESPONSE_SUFFIX)) {
            String trimmed = response.replace(RESPONSE_PREFIX, "");
            trimmed = trimmed.replace(RESPONSE_SUFFIX, "");
            try {
                Long aphiaId = Long.parseLong(trimmed);
                id = TaxonomyProvider.ID_PREFIX_WORMS + aphiaId;
            } catch (NumberFormatException ex) {
                //ignore
            }
        }
        return id;
    }

    private String getResponse(String methodName, String paramName, String paramValue) throws PropertyEnricherException {
        HttpPost post = new HttpPost("https://www.marinespecies.org/aphia.php?p=soap");
        post.setHeader("SOAPAction", "http://tempuri.org/getAphiaID");
        post.setHeader("Content-Type", "text/xml;charset=utf-8");
        String requestBody = "<?xml version=\"1.0\" ?>";
        requestBody += "<soap:Envelope ";
        requestBody += "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ";
        requestBody += "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" ";
        requestBody += "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">";
        requestBody += "<soap:Body>";
        requestBody += "<" + methodName + " xmlns=\"http://tempuri.org/\">";
        requestBody = requestBody + "<" + paramName + ">" + paramValue + "</" + paramName + ">";
        requestBody = requestBody + "<marine_only>false</marine_only>";
        requestBody += "</" + methodName + "></soap:Body></soap:Envelope>";

        InputStreamEntity catchEntity;
        try {
            catchEntity = new InputStreamEntity(new ByteArrayInputStream(requestBody.getBytes("UTF-8")), requestBody.getBytes().length);
        } catch (UnsupportedEncodingException e) {
            throw new PropertyEnricherException("problem creating request body for [" + post.getURI().toString() + "]", e);
        }
        post.setEntity(catchEntity);

        BasicResponseHandler responseHandler = new BasicResponseHandler();
        String response;
        try {
            response = HttpUtil.executeWithTimer(post, responseHandler);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to connect to [" + post.getURI().toString() + "]", e);
        }
        return response;
    }

    Map<String, String> enrichById(final String id, final Map<String, String> properties) throws PropertyEnricherException {
        if (isAlphiaID(id)) {
            String response = getResponse("getAphiaRecordByID", "AphiaID", parseAphiaId(id));
            String aphiaId = id;
            String validAphiaId = XmlUtil.extractName(response, "valid_AphiaID");
            if (StringUtils.isNotBlank(validAphiaId)) {
                aphiaId = TaxonomyProvider.ID_PREFIX_WORMS + validAphiaId;
                properties.put(PropertyAndValueDictionary.EXTERNAL_ID, aphiaId);
                properties.put(PropertyAndValueDictionary.NAME, XmlUtil.extractName(response, "valid_name"));
            }
            if (isAlphiaID(aphiaId)) {
                String response1 = getResponse("getAphiaClassificationByID", "AphiaID", parseAphiaId(aphiaId));
                String value = XmlUtil.extractPath(response1, "scientificname", "");
                properties.put(PropertyAndValueDictionary.PATH, StringUtils.isBlank(value) ? null : value);
                value = XmlUtil.extractPath(response1, "AphiaID", TaxonomyProvider.ID_PREFIX_WORMS);
                properties.put(PropertyAndValueDictionary.PATH_IDS, StringUtils.isBlank(value) ? null : value);
                value = XmlUtil.extractPath(response1, "rank", "");
                String[] ranks = CSVTSVUtil.splitPipes(value);
                if (ranks != null && ranks.length > 0) {
                    properties.put(PropertyAndValueDictionary.RANK, StringUtils.trim(StringUtils.lowerCase(ranks[ranks.length - 1])));
                }

                properties.put(PropertyAndValueDictionary.PATH_NAMES, StringUtils.isBlank(value) ? null : StringUtils.lowerCase(value));

                response1 = getResponse("getAphiaVernacularsByID", "AphiaID", parseAphiaId(aphiaId));
                String vernaculars = XmlUtil.extractPath(response1, "vernacular", "");
                String languageCodes = XmlUtil.extractPath(response1, "language_code", "");
                String[] commonNames = CSVTSVUtil.splitPipes(vernaculars);
                String[] langCodes = CSVTSVUtil.splitPipes(languageCodes);
                List<String> names = new ArrayList<String>();
                for (int i = 0; i < commonNames.length && (langCodes.length == commonNames.length); i++) {
                    String code = languageLookup.lookupLanguageCodeFor(StringUtils.trim(langCodes[i]));
                    names.add(StringUtils.trim(commonNames[i]) + " @" + (code == null ? langCodes[i] : code));
                }
                properties.put(PropertyAndValueDictionary.COMMON_NAMES, StringUtils.join(names, CharsetConstant.SEPARATOR));
            }
        }

        return properties;
    }

    String lookupTaxonPathById(String id) throws PropertyEnricherException {
        String path = null;
        if (isAlphiaID(id)) {
            final String alphiaId = parseAphiaId(id);
            String response = getResponse("getAphiaClassificationByID", "AphiaID", alphiaId);
            path = XmlUtil.extractPath(response, "scientificname", "");
        }
        return StringUtils.isBlank(path) ? null : path;
    }

    private String parseAphiaId(String id) {
        return ExternalIdUtil.stripPrefix(TaxonomyProvider.WORMS, id);
    }

    private boolean isAlphiaID(String id) {
        return TaxonomyProvider.WORMS.equals(ExternalIdUtil.taxonomyProviderFor(id));
    }

    @Override
    public Map<String, String> enrich(final Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enrichedProperties = new HashMap<String, String>(properties);
        if (!isAlphiaID(properties.get(PropertyAndValueDictionary.EXTERNAL_ID))) {
            enrichedProperties.put(PropertyAndValueDictionary.EXTERNAL_ID, lookupIdByName(properties.get(PropertyAndValueDictionary.NAME)));
        }

        enrichedProperties = enrichById(enrichedProperties.get(PropertyAndValueDictionary.EXTERNAL_ID), enrichedProperties);

        return Collections.unmodifiableMap(enrichedProperties);
    }

    @Override
    public void shutdown() {

    }
}
