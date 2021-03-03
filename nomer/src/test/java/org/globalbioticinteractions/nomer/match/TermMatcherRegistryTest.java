package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.MatchTestUtil;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class TermMatcherRegistryTest {

    @Test
    public void createDefaultTermMatcher() {
        TermMatcher matcher = TermMatcherRegistry.termMatcherFor("this doesn't exist", new MatchTestUtil.TermMatcherContextDefault());
        assertThat(matcher.getClass().getName(), is(TaxonCacheService.class.getName()));
    }

}