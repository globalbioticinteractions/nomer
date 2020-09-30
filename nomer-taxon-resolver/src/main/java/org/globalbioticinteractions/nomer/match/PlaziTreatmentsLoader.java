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
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.TaxonCacheListener;

import java.io.InputStream;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlaziTreatmentsLoader {

    public static void importTreatment(InputStream treatmentGraph, TaxonCacheListener listener) {
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
                        "  ?treatment trt:publishedIn ?publication .\n" +
                        "  OPTIONAL { ?tc dwc:species ?specificEpithet . }\n" +
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
            List<String> taxonRanks = Arrays.asList("?specificEpithet", "?genus", "?family", "?order", "?class", "?phylum", "?kingdom");
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

            RDFNode rdfNode = next.get("?treatment");
            if (rdfNode != null && rdfNode.isURIResource()) {
                taxonMap.put(PropertyAndValueDictionary.EXTERNAL_ID, rdfNode.asResource().getURI());
            }

            String path = TaxonUtil.generateTaxonPath(taxonMap);
            taxonMap.put(PropertyAndValueDictionary.PATH, path);

            String value = TaxonUtil.generateTaxonName(taxonMap, taxonRanks, "genus", "specificEpithet", "subspecificEpithet", "species");
            if (StringUtils.isBlank(value)) {
                String[] pathSplit = org.apache.commons.lang3.StringUtils.split(path, CharsetConstant.SEPARATOR);
                value = pathSplit.length > 0 ? pathSplit[pathSplit.length - 1] : "";
            }
            taxonMap.put(PropertyAndValueDictionary.NAME, value);

            String pathNames = TaxonUtil.generateTaxonPathNames(taxonMap);
            taxonMap.put(PropertyAndValueDictionary.PATH_NAMES, pathNames);

            String[] split = org.apache.commons.lang3.StringUtils.split(pathNames, CharsetConstant.SEPARATOR);
            taxonMap.put(PropertyAndValueDictionary.RANK, split.length > 0 ? split[split.length - 1] : "");


            Taxon taxon = TaxonUtil.mapToTaxon(taxonMap);
            listener.addTaxon(taxon);

        }
    }

}
