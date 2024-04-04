package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatchListener;
import org.globalbioticinteractions.nomer.cmd.OutputFormat;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DiscoverLifeTaxonService2Test extends DiscoverLifeTaxonServiceTest {

    @Before
    public void init() throws PropertyEnricherException {
        DiscoverLifeTaxonService discoverLifeTaxonService = new DiscoverLifeTaxonService(getTmpContext());
        discoverLifeTaxonService.match(Arrays.asList(new TermImpl(null, "Apis mellifera"))
                , new TermMatchListener() {
                    @Override
                    public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {

                    }
                });
        setDiscoverLifeTaxonService(discoverLifeTaxonService);
    }

    @After
    public void close() {
        getDiscoverLifeTaxonService().close();
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
                return DiscoverLifeTaxonServiceTestBase.class.getResourceAsStream("discoverlife-bees.xml");
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
                return "https://example.org/bees";
            }
        };
    }


}