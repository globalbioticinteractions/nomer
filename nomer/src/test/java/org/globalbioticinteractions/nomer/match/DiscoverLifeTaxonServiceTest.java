package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.DiscoverLifeUtil;
import org.eol.globi.taxon.TermMatchListener;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;

public class DiscoverLifeTaxonServiceTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private DiscoverLifeTaxonService discoverLifeTaxonService;

    @Before
    public void init() throws PropertyEnricherException {
        discoverLifeTaxonService = new DiscoverLifeTaxonService(getTmpContext());
        discoverLifeTaxonService.match(Arrays.asList(new TermImpl(null, "Apis mellifera"))
                , new TermMatchListener() {
                    @Override
                    public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {

                    }
                });
    }

    @After
    public void close() {
        discoverLifeTaxonService.close();
    }

    @Test
    public void lookupByName() throws PropertyEnricherException {
        assertLookup(discoverLifeTaxonService);
        assertLookup(discoverLifeTaxonService);
    }

    @Test
    public void lookupByNullName() throws PropertyEnricherException {
        final AtomicInteger counter = new AtomicInteger(0);

        String providedName = null;
        List<Term> termsToBeMatched = Collections.singletonList(new TaxonImpl(providedName));
        discoverLifeTaxonService
                .match(termsToBeMatched, new TermMatchListener() {
                    @Override
                    public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                        assertThat(nameType, Is.is(NameType.NONE));
                        counter.getAndIncrement();
                    }
                });

        assertThat(counter.get(), Is.is(1));
    }

    @Test
    public void lookupBySynonym() throws PropertyEnricherException {
        assertLookupSynonym(discoverLifeTaxonService);
        assertLookupSynonym(discoverLifeTaxonService);
    }


    @Test
    public void lookupByHomonym() throws PropertyEnricherException {
        final AtomicInteger homonymCounter = new AtomicInteger(0);

        String providedName = "Andrena proxima";
        List<Term> termsToBeMatched = Collections.singletonList(new TaxonImpl(providedName));
        discoverLifeTaxonService
                .match(termsToBeMatched, new TermMatchListener() {
                    @Override
                    public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                        if (NameType.HOMONYM_OF.equals(nameType)) {
                            Taxon providedTaxon = (Taxon) providedTerm;
                            assertThat(providedTaxon.getName(), Is.is(providedName));
                            assertThat(nameType, Is.is(NameType.HOMONYM_OF));
                            assertThat(resolvedTaxon.getName(), Is.is(providedName));
                            assertThat(resolvedTaxon.getAuthorship(), Is.is("(Kirby, 1802)"));
                            homonymCounter.getAndIncrement();
                        }
                    }
                });

        assertThat(homonymCounter.get(), Is.is(1));
    }

    @Test
    public void lookupByHomonymWithParenthesis() throws PropertyEnricherException {
        final AtomicInteger homonymCounter = new AtomicInteger(0);

        final String providedName = "Xylocopa (Proxylocopa) sinensis";
        List<Term> termsToBeMatched = Collections.singletonList(new TaxonImpl(providedName));
        discoverLifeTaxonService
                .match(termsToBeMatched, new TermMatchListener() {
                    @Override
                    public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                        if (NameType.HOMONYM_OF.equals(nameType)) {
                            assertThat(providedTerm.getName(), Is.is(providedName));
                            assertThat(nameType, Is.is(NameType.HOMONYM_OF));
                            assertThat(resolvedTaxon.getName(), Is.is("Xylocopa sinensis"));
                            assertThat(resolvedTaxon.getAuthorship(), Is.is("Smith, 1854"));
                            homonymCounter.getAndIncrement();
                        }
                    }
                });

        assertThat(homonymCounter.get(), Is.is(1));
    }

    @Test
    public void trimScientificName() {
        String actual = "Xylocopa (Proxylocopa) sinensis";
        String trimmedName = DiscoverLifeUtil.trimScientificName(actual);
        assertThat(trimmedName, Is.is("Xylocopa sinensis"));
    }

    @Test
    public void lookupByNoMatchHomonym() throws PropertyEnricherException {
        final AtomicInteger homonymCounter = new AtomicInteger(0);

        String providedName = "Apis muraria";
        List<Term> termsToBeMatched = Collections.singletonList(new TaxonImpl(providedName));
        discoverLifeTaxonService
                .match(termsToBeMatched, new TermMatchListener() {
                    @Override
                    public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                        if (NameType.HOMONYM_OF.equals(nameType)) {
                            assertThat(providedTerm.getName(), Is.is(providedName));
                            assertThat(nameType, Is.is(NameType.HOMONYM_OF));
                            assertThat(resolvedTaxon.getName(), Is.is(PropertyAndValueDictionary.NO_MATCH));
                            assertThat(resolvedTaxon.getId(), Is.is(PropertyAndValueDictionary.NO_MATCH));
                            homonymCounter.getAndIncrement();
                        }
                    }
                });

        assertThat(homonymCounter.get(), Is.is(1));
    }

    @Test
    public void lookupSubspecies() throws PropertyEnricherException {
        final AtomicInteger homonymCounter = new AtomicInteger(0);

        String providedName = "Pseudopanurgus nebrascensis timberlakei";
        List<Term> termsToBeMatched = Collections.singletonList(new TaxonImpl(".*", ".*"));
        discoverLifeTaxonService
                .match(termsToBeMatched, new TermMatchListener() {
                    @Override
                    public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                        if (NameType.HOMONYM_OF.equals(nameType)
                                && StringUtils.equals(providedTerm.getName(), providedName)) {
                            Taxon providedTaxon = (Taxon) providedTerm;
                            assertThat(providedTaxon.getName(), Is.is(providedName));
                            assertThat(providedTaxon.getRank(), Is.is("subspecies"));
                            assertThat(providedTaxon.getAuthorship(), Is.is("Michener, 1947"));

                            assertThat(resolvedTaxon.getName(), Is.is("no:match"));
                            assertThat(resolvedTaxon.getId(), Is.is("no:match"));

                            homonymCounter.getAndIncrement();
                        }
                    }
                });

        assertThat(homonymCounter.get(), Is.is(1));
    }

    @Test
    public void lookupVariety() throws PropertyEnricherException {
        final AtomicInteger homonymCounter = new AtomicInteger(0);

        String providedName = "Psithyrus (Allopsithyrus) barbutellus var bimaculatus";
        List<Term> termsToBeMatched = Collections.singletonList(new TaxonImpl(providedName));
        discoverLifeTaxonService
                .match(termsToBeMatched, new TermMatchListener() {
                    @Override
                    public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                        if (NameType.HOMONYM_OF.equals(nameType)) {
                            Taxon providedTaxon = (Taxon) providedTerm;
                            assertThat(providedTaxon.getName(), Is.is(providedName));
                            homonymCounter.getAndIncrement();
                        }
                    }
                });

        assertThat(homonymCounter.get(), Is.is(1));
    }

    @Test
    public void matchAll() throws PropertyEnricherException {
        final AtomicInteger counter = new AtomicInteger(0);

        List<Term> termsToBeMatched = Collections.singletonList(
                new TaxonImpl(".*", ".*"));
        discoverLifeTaxonService
                .match(termsToBeMatched, new TermMatchListener() {
                    @Override
                    public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                        if (counter.get() == 0) {
                            assertThat(providedTerm.getName(), Is.is("Acamptopoeum argentinum"));
                            assertThat(nameType, Is.is(NameType.HAS_ACCEPTED_NAME));
                            assertThat(resolvedTaxon.getName(), Is.is("Acamptopoeum argentinum"));
                            assertThat(resolvedTaxon.getId(), Is.is("https://www.discoverlife.org/mp/20q?search=Acamptopoeum+argentinum"));
                        }

                        counter.getAndIncrement();
                    }
                });

        assertThat(counter.get(), Is.is(50107));
    }

    @Test
    public void lookupByNonExistingName() throws PropertyEnricherException {
        AtomicReference<NameType> noMatch = new AtomicReference<>(null);
        discoverLifeTaxonService.match(Collections.singletonList(new TaxonImpl("Donald duck")), new TermMatchListener() {

            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
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
                    public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
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
                    public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
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
                    return folder.newFolder().getAbsolutePath();
                } catch (IOException e) {
                    throw new IllegalStateException("kaboom!", e);
                }
            }

            @Override
            public InputStream retrieve(URI uri) throws IOException {
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
            public String getOutputFormat() {
                return null;
            }

            @Override
            public String getProperty(String key) {
                return null;
            }
        };
    }

}