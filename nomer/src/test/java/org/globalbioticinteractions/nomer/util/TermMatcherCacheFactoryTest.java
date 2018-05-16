package org.globalbioticinteractions.nomer.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TermMatcherCacheFactoryTest {

    @Before
    public void clean() {
        FileUtils.deleteQuietly(new File(new MatchTestUtil.TermMatcherContextDefault().getCacheDir()));
    }

    @Test
    public void smallCache() throws PropertyEnricherException {
        TermMatcher termMatcher = new TermMatcherCacheFactory().createTermMatcher(MatchTestUtil.getLocalTermMatcherCache());

        AtomicBoolean hasMatch = new AtomicBoolean(false);
        termMatcher.findTerms(Collections.singletonList(new TermImpl("EOL:1276240", "bla")), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long id, String name, Taxon taxon, NameType nameType) {
                assertThat(taxon.getName(), is(not("bla")));

                hasMatch.set(true);
            }
        });

        assertThat(hasMatch.get(), is(true));
    }

    @Test
    public void smallCacheLimitHits() throws PropertyEnricherException {
        assertNumberOfHits("1", 1);
    }

    @Test
    public void smallCacheLimitTwoHits() throws PropertyEnricherException {
        assertNumberOfHits("3", 2);
    }

    @Test
    public void smallCacheLimitDefaultHits() throws PropertyEnricherException {
        assertNumberOfHits(null, 2);
    }

    private void assertNumberOfHits(String maxLinksPerTerm, int expectedNumberOfLinks) throws PropertyEnricherException {
        TermMatcher termMatcher = new TermMatcherCacheFactory().createTermMatcher(new MatchTestUtil.TermMatcherContextDefault() {

            @Override
            public String getProperty(String key) {
                Map<String, String> props = new TreeMap<>();
                props.put("nomer.term.map.url", getClass().getResource("/org/eol/globi/taxon/taxonMap.tsv").toString());
                if (StringUtils.isNumeric(maxLinksPerTerm)) {
                    props.put("nomer.term.map.maxLinksPerTerm", maxLinksPerTerm);
                }
                props.put("nomer.term.cache.url", getClass().getResource("/org/eol/globi/taxon/taxonCache.tsv").toString());
                return props.get(key);
            }

        });

        AtomicInteger numberOfResults = new AtomicInteger(0);
        termMatcher.findTerms(Collections.singletonList(new TermImpl("", "Homo sapiens")), (id, name, taxon, nameType) -> {
            assertThat(taxon.getName(), is("Homo sapiens"));
            numberOfResults.incrementAndGet();
            assertThat(Arrays.asList("EOL:327955", "NCBI:9606").contains(taxon.getId()), is(true));
        });

        assertThat(numberOfResults.get(), is(expectedNumberOfLinks));
    }

}