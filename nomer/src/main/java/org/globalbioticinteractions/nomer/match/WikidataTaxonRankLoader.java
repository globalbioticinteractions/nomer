package org.globalbioticinteractions.nomer.match;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.ExternalIdUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WikidataTaxonRankLoader {
    private static final String WIKIDATA_ENTITY_PREFIX = "http://www.wikidata.org/entity/";

    public static URI createWikidataTaxonRankQuery() throws URISyntaxException {
        String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX bd: <http://www.bigdata.com/rdf#>\n" +
                "PREFIX wd: <" + WIKIDATA_ENTITY_PREFIX + ">\n" +
                "PREFIX wikibase: <http://wikiba.se/ontology#>\n" +
                "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n" +
                "SELECT ?i ?l WHERE {\n" +
                "  ?i wdt:P31 wd:Q427626.\n" +
                "  ?i rdfs:label ?l\n" +
                "}";

        return new URI("https", "query.wikidata.org", "/sparql", "format=json&query=" + queryString, null);
    }

    public static void importTaxonRanks(TermListener termListener, ResourceService resourceService, URI req) throws IOException {
        InputStream is = resourceService.retrieve(req);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IOUtils.copy(is, outputStream);
        String json = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        handleWikidataTaxonRanks(termListener, json);
    }

    static void handleWikidataTaxonRanks(TermListener termListener, String json) throws IOException {
        JsonNode jsonNode = new ObjectMapper().readTree(json);
        JsonNode results = jsonNode.get("results");
        JsonNode bindings = results.get("bindings");
        Map<String, String> ranks = new HashMap<>();
        for (JsonNode binding : bindings) {
            JsonNode item = binding.get("i");
            String value = item.get("value").asText();

            JsonNode label = binding.get("l");
            String language = label.get("xml:lang").asText();
            String labelString = label.get("value").asText();
            String s = ranks.containsKey(value) ? ranks.get(value) + CharsetConstant.SEPARATOR : "";
            ranks.put(value, s + labelString + " " + CharsetConstant.LANG_SEPARATOR_CHAR + language);
        }

        for (Map.Entry<String, String> rankEntries : ranks.entrySet()) {
            String commonNames = rankEntries.getValue();
            String[] names = CSVTSVUtil.splitPipes(commonNames);
            String primary = Arrays.stream(names)
                    .filter(str -> StringUtils.endsWith(StringUtils.trim(str), "@en"))
                    .findFirst().orElse(names.length == 0 ? "" : names[0]);
            if (StringUtils.isNotBlank(primary)) {
                String id = StringUtils.replace(rankEntries.getKey(), WIKIDATA_ENTITY_PREFIX, TaxonomyProvider.WIKIDATA.getIdPrefix());
                String primaryLabel = StringUtils.trim(primary.split(CharsetConstant.LANG_SEPARATOR_CHAR)[0]);
                TaxonImpl taxon = new TaxonImpl(primaryLabel, id);
                taxon.setCommonNames(commonNames);
                taxon.setPath(primaryLabel);
                taxon.setPathIds(id);
                taxon.setExternalUrl(ExternalIdUtil.urlForExternalId(id));
                termListener.onTerm(taxon);
            }
        }
    }

    static TermListener createCacheWriter(PrintStream out) {
        return taxon -> out.println(taxon.getExternalId()
                + "\t"
                + taxon.getName()
                + "\t\t"
                + taxon.getCommonNames()
                + "\t"
                + taxon.getPath()
                + "\t"
                + taxon.getPathIds()
                + "\t\t"
                + taxon.getExternalUrl()
                + "\t");
    }

    public static TermListener createMapWriter(PrintStream out) {
        return taxon -> {
                List<String> names = Arrays.asList(CSVTSVUtil.splitPipes(taxon.getCommonNames()));
                names.stream().map(str -> str.split(CharsetConstant.LANG_SEPARATOR_CHAR)[0])
                        .map(StringUtils::trim)
                        .map(str -> Stream.of("", str, taxon.getExternalId(), taxon.getName()))
                        .map(stream -> CSVTSVUtil.mapEscapedValues(stream).collect(Collectors.joining("\t")))
                        .forEach(out::println);

            };
    }

    interface TermListener {
        void onTerm(Taxon taxon);
    }
}
