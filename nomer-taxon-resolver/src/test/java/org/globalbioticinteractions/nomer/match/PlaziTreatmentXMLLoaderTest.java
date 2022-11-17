package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.Taxon;
import org.eol.globi.taxon.TaxonCacheListener;
import org.hamcrest.core.Is;
import org.junit.Ignore;
import org.junit.Test;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class PlaziTreatmentXMLLoaderTest extends PlaziTreatmentLoaderTest {

    public PlaziTreatmentLoader createLoader() {
        return new PlaziTreatmentXMLLoader();
    }

    public String getExtension() {
        return ".xml";
    }

    @Ignore
    @Test
    public void importFromFile() throws URISyntaxException {
        InputStream treatmentGraph = getClass().getResourceAsStream("plazi/000087F6E320FF95FF7EFDC1FAE4FA7B" + getExtension());
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

        createLoader().loadTreatment(treatmentGraph, listener);
        assertThat(counter.get(), Is.is(3));

        Taxon taxon = taxa.get(0);
        assertThat(taxon.getExternalId(), Is.is("GBIF:164283321"));
        assertThat(taxon.getName(), Is.is("Carvalhoma"));

        taxon = taxa.get(1);
        assertThat(taxon.getExternalId(), Is.is("doi:10.5281/zenodo.3854772"));
        assertThat(taxon.getName(), Is.is("Carvalhoma"));

        taxon = taxa.get(2);
        assertThat(taxon.getExternalId(), Is.is("http://taxon-concept.plazi.org/id/Animalia/Carvalhoma_Slater_1977"));
        assertThat(taxon.getName(), Is.is("Carvalhoma"));
        assertThat(taxon.getRank(), Is.is("genus"));

        assertThat(taxon.getPath(), Is.is("Animalia | Arthropoda | Insecta | Hemiptera | Miridae | Carvalhoma"));
        assertThat(taxon.getPathNames(), Is.is("kingdom | phylum | class | order | family | genus"));


    }

    @Test
    public void importFromRhinolophusSinicus() throws URISyntaxException {
        InputStream treatmentGraph = getClass().getResourceAsStream("plazi/03AF87D3C435B542FF728049FB55BB1B" + getExtension());
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

        createLoader().loadTreatment(treatmentGraph, listener);
        assertThat(counter.get(), Is.is(2));

        Taxon taxon = taxa.get(0);
        assertThat(taxon.getExternalId(), Is.is("http://treatment.plazi.org/id/03AF87D3C435B542FF728049FB55BB1B"));
        assertThat(taxon.getName(), Is.is("\nRhinolophus sinicus\nK. Andersen, 1905\n"));

        taxon = taxa.get(1);
        assertThat(taxon.getExternalId(), Is.is("urn:lsid:plazi:treatment:03AF87D3C435B542FF728049FB55BB1B"));
        assertThat(taxon.getName(), Is.is("\nRhinolophus sinicus\nK. Andersen, 1905\n"));

    }

    @Test
    public void importWithSubspecies() throws URISyntaxException {
        InputStream treatmentGraph = getClass().getResourceAsStream("plazi/0B6AC9BA1E03488CE06DCAA62DC4AA02" + getExtension());
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


        createLoader().loadTreatment(treatmentGraph, listener);
        assertThat(counter.get(), Is.is(3));

        Taxon taxon = taxa.get(0);
        assertThat(taxon.getExternalId(), Is.is("GBIF:164809869"));
        assertThat(taxon.getName(), Is.is("Homo sapiens subsp. ferus"));

        taxon = taxa.get(1);
        assertThat(taxon.getExternalId(), Is.is("http://treatment.plazi.org/id/0B6AC9BA1E03488CE06DCAA62DC4AA02"));
        assertThat(taxon.getName(), Is.is("Homo sapiens subsp. ferus"));

        taxon = taxa.get(2);
        assertThat(taxon.getExternalId(), Is.is("urn:lsid:zoobank.org:act:ADAA382F-6794-4AB2-B43B-5412B0394005"));
        assertThat(taxon.getName(), Is.is("Homo sapiens subsp. ferus"));


    }



}
