package org.globalbioticinteractions.nomer.match;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.HttpUtil;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class RelationsOntologyLoaderTest {

    @Test
    public void importFromRO() throws IOException {
        OntModel m = ModelFactory.createOntologyModel();
        URL uri = new URL("https://raw.githubusercontent.com/oborel/obo-relations/master/ro.owl");
        m.read(uri.openStream(), "RDF/XML");

        String queryString = "SELECT * WHERE {{SELECT ?uri ?label " +
                "WHERE { " +
                //"?uri <http://www.geneontology.org/formats/oboInOwl#inSubset> <http://purl.obolibrary.org/obo/ro/subsets#ro-eco> . " +
                "?uri <http://www.w3.org/2000/01/rdf-schema#label> ?label . " +
                "?uri <http://www.w3.org/2000/01/rdf-schema#subPropertyOf>* <http://purl.obolibrary.org/obo/RO_0002437> . " +
                "?uri <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> ?parentUri . " +
                "}} UNION {" +
        "SELECT ?uri ?label ?altLabel " +
                "WHERE { " +
                "?uri <http://www.geneontology.org/formats/oboInOwl#inSubset> <http://purl.obolibrary.org/obo/ro/subsets#ro-eco> . " +
                "?uri <http://www.w3.org/2000/01/rdf-schema#label> ?label . " +
                "?inverseUri <http://www.w3.org/2000/01/rdf-schema#subPropertyOf>* <http://purl.obolibrary.org/obo/RO_0002437> . " +
                "?inverseUri <http://www.w3.org/2000/01/rdf-schema#subPropertyOf> ?parentUri . " +
                "?uri <http://www.w3.org/2002/07/owl#inverseOf> ?inverseUri . " +
                "}}}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, m);
        ResultSet rs = qexec.execSelect();
        Set<String> interactionMap = new TreeSet<>();
        while (rs.hasNext()) {
            QuerySolution next = rs.next();
            RDFNode uri1 = next.get("uri");
            RDFNode label = next.get("label");
            interactionMap.add(uri1.toString() + "\t" + label.toString());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream writer = new PrintStream(out, true, StandardCharsets.UTF_8.name());
        writer.println("IRI\tlabel");

        interactionMap.forEach(writer::println);
        writer.flush();
        writer.close();

        String actual = out.toString(StandardCharsets.UTF_8.name());
        assertThat(actual, is(IOUtils.toString(getClass().getResourceAsStream("ro.tsv"), StandardCharsets.UTF_8)));
    }

}
