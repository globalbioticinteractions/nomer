package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.Taxon;
import org.eol.globi.taxon.TaxonCacheListener;
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
        InputStream treatmentGraph = getClass().getResourceAsStream("plazi/000087F6E320FF95FF7EFDC1FAE4FA7B.ttl");
        AtomicInteger counter = new AtomicInteger(0);

        assertNotNull(treatmentGraph);

        List<Taxon> taxa = new ArrayList<>();
        TaxonCacheListener listener = new TaxonCacheListener() {

            @Override
            public void addTaxon(Taxon term) {
                taxa.add(term);
                counter.getAndIncrement();
            }

            @Override
            public void start() {

            }

            @Override
            public void finish() {

            }
        };

        PlaziTreatmentsLoader.importTreatment(treatmentGraph, listener);
        assertThat(counter.get(), Is.is(2));

        Taxon taxon = taxa.get(0);
        assertThat(taxon.getExternalId(), Is.is("http://treatment.plazi.org/id/000087F6E320FF95FF7EFDC1FAE4FA7B"));
        assertThat(taxon.getName(), Is.is("Carvalhoma"));
        assertThat(taxon.getRank(), Is.is("genus"));

        assertThat(taxon.getPath(), Is.is("Animalia | Arthropoda | Insecta | Hemiptera | Miridae | Carvalhoma"));
        assertThat(taxon.getPathNames(), Is.is("kingdom | phylum | class | order | family | genus"));

        Taxon secondTaxon = taxa.get(1);
        assertThat(secondTaxon.getExternalId(), Is.is("doi:10.5281/zenodo.3854772"));
        assertThat(secondTaxon.getName(), Is.is("Carvalhoma"));
        assertThat(secondTaxon.getRank(), Is.is("genus"));

        assertThat(secondTaxon.getPath(), Is.is("Animalia | Arthropoda | Insecta | Hemiptera | Miridae | Carvalhoma"));
        assertThat(secondTaxon.getPathNames(), Is.is("kingdom | phylum | class | order | family | genus"));

    }

    @Test
    public void importFromRhinolophusSinicus() throws URISyntaxException {
        InputStream treatmentGraph = getClass().getResourceAsStream("plazi/03AF87D3C435B542FF728049FB55BB1B.ttl");
        AtomicInteger counter = new AtomicInteger(0);

        assertNotNull(treatmentGraph);

        List<Taxon> taxa = new ArrayList<>();
        TaxonCacheListener listener = new TaxonCacheListener() {

            @Override
            public void addTaxon(Taxon term) {
                taxa.add(term);
                counter.getAndIncrement();
            }

            @Override
            public void start() {

            }

            @Override
            public void finish() {

            }
        };

        PlaziTreatmentsLoader.importTreatment(treatmentGraph, listener);
        assertThat(counter.get(), Is.is(2));

        Taxon taxon = taxa.get(0);
        assertThat(taxon.getExternalId(), Is.is("http://treatment.plazi.org/id/03AF87D3C435B542FF728049FB55BB1B"));
        assertThat(taxon.getName(), Is.is("Rhinolophus sinicus"));
        assertThat(taxon.getRank(), Is.is("species"));

        Taxon secondTaxon = taxa.get(1);
        assertThat(secondTaxon.getExternalId(), Is.is("doi:10.3161/150811009X465703"));
        assertThat(secondTaxon.getName(), Is.is("Rhinolophus sinicus"));
        assertThat(secondTaxon.getRank(), Is.is("species"));

    }


}
