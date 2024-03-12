package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatchListener;
import org.globalbioticinteractions.nomer.cmd.OutputFormat;
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

import static junit.framework.TestCase.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;

public class BatNamesTaxonServiceIT {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private BatNamesTaxonService batNamesTaxonService;

    @Before
    public void init() throws PropertyEnricherException {
        batNamesTaxonService = new BatNamesTaxonService(getTmpContext());
        batNamesTaxonService.match(Arrays.asList(new TermImpl(null, "Rhinolophus sinicus"))
                , new TermMatchListener() {
                    @Override
                    public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {

                    }
                });
    }

    @After
    public void close() {
        batNamesTaxonService.close();
    }

    @Test
    public void lookupByName() throws PropertyEnricherException {
        assertLookup(batNamesTaxonService);
        assertLookup(batNamesTaxonService);
    }

    private void assertLookup(BatNamesTaxonService batNamesTaxonService) throws PropertyEnricherException {
        final AtomicInteger counter = new AtomicInteger(0);

        String providedName = "Rhinolophus sinicus";
        List<Term> termsToBeMatched = Collections.singletonList(new TaxonImpl(providedName));
        batNamesTaxonService
                .match(termsToBeMatched, new TermMatchListener() {
                    @Override
                    public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                        assertThat(providedTerm.getName(), Is.is(providedName));
                        assertThat(nameType, Is.is(NameType.HAS_ACCEPTED_NAME));
                        assertThat(resolvedTaxon.getName(), Is.is("Rhinolophus sinicus"));
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