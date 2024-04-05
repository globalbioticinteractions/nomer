package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.DiscoverLifeUtilXHTML;
import org.eol.globi.taxon.TermMatchListener;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static junit.framework.TestCase.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class DiscoverLifeTaxonServiceTestBase {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private DiscoverLifeTaxonService discoverLifeTaxonService;

    @Test
    public void lookupByName() throws PropertyEnricherException {
        assertLookup(getDiscoverLifeTaxonService());
        assertLookup(getDiscoverLifeTaxonService());
    }

    @Test
    public void lookupByNullName() throws PropertyEnricherException {
        final AtomicInteger counter = new AtomicInteger(0);

        String providedName = null;
        List<Term> termsToBeMatched = Collections.singletonList(new TaxonImpl(providedName));
        getDiscoverLifeTaxonService()
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
        assertLookupSynonym();
        assertLookupSynonym();
    }


    @Test
    public void lookupByHomonym() throws PropertyEnricherException {
        final AtomicInteger homonymCounter = new AtomicInteger(0);

        String providedName = "Andrena proxima";
        List<Term> termsToBeMatched = Collections.singletonList(new TaxonImpl(providedName));
        getDiscoverLifeTaxonService()
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
        getDiscoverLifeTaxonService()
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
        String trimmedName = DiscoverLifeUtilXHTML.trimScientificName(actual);
        assertThat(trimmedName, Is.is("Xylocopa sinensis"));
    }

    @Test
    public void lookupByNoMatchHomonym() throws PropertyEnricherException {
        final AtomicInteger homonymCounter = new AtomicInteger(0);

        String providedName = "Apis muraria";
        List<Term> termsToBeMatched = Collections.singletonList(new TaxonImpl(providedName));
        getDiscoverLifeTaxonService()
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
        getDiscoverLifeTaxonService()
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
        getDiscoverLifeTaxonService()
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
    // see https://github.com/GlobalNamesArchitecture/dwca_hunter/issues/53
    // see https://github.com/globalbioticinteractions/nomer/issues/72
    public void ignoreSelfReferentialSynonyms() throws PropertyEnricherException {
        final AtomicInteger homonymCounter = new AtomicInteger(0);
        final AtomicInteger synonymCounter = new AtomicInteger(0);
        final AtomicInteger acceptedNameCounter = new AtomicInteger(0);

        String providedName = "Pseudapis neumayeri";
        List<Term> termsToBeMatched = Collections.singletonList(new TaxonImpl(providedName));
        getDiscoverLifeTaxonService()
                .match(termsToBeMatched, new TermMatchListener() {
                    @Override
                    public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                        if (NameType.HOMONYM_OF.equals(nameType)) {
                            homonymCounter.getAndIncrement();
                        }
                        if (NameType.SYNONYM_OF.equals(nameType)) {
                            Taxon providedTaxon = (Taxon) providedTerm;
                            assertFalse(DiscoverLifeUtilXHTML.isSelfReferential(providedTaxon, resolvedTaxon));
                            synonymCounter.getAndIncrement();
                        }
                        if (NameType.HAS_ACCEPTED_NAME.equals(nameType)) {
                            Taxon providedTaxon = (Taxon) providedTerm;
                            assertThat(resolvedTaxon.getName(), Is.is(providedTaxon.getName()));
                            assertThat(resolvedTaxon.getExternalId(), Is.is("https://www.discoverlife.org/mp/20q?search=Pseudapis+neumayeri"));
                            assertThat(resolvedTaxon.getAuthorship(), Is.is("Bossert and Pauly, 2019"));
                            acceptedNameCounter.getAndIncrement();
                        }
                    }
                });

        assertThat(synonymCounter.get(), Is.is(0));
        assertThat(homonymCounter.get(), Is.is(0));
        assertThat(acceptedNameCounter.get(), Is.is(1));
    }

    @Test
    public void matchAll() throws PropertyEnricherException {
        final AtomicInteger counter = new AtomicInteger(0);

        List<Term> termsToBeMatched = Collections.singletonList(
                new TaxonImpl(".*", ".*"));
        getDiscoverLifeTaxonService()
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

        assertThat(counter.get(), Is.is(58495));
    }

    @Test
    public void lookupByNonExistingName() throws PropertyEnricherException {
        AtomicReference<NameType> noMatch = new AtomicReference<>(null);
        getDiscoverLifeTaxonService().match(Collections.singletonList(new TaxonImpl("Donald duck")), new TermMatchListener() {

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
        getDiscoverLifeTaxonService()
                .match(termsToBeMatched, new TermMatchListener() {
                    @Override
                    public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                        assertThat(providedTerm.getName(), Is.is(providedName));
                        assertThat(nameType, Is.is(NameType.HAS_ACCEPTED_NAME));
                        assertThat(resolvedTaxon.getPath(), Is.is("Animalia | Arthropoda | Insecta | Hymenoptera | Andrenidae | Acamptopoeum | Acamptopoeum argentinum"));
                        assertThat(resolvedTaxon.getPathIds(), Is.is("https://www.discoverlife.org/mp/20q?search=Animalia | https://www.discoverlife.org/mp/20q?search=Arthropoda | https://www.discoverlife.org/mp/20q?search=Insecta | https://www.discoverlife.org/mp/20q?search=Hymenoptera | https://www.discoverlife.org/mp/20q?search=Andrenidae | https://www.discoverlife.org/mp/20q?search=Acamptopoeum | https://www.discoverlife.org/mp/20q?search=Acamptopoeum+argentinum"));
                        assertThat(resolvedTaxon.getPathNames(), Is.is("kingdom | phylum | class | order | family | genus | species"));
                        assertThat(resolvedTaxon.getName(), Is.is("Acamptopoeum argentinum"));
                        assertThat(resolvedTaxon.getRank(), Is.is("species"));
                        assertThat(resolvedTaxon.getId(), Is.is("https://www.discoverlife.org/mp/20q?search=Acamptopoeum+argentinum"));
                        counter.getAndIncrement();
                    }
                });

        assertThat(counter.get(), Is.is(1));
    }

    private void assertLookupSynonym() throws PropertyEnricherException {
        final AtomicInteger counter = new AtomicInteger(0);

        String providedName = "Perdita argentina";
        List<Term> termsToBeMatched = Collections.singletonList(new TaxonImpl(providedName));
        getDiscoverLifeTaxonService()
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


    public DiscoverLifeTaxonService getDiscoverLifeTaxonService() {
        return discoverLifeTaxonService;
    }

    public void setDiscoverLifeTaxonService(DiscoverLifeTaxonService discoverLifeTaxonService) {
        this.discoverLifeTaxonService = discoverLifeTaxonService;
    }



}