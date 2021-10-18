package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatchListener;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class DiscoverLifeTaxonServiceTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void lookupByName() throws PropertyEnricherException {
        DiscoverLifeTaxonService discoverLifeTaxonService
                = new DiscoverLifeTaxonService(getTmpContext());
        assertLookup(discoverLifeTaxonService);
        assertLookup(discoverLifeTaxonService);
    }

    @Test
    public void lookupBySynonym() throws PropertyEnricherException {
        DiscoverLifeTaxonService discoverLifeTaxonService
                = new DiscoverLifeTaxonService(getTmpContext());
        assertLookupSynonym(discoverLifeTaxonService);
        assertLookupSynonym(discoverLifeTaxonService);
    }

    @Test
    public void lookupByNonExistingName() throws PropertyEnricherException {
        AtomicReference<NameType> noMatch = new AtomicReference<>(null);
        DiscoverLifeTaxonService discoverLifeTaxonService = new DiscoverLifeTaxonService(getTmpContext());
        discoverLifeTaxonService.match(Arrays.asList(new TaxonImpl("Donald duck")), new TermMatchListener() {

            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, Taxon resolvedTaxon, NameType nameType) {
                noMatch.set(nameType);
            }
        });

        assertThat(noMatch.get(), Is.is(NameType.NONE));
    }

    private void assertLookup(DiscoverLifeTaxonService discoverLifeTaxonService) throws PropertyEnricherException {
        final AtomicInteger counter = new AtomicInteger(0);

        String providedName = "Acamptopoeum argentinum";
        List<Term> termsToBeMatched = Collections.singletonList(new TaxonImpl(providedName));
        discoverLifeTaxonService
                .match(termsToBeMatched, new TermMatchListener() {
                    @Override
                    public void foundTaxonForTerm(Long requestId, Term providedTerm, Taxon resolvedTaxon, NameType nameType) {
                        assertThat(providedTerm.getName(), Is.is(providedName));
                        assertThat(nameType, Is.is(NameType.HAS_ACCEPTED_NAME));
                        assertThat(resolvedTaxon.getPath(), Is.is("Animalia | Arthropoda | Insecta | Hymenoptera | Andrenidae | Acamptopoeum argentinum"));
                        assertThat(resolvedTaxon.getPathIds(), Is.is("https://www.discoverlife.org/mp/20q?search=Animalia | https://www.discoverlife.org/mp/20q?search=Arthropoda | https://www.discoverlife.org/mp/20q?search=Insecta | https://www.discoverlife.org/mp/20q?search=Hymenoptera | https://www.discoverlife.org/mp/20q?search=Andrenidae | https://www.discoverlife.org/mp/20q?search=Acamptopoeum+argentinum"));
                        assertThat(resolvedTaxon.getPathNames(), Is.is("kingdom | phylum | class | order | family | species"));
                        assertThat(resolvedTaxon.getName(), Is.is("Acamptopoeum argentinum"));
                        assertThat(resolvedTaxon.getRank(), Is.is("species"));
                        assertThat(resolvedTaxon.getId(), Is.is("https://www.discoverlife.org/mp/20q?search=Acamptopoeum+argentinum"));
                        counter.getAndIncrement();
                    }
                });

        assertThat(counter.get(), Is.is(1));
    }

    private void assertLookupSynonym(DiscoverLifeTaxonService discoverLifeTaxonService) throws PropertyEnricherException {
        final AtomicInteger counter = new AtomicInteger(0);

        String providedName = "Perdita argentina";
        List<Term> termsToBeMatched = Collections.singletonList(new TaxonImpl(providedName));
        discoverLifeTaxonService
                .match(termsToBeMatched, new TermMatchListener() {
                    @Override
                    public void foundTaxonForTerm(Long requestId, Term providedTerm, Taxon resolvedTaxon, NameType nameType) {
                        assertThat(providedTerm.getName(), Is.is(providedName));
                        assertThat(nameType, Is.is(NameType.SYNONYM_OF));
                        assertThat(resolvedTaxon.getName(), Is.is("Acamptopoeum argentinum"));
                        assertThat(resolvedTaxon.getId(), Is.is("https://www.discoverlife.org/mp/20q?search=Acamptopoeum+argentinum"));
                        counter.getAndIncrement();
                    }
                });

        assertThat(counter.get(), Is.is(1));
    }

    private TermMatcherContext getTmpContext() {
        return new TermMatcherContext() {
            @Override
            public String getCacheDir() {
                try {
                    return folder.newFolder("cacheDir").getAbsolutePath();
                } catch (IOException e) {
                    throw new IllegalStateException("kaboom!");
                }
            }

            @Override
            public InputStream getResource(String uri) throws IOException {
                return null;
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
            public String getProperty(String key) {
                return null;
            }
        };
    }

}