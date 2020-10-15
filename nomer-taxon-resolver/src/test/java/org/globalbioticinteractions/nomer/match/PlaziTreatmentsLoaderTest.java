package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.Taxon;
import org.eol.globi.taxon.TaxonCacheListener;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;

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
        assertThat(counter.get(), Is.is(3));

        Taxon taxon = taxa.get(1);
        assertThat(taxon.getExternalId(), Is.is("http://treatment.plazi.org/id/000087F6E320FF95FF7EFDC1FAE4FA7B"));
        assertThat(taxon.getName(), Is.is("Carvalhoma"));

        taxon = taxa.get(2);
        assertThat(taxon.getExternalId(), Is.is("doi:10.5281/zenodo.3854772"));
        assertThat(taxon.getName(), Is.is("Carvalhoma"));

        taxon = taxa.get(0);
        assertThat(taxon.getExternalId(), Is.is("http://taxon-concept.plazi.org/id/Animalia/Carvalhoma_Slater_1977"));
        assertThat(taxon.getName(), Is.is("Carvalhoma"));
        assertThat(taxon.getRank(), Is.is("genus"));

        assertThat(taxon.getPath(), Is.is("Animalia | Arthropoda | Insecta | Hemiptera | Miridae | Carvalhoma"));
        assertThat(taxon.getPathNames(), Is.is("kingdom | phylum | class | order | family | genus"));


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
        assertThat(counter.get(), Is.is(3));

        Taxon taxon = taxa.get(1);
        assertThat(taxon.getExternalId(), Is.is("http://treatment.plazi.org/id/03AF87D3C435B542FF728049FB55BB1B"));
        assertThat(taxon.getName(), Is.is("Rhinolophus sinicus"));

        Taxon secondTaxon = taxa.get(2);
        assertThat(secondTaxon.getExternalId(), Is.is("doi:10.3161/150811009X465703"));
        assertThat(secondTaxon.getName(), Is.is("Rhinolophus sinicus"));

    }

    @Test
    public void importWithSubspecies() throws URISyntaxException {
        InputStream treatmentGraph = getClass().getResourceAsStream("plazi/0B6AC9BA1E03488CE06DCAA62DC4AA02.ttl");
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
        assertThat(counter.get(), Is.is(3));

        Taxon taxon = taxa.get(0);
        assertThat(taxon.getExternalId(), Is.is("http://taxon-concept.plazi.org/id/Animalia/Homo_sapiens_ferus_Linnaeus_1758"));
        assertThat(taxon.getName(), Is.is("Homo sapiens ferus"));
        assertThat(taxon.getPath(), Is.is("Animalia | Chordata | Mammalia | Primates | Hominidae | Homo | Homo sapiens ferus"));
        assertThat(taxon.getRank(), Is.is("species"));

    }


}
