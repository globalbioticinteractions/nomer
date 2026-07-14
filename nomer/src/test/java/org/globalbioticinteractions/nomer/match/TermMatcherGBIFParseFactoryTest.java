package org.globalbioticinteractions.nomer.match;

import junit.framework.TestCase;
import org.apache.commons.collections4.CollectionUtils;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.cmd.OutputFormat;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class TermMatcherGBIFParseFactoryTest {

    @Test
    public void aggregate() throws PropertyEnricherException {
        AtomicBoolean found = new AtomicBoolean(false);
        TermMatcherGBIFParseFactory termMatcherGBIFParseFactory = new TermMatcherGBIFParseFactory();
        TermMatcher termMatcher = termMatcherGBIFParseFactory.createTermMatcher(new TermMatcherContext() {
            @Override
            public String getCacheDir() {
                return "";
            }

            @Override
            public List<String> getMatchers() {
                return Collections.emptyList();
            }

            @Override
            public Map<Integer, String> getInputSchema() {
                return Collections.emptyMap();
            }

            @Override
            public Map<Integer, String> getOutputSchema() {
                return Collections.emptyMap();
            }

            @Override
            public OutputFormat getOutputFormat() {
                return null;
            }

            @Override
            public InputStream retrieve(URI uri) throws IOException {
                return null;
            }

            @Override
            public String getProperty(String key) {
                return "";
            }
        });
        termMatcher.match(Arrays.asList(new TermImpl(null, "Rubus fruticosus agg.")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                assertThat(taxon.getName(), is("Rubus fruticosus"));
                found.set(true);
            }
        });

        assertTrue(found.get());
    }


}