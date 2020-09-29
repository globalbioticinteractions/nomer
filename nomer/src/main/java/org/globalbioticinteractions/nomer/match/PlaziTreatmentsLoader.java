package org.globalbioticinteractions.nomer.match;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.TaxonImportListener;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlaziTreatmentsLoader {
    private static final String WIKIDATA_ENTITY_PREFIX = "http://www.wikidata.org/entity/";

    public static void importTaxonRanks(TermListener termListener) throws URISyntaxException, IOException {
        String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX bd: <http://www.bigdata.com/rdf#>\n" +
                "PREFIX wd: <" + WIKIDATA_ENTITY_PREFIX + ">\n" +
                "PREFIX wikibase: <http://wikiba.se/ontology#>\n" +
                "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n" +
                "SELECT ?i ?l WHERE {\n" +
                "  ?i wdt:P31 wd:Q427626.\n" +
                "  ?i rdfs:label ?l\n" +
                "}";

        URI req = new URI("https", "query.wikidata.org", "/sparql", "format=json&query=" + queryString, null);
        HttpRequestBase httpGet = new HttpGet(req);
        HttpResponse response = HttpUtil.getFailFastHttpClient().execute(httpGet);
        StatusLine statusLine = response.getStatusLine();
        HttpEntity entity = response.getEntity();
        String json = getJson(statusLine, entity);
        handleWikidataTaxonRanks(termListener, json);
    }

    static void handleWikidataTaxonRanks(TermListener termListener, String json) throws IOException {
        JsonNode jsonNode = new ObjectMapper().readTree(json);
        JsonNode results = jsonNode.get("results");
        JsonNode bindings = results.get("bindings");
        Map<String, String> ranks = new HashMap<>();
        for (JsonNode binding : bindings) {
            JsonNode item = binding.get("i");
            String value = item.get("value").getTextValue();

            JsonNode label = binding.get("l");
            String language = label.get("xml:lang").getTextValue();
            String labelString = label.get("value").getTextValue();
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

    private static String getJson(StatusLine statusLine, HttpEntity entity) throws IOException {
        if (statusLine.getStatusCode() >= 300) {
            EntityUtils.consume(entity);
            throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
        } else {
            return entity == null ? null : EntityUtils.toString(entity, StandardCharsets.UTF_8);
        }
    }

    public static TermListener createCacheWriter(PrintStream out) {
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

    public static void importTreatment(InputStream treatmentGraph, TaxonImportListener listener) {
        OntModel m = ModelFactory.createOntologyModel();
        m.read(treatmentGraph, null, "TURTLE");

        String queryString =
                "PREFIX tr: <http://www.thomsonreuters.com/>\n" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX trt: <http://plazi.org/vocab/treatment#>\n" +
                        "PREFIX fp: <http://filteredpush.org/ontologies/oa/dwcFP#>\n" +
                        "PREFIX dwc: <http://rs.tdwg.org/dwc/terms/>\n" +
                        "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                        "SELECT * WHERE { \n" +
                        "  ?treatment (trt:augmentsTaxonConcept|trt:definesTaxonConcept|trt:deprecates) ?tc .\n" +
                        "  ?tc trt:hasTaxonName ?tn .\n" +
                        "  ?tc a fp:TaxonConcept .\n" +
                        "  OPTIONAL { ?tc dwc:species ?species . }\n" +
                        "  OPTIONAL { ?tc dwc:genus ?genus . }\n" +
                        "  OPTIONAL { ?tc dwc:family ?family . }\n" +
                        "  OPTIONAL { ?tc dwc:class ?class . }\n" +
                        "  OPTIONAL { ?tc dwc:order ?order . }\n" +
                        "  OPTIONAL { ?tc dwc:phylum ?phylum . }\n" +
                        "  OPTIONAL { ?tc dwc:kingdom ?kingdom . }\n" +
                        "  OPTIONAL { ?tc dwc:rank ?rank . }\n" +
                        "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, m);
        ResultSet rs = qexec.execSelect();
        while (rs.hasNext()) {
            final QuerySolution next = rs.next();
            List<String> taxonRanks = Arrays.asList("?species", "?genus", "?family", "?order", "?class", "?phylum", "?kingdom");
            Map<String, String> taxonMap =
                    taxonRanks
                            .stream()
                            .map(key -> {
                                RDFNode value = next.get(key);
                                String valueString = value != null && value.isLiteral()
                                        ? value.asLiteral().getLexicalForm()
                                        : "";
                                return new AbstractMap.SimpleEntry<>(key.substring(1), valueString);
                            })
                            .filter(x -> org.apache.commons.lang3.StringUtils.isNoneBlank(x.getValue()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            RDFNode rdfNode = next.get("?tc");
            if (rdfNode != null && rdfNode.isURIResource()) {
                taxonMap.put(PropertyAndValueDictionary.EXTERNAL_ID, rdfNode.asResource().getURI());
            }

            String path = TaxonUtil.generateTaxonPath(taxonMap);
            taxonMap.put(PropertyAndValueDictionary.PATH, path);

            String[] pathSplit = org.apache.commons.lang3.StringUtils.split(path, CharsetConstant.SEPARATOR);

            taxonMap.put(PropertyAndValueDictionary.NAME, pathSplit.length > 0 ? pathSplit[pathSplit.length - 1] : "");

            String pathNames = TaxonUtil.generateTaxonPathNames(taxonMap);
            taxonMap.put(PropertyAndValueDictionary.PATH_NAMES, pathNames);

            String[] split = org.apache.commons.lang3.StringUtils.split(pathNames, CharsetConstant.SEPARATOR);
            taxonMap.put(PropertyAndValueDictionary.RANK, split.length > 0 ? split[split.length - 1] : "");


            Taxon taxon = TaxonUtil.mapToTaxon(taxonMap);
            listener.addTerm(taxon);

        }
    }

    interface TermListener {
        void onTerm(Taxon taxon);
    }
}
