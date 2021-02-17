package org.eol.globi.taxon;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.HttpUtil;
import org.globalbioticinteractions.nomer.util.PropertyEnricherInfo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@PropertyEnricherInfo(name = "bold-web", description = "Use BOLD webservice to lookup taxa by bin/taxon id using BOLD:* and BOLDTaxon:* prefixes.")
public class BOLDService extends PropertyEnricherSimple {
    private static final List<String> BOLD_RANKS = Arrays.asList("phylum", "class", "order", "family", "subfamily", "genus", "species", "subspecies");

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enriched = new HashMap<String, String>(properties);
        String externalId = properties.get(PropertyAndValueDictionary.EXTERNAL_ID);
        String name = properties.get(PropertyAndValueDictionary.NAME);

        if (StringUtils.startsWith(externalId, "BOLD:")) {
            enrichWithBINTaxon(enriched, externalId);
        } else if (StringUtils.startsWith(name, "BOLD:")) {
            enrichWithBINTaxon(enriched, name);
        } else if (StringUtils.startsWith(externalId, "BOLDTaxon:")) {
            Taxon taxon = getTaxonAPIResponse("taxId=" + StringUtils.removeStart(externalId, "BOLDTaxon:") + "&dataTypes=basic&includeTree=true");
            if (taxon != null) {
                enriched.putAll(TaxonUtil.taxonToMap(taxon));
            }
        }
        return enriched;
    }

    private void enrichWithBINTaxon(Map<String, String> enriched, String bin) throws PropertyEnricherException {
        Taxon taxon = getBINResponse("bin=" + bin + "&format=tsv");
        if (taxon != null) {
            enriched.putAll(TaxonUtil.taxonToMap(taxon));
        }
    }


    private Taxon getBINResponse(String queryString) throws PropertyEnricherException {
        URI uri;
        try {
            uri = new URI("http",
                    null, "www.boldsystems.org",
                    80,
                    "/index.php/API_Public/specimen",
                    queryString,
                    null);
        } catch (URISyntaxException e) {
            throw new PropertyEnricherException("failed to create uri", e);
        }
        HttpGet get = new HttpGet(uri);

        HttpClient httpClient = HttpUtil.getHttpClient();
        try {
            HttpResponse execute = httpClient.execute(get);
            try (InputStream is = execute.getEntity().getContent()) {
                return parseTaxon(is);
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to query [" + uri.toASCIIString() + "]", e);
        }
    }

    private Taxon getTaxonAPIResponse(String queryString) throws PropertyEnricherException {
        URI uri;
        try {
            uri = new URI("http",
                    null, "www.boldsystems.org",
                    80,
                    "/index.php/API_Tax/TaxonData",
                    queryString,
                    null);
        } catch (URISyntaxException e) {
            throw new PropertyEnricherException("failed to create uri", e);
        }
        try {
            return parseTaxonIdMatch(HttpUtil.getContent(uri));
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to match [" + queryString + "]", e);
        }
    }

    public void shutdown() {

    }

    static Taxon parseTaxon(InputStream is) throws IOException {
        LabeledCSVParser labeledTSVParser = CSVTSVUtil.createLabeledTSVParser(is);

        labeledTSVParser.getLine();

        TaxonImpl taxon = new TaxonImpl();

        List<String> names = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        List<String> ranks = new ArrayList<>();

        for (String rank : BOLD_RANKS) {
            String name = StringUtils.trim(labeledTSVParser.getValueByLabel(rank + "_name"));
            String id = StringUtils.trim(labeledTSVParser.getValueByLabel(rank + "_taxID"));
            String externalId = "BOLDTaxon:" + StringUtils.trim(id);
            if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(id)) {
                names.add(name);
                ranks.add(rank);
                ids.add(externalId);

                taxon.setName(name);
                taxon.setRank(rank);
                taxon.setExternalId(externalId);
            }
        }


        taxon.setPath(String.join(CharsetConstant.SEPARATOR, names));
        taxon.setPathNames(String.join(CharsetConstant.SEPARATOR, ranks));
        taxon.setPathIds(String.join(CharsetConstant.SEPARATOR, ids));
        return names.isEmpty() ? null : taxon;
    }

    static Taxon parseTaxonIdMatch(String result) throws IOException {
        JsonNode jsonNode = new ObjectMapper().readTree(result);

        Taxon taxon = null;

        List<String> names = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        List<String> ranks = new ArrayList<>();

        for (Iterator<Map.Entry<String, JsonNode>> it = jsonNode.getFields(); it.hasNext(); ) {
            JsonNode node = it.next().getValue();
            if (node.has("taxon")
                    && node.has("taxid")
                    && node.has("tax_rank")) {

                String name = node.get("taxon").asText();
                String rank = node.get("tax_rank").asText();
                String externalId = "BOLDTaxon:" + node.get("taxid").asText();
                if (taxon == null) {
                    taxon = new TaxonImpl(name, externalId);
                    taxon.setRank(rank);
                }

                names.add(name);
                ranks.add(rank);
                ids.add(externalId);
            }
        }

        Collections.reverse(names);
        Collections.reverse(ranks);
        Collections.reverse(ids);

        if (taxon != null) {
            taxon.setPathIds(String.join(CharsetConstant.SEPARATOR, ids));
            taxon.setPath(String.join(CharsetConstant.SEPARATOR, names));
            taxon.setPathNames(String.join(CharsetConstant.SEPARATOR, ranks));
        }
        return taxon;
    }

}
