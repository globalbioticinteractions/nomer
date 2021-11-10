package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.hamcrest.core.Is;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.MatcherAssert.assertThat;

@Ignore(value = "https://github.com/globalbioticinteractions/nomer/issues/28")
public class TermMatcherOpenBiodivFactoryTest {

    @Test
    public void matchTaxonConcept() throws PropertyEnricherException {
        String termId = TaxonomyProvider.OPEN_BIODIV.getIdPrefix() + "4B689A17-2541-4F5F-A896-6F0C2EEA3FB4";
        assertMatchingTermId(new TermImpl(termId, ""));
    }

    @Test
    public void matchTaxonConceptWithFullURI() throws PropertyEnricherException {
        assertMatchingTermId(new TermImpl("http://openbiodiv.net/4B689A17-2541-4F5F-A896-6F0C2EEA3FB4", ""));
    }

    @Test
    public void noMatchTaxonConceptWithNameOnly() throws PropertyEnricherException {
        Term term = new TermImpl(null, "Acanthaceae");
        assertNoMatch(term);
    }

    @Test
    public void noMatchTaxonConceptInvalidURI() throws PropertyEnricherException {
        Term term = new TermImpl("http://openbiodiv.net/2342", "Acanthaceae");
        assertNoMatch(term);
    }

    private void assertNoMatch(Term term) throws PropertyEnricherException {
        final TermMatcher termMatcher =
                new TermMatcherOpenBiodivFactory().createTermMatcher(testContext());
        final AtomicBoolean anyMatches = new AtomicBoolean(false);
        termMatcher.match(Collections.singletonList(
                term), (requestId, providedTerm, resolvedTaxon, nameType) -> {
                    anyMatches.set(anyMatches.get() || !NameType.NONE.equals(nameType));
                });
        assertThat(anyMatches.get(), Is.is(false));
    }

    private void assertMatchingTermId(Term term) throws PropertyEnricherException {
        final ArrayList<Taxon> resolveTaxa = new ArrayList<>();
        final AtomicBoolean matchesOnly = new AtomicBoolean(true);
        final TermMatcher termMatcher =
                new TermMatcherOpenBiodivFactory().createTermMatcher(testContext());
        termMatcher.match(Collections.singletonList(
                term), (requestId, providedTerm, nameType, resolvedTaxon) -> {
                    resolveTaxa.add(resolvedTaxon);
                    matchesOnly.set(NameType.SAME_AS.equals(nameType) && matchesOnly.get());

                });
        assertThat(resolveTaxa.size(), Is.is(1));

        assertThat(matchesOnly.get(), Is.is(true));
        assertThat(resolveTaxa.get(0).getPath(), Is.is("Plantae | Tracheophyta | Magnoliopsida | Lamiales | Acanthaceae"));
    }


    private TermMatcherContext testContext() {
        return new TermMatcherContext() {
            @Override
            public String getCacheDir() {
                return null;
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