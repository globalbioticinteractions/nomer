package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;
import org.globalbioticinteractions.nomer.cmd.OutputFormat;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.eol.globi.service.TaxonUtil.taxonToMap;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class PlaziServiceTest {

    @Test
    public void match() throws PropertyEnricherException {

        TermMatcher service = createService();

        List<Taxon> found = new ArrayList<>();
        service.match(
                Collections.singletonList(new TermImpl(null, "Carvalhoma")), new TermMatchListener() {
                    @Override
                    public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                        found.add(taxon);
                        assertThat(nameType, is(NameType.OCCURS_IN));
                    }
                });


        assertThat(found.size(), is(3));
        Taxon taxon = found.get(0);
        assertThat(taxon.getPath(), is("Animalia | Arthropoda | Insecta | Hemiptera | Miridae | Carvalhoma"));
        assertThat(taxon.getExternalId(), is("http://taxon-concept.plazi.org/id/Animalia/Carvalhoma_Slater_1977"));
        assertThat(taxon.getPathNames(), is("kingdom | phylum | class | order | family | genus"));
        assertThat(taxon.getName(), is("Carvalhoma"));
    }

    @Test
    public void noMatch() throws PropertyEnricherException {

        TermMatcher service = createService();

        List<Taxon> found = new ArrayList<>();
        service.match(
                Collections.singletonList(new TermImpl(null, "Donald duck")), new TermMatchListener() {
                    @Override
                    public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                        found.add(taxon);
                        assertThat(nameType, is(NameType.NONE));
                    }
                });


        assertThat(found.size(), is(1));
        Taxon taxon = found.get(0);
        assertThat(taxon.getPath(), is(nullValue()));
        assertThat(taxon.getExternalId(), is(nullValue()));
        assertThat(taxon.getPathNames(), is(nullValue()));
        assertThat(taxon.getName(), is("Donald duck"));
    }


    @Test
    public void enrichById() throws PropertyEnricherException {
        TermMatcher service = createService();

        List<Taxon> found = new ArrayList<>();
        service.match(
                Collections.singletonList(new TermImpl("http://treatment.plazi.org/id/000087F6E320FF95FF7EFDC1FAE4FA7B", null)), new TermMatchListener() {
                    @Override
                    public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                        found.add(taxon);
                    }
                });


        assertThat(found.size(), is(1));
        Taxon taxon = found.get(0);
        String expectedTreatment = "http://treatment.plazi.org/id/000087F6E320FF95FF7EFDC1FAE4FA7B";
        assertThat(taxon.getExternalId(), is(expectedTreatment));
        assertThat(taxon.getPath(), is(expectedTreatment));
        assertThat(taxon.getName(), is(expectedTreatment));
    }

    @Test
    public void enrichByShortId() throws PropertyEnricherException {

        TermMatcher service = createService();

        List<Taxon> found = new ArrayList<>();
        service.match(
                Collections.singletonList(new TermImpl("PLAZI:000087F6E320FF95FF7EFDC1FAE4FA7B", null)), new TermMatchListener() {
                    @Override
                    public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                        found.add(taxon);
                    }
                });


        Taxon taxon1 = found.get(0);
        String expectedTreatmentId = "http://treatment.plazi.org/id/000087F6E320FF95FF7EFDC1FAE4FA7B";
        assertThat(taxon1.getExternalId(), is(expectedTreatmentId));
        assertThat(taxon1.getPath(), is(expectedTreatmentId));
        assertThat(taxon1.getName(), is(expectedTreatmentId));
        assertThat(taxon1.getPathNames(), is(nullValue()));
    }

    @Test
    public void enrichByDoi() throws PropertyEnricherException {
        TermMatcher service = createService();

        List<Taxon> found = new ArrayList<>();
        service.match(
                Collections.singletonList(new TermImpl("doi:10.5281/zenodo.3854772", null)),
                (aLong, term, nameType , taxon) -> found.add(taxon));


        assertThat(found.size(), is(1));
        Taxon taxon1 = found.get(0);
        assertThat(taxon1.getPath(), is("doi:10.5281/zenodo.3854772"));
        assertThat(taxon1.getExternalId(), is("doi:10.5281/zenodo.3854772"));
        assertThat(taxon1.getPathNames(), is(nullValue()));
        assertThat(taxon1.getName(), is("doi:10.5281/zenodo.3854772"));
    }


    @Test
    public void enrichNoMatch() throws PropertyEnricherException {
        TermMatcher service = createService();

        AtomicInteger counter = new AtomicInteger();
        service.match(
                Collections.singletonList(new TermImpl("ITIS:999999999", null)), new TermMatchListener() {
                    @Override
                    public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                        counter.getAndIncrement();
                        assertThat(nameType, is(NameType.NONE));
                    }
                });

        assertThat(counter.get(), is(1));
    }

    @Test
    public void enrichPrefixMismatch() throws PropertyEnricherException {
        TermMatcher service = createService();

        AtomicInteger counter = new AtomicInteger();
        service.match(
                Collections.singletonList(new TermImpl("FOO:2", null)), new TermMatchListener() {
                    @Override
                    public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                        counter.getAndIncrement();
                        assertThat(nameType, is(NameType.NONE));
                    }
                });

        assertThat(counter.get(), is(1));
    }

    private TermMatcher createService() {
        File file = new File("target/cache" + UUID.randomUUID());
        return new PlaziService(new TermMatcherContext() {
            @Override
            public String getCacheDir() {
                return file.getAbsolutePath();
            }

            @Override
            public InputStream retrieve(URI uri) throws IOException {
                return getClass().getResourceAsStream(uri.toString());
            }

            @Override
            public List<String> getMatchers() {
                return null;
            }

            @Override
            public Map<Integer, String> getInputSchema() {
                return null;
            }

            @Override
            public Map<Integer, String> getOutputSchema() {
                return null;
            }

            @Override
            public OutputFormat getOutputFormat() {
                return null;
            }

            @Override
            public String getProperty(String key) {
                return new TreeMap<String, String>() {
                    {
                        put("nomer.plazi.treatments.archive", "/org/globalbioticinteractions/nomer/match/plazi/treatments.zip");
                    }
                }.get(key);
            }
        });
    }


}