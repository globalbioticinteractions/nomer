package org.globalbioticinteractions.nomer.match;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class RelationsOntologyLoaderTest {

    @Test
    public void importFromRO() throws IOException {
        OntModel m = ModelFactory.createOntologyModel();
        URL uri = new URL("https://raw.githubusercontent.com/oborel/obo-relations/master/ro.owl");
        m.read(uri.openStream(), "RDF/XML");

        String queryString = "SELECT * WHERE {{SELECT ?uri ?label ?definition " +
                "WHERE { " +
                //"?uri <http://www.geneontology.org/formats/oboInOwl#inSubset> <http://purl.obolibrary.org/obo/ro/subsets#ro-eco> . " +
                "?uri <http://www.w3.org/2000/01/rdf-schema#label> ?label . " +
                "?uri <http://www.w3.org/2000/01/rdf-schema#subPropertyOf>* <http://purl.obolibrary.org/obo/RO_0002437> . " +
                "?uri <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> ?parentUri . " +
                "OPTIONAL { ?uri <http://purl.obolibrary.org/obo/IAO_0000115> ?definition . }" +
                "}} UNION {" +
        "SELECT ?uri ?label ?definition ?altLabel " +
                "WHERE { " +
                "?uri <http://www.geneontology.org/formats/oboInOwl#inSubset> <http://purl.obolibrary.org/obo/ro/subsets#ro-eco> . " +
                "?uri <http://www.w3.org/2000/01/rdf-schema#label> ?label . " +
                "?inverseUri <http://www.w3.org/2000/01/rdf-schema#subPropertyOf>* <http://purl.obolibrary.org/obo/RO_0002437> . " +
                "?inverseUri <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> ?parentUri . " +
                "?uri <http://www.w3.org/2002/07/owl#inverseOf> ?inverseUri . " +
                "OPTIONAL { ?uri <http://purl.obolibrary.org/obo/IAO_0000115> ?definition . }" +
                "}}}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, m);
        ResultSet rs = qexec.execSelect();
        Set<String> interactionMap = new TreeSet<>();
        while (rs.hasNext()) {
            QuerySolution next = rs.next();
            RDFNode uri1 = next.get("uri");
            RDFNode label = next.get("label");
            RDFNode definition = next.get("definition");
            interactionMap.add(uri1.toString() +
                    "\t" + label.toString() +
                    "\t" + (definition == null ? "needs definition" : definition.toString()));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream writer = new PrintStream(out, true, StandardCharsets.UTF_8.name());
        writer.println("IRI\tlabel\tdefinition");

        interactionMap.forEach(writer::println);
        writer.flush();
        writer.close();

        String actual = StringUtils.trim(out.toString(StandardCharsets.UTF_8.name()));

        System.out.println(actual);

        assertThat(actual, is(IOUtils.toString(getClass().getResourceAsStream("ro.tsv"), StandardCharsets.UTF_8)));
    }

}
