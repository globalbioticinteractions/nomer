package org.globalbioticinteractions.nomer.util;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TermMatcherCacheFactoryTest {

    @Test
    public void smallCache() throws PropertyEnricherException {
        TermMatcher termMatcher = new TermMatcherCacheFactory().createTermMatcher(new MatchTestUtil.TermMatcherContextDefault() {

            @Override
            public String getProperty(String key) {
                Map<String, String> props = new TreeMap<>();
                props.put("nomer.term.map.url", getClass().getResource("/org/eol/globi/taxon/taxonMap.tsv.gz").toString());
                props.put("nomer.term.cache.url", getClass().getResource("/org/eol/globi/taxon/taxonCache.tsv.gz").toString());
                return props.get(key);
            }

        });

        AtomicBoolean hasMatch = new AtomicBoolean(false);
        termMatcher.findTerms(Arrays.asList(new TermImpl("EOL:1276240", "bla")), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long id, String name, Taxon taxon, NameType nameType) {
                assertThat(taxon.getName(), Is.is(not("bla")));
                hasMatch.set(true);
            }
        });

        assertThat(hasMatch.get(), Is.is(true));
    }

}