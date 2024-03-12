package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.cmd.OutputFormat;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.MatcherAssert.assertThat;

public class TermMatcherWikidataFactoryTest {

    @Test
    public void matchByNCBITaxonId() throws PropertyEnricherException {
        final ArrayList<Taxon> resolveTaxa = new ArrayList<>();
        final AtomicBoolean matchesOnly = new AtomicBoolean(true);
        final TermMatcher termMatcher =
                new TermMatcherWikidataFactory().createTermMatcher(testContext());
        termMatcher.match(Collections.singletonList(new TermImpl("NCBI:9606", "")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon ) {
                resolveTaxa.add(resolvedTaxon);
                matchesOnly.set(NameType.SAME_AS.equals(nameType) && matchesOnly.get());

            }
        });

        assertThat(resolveTaxa.size() > 0, Is.is(true));
        assertThat(matchesOnly.get(), Is.is(true));
    }

    @Test
    public void matchByWikidataId() throws PropertyEnricherException {
        final ArrayList<Taxon> resolveTaxa = new ArrayList<>();
        final AtomicBoolean matchesOnly = new AtomicBoolean(true);
        final TermMatcher termMatcher =
                new TermMatcherWikidataFactory().createTermMatcher(testContext());
        termMatcher.match(Collections.singletonList(new TermImpl("WD:Q140", "")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon ) {
                resolveTaxa.add(resolvedTaxon);
                matchesOnly.set(NameType.SAME_AS.equals(nameType) && matchesOnly.get());

            }
        });

        assertThat(resolveTaxa.size() > 0, Is.is(true));
        assertThat(matchesOnly.get(), Is.is(true));
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
            public OutputFormat getOutputFormat() {
                return null;
            }

            @Override
            public String getProperty(String key) {
                return null;
            }
        };
    }

}