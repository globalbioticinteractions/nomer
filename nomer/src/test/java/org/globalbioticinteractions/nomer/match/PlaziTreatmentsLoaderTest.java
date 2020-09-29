package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.Taxon;
import org.eol.globi.taxon.TaxonImportListener;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PlaziTreatmentsLoaderTest {

    @Test
    public void importFromFile() throws URISyntaxException {
        InputStream resourceAsStream = getClass().getResourceAsStream("plazi/000087F6E320FF95FF7EFDC1FAE4FA7B.ttl");
        InputStream treatmentGraph = resourceAsStream;
        AtomicInteger counter = new AtomicInteger(0);

        assertNotNull(treatmentGraph);

        List<Taxon> taxa = new ArrayList<>();
        TaxonImportListener listener = new TaxonImportListener() {

            @Override
            public void addTerm(Taxon term) {
                taxa.add(term);
                counter.getAndIncrement();
            }

            @Override
            public void addTerm(String key, Taxon term) {

            }

            @Override
            public void start() {

            }

            @Override
            public void finish() {

            }
        };

        PlaziTreatmentsLoader.importTreatment(treatmentGraph, listener);
        assertThat(counter.get(), Is.is(1));

        Taxon taxon = taxa.get(0);
        assertThat(taxon.getExternalId(), Is.is("http://taxon-concept.plazi.org/id/Animalia/Carvalhoma_Slater_1977"));
        assertThat(taxon.getName(), Is.is("Carvalhoma"));
        assertThat(taxon.getRank(), Is.is("genus"));

        assertThat(taxon.getPath(), Is.is("Animalia | Arthropoda | Insecta | Hemiptera | Miridae | Carvalhoma"));
        assertThat(taxon.getPathNames(), Is.is("kingdom | phylum | class | order | family | genus"));


    }


}
