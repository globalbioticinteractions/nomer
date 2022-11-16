package org.globalbioticinteractions.nomer.match;

import org.apache.commons.io.FileUtils;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.MatchTestUtil;
import org.globalbioticinteractions.nomer.util.TestTermMatcherContextDefault;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class TermMatcherFactoryTaxonRanksTest {

    private TermMatcher termMatcher;

    @Before
    public void init() {
        FileUtils.deleteQuietly(new File(new TestTermMatcherContextDefault().getCacheDir()));
        termMatcher = new TermMatcherFactoryTaxonRanks().createTermMatcher(MatchTestUtil.getLocalTermMatcherCache());
    }

    @Test
    public void ranks() throws PropertyEnricherException {
        List<Term> bla = Collections.singletonList(new TermImpl("", "genus"));
        AtomicBoolean found = new AtomicBoolean(false);
        termMatcher.match(bla, (aLong, s, nameType, taxon) -> {
            assertThat(taxon.getExternalId(), is("WD:Q34740"));
            assertThat(taxon.getName(), is("genus"));
            found.set(true);
        });
        assertTrue(found.get());
    }

    @Test
    public void ranksShort() throws PropertyEnricherException {
        List<Term> bla = Collections.singletonList(new TermImpl("", "sp."));
        AtomicBoolean found = new AtomicBoolean(false);
        termMatcher.match(bla, (aLong, s, nameType, taxon) -> {
            assertThat(taxon.getExternalId(), is("WD:Q7432"));
            assertThat(taxon.getName(), is("species"));
            found.set(true);
        });
        assertTrue(found.get());
    }

    @Test
    public void byId() throws PropertyEnricherException {
        assertFoundById(termMatcher);
    }

    private void assertFoundById(TermMatcher termMatcher) throws PropertyEnricherException {
        List<Term> bla = Collections.singletonList(new TermImpl("WD:Q7432", ""));
        AtomicBoolean found = new AtomicBoolean(false);
        termMatcher.match(bla, (aLong, s, nameType, taxon) -> {
            assertThat(taxon.getExternalId(), is("WD:Q7432"));
            assertThat(taxon.getName(), is("species"));
            found.set(true);
        });
        assertTrue(found.get());
    }

    @Test
    public void twoMatchersSameCacheDir() throws PropertyEnricherException {
        termMatcher = new TermMatcherFactoryTaxonRanks()
                .createTermMatcher(MatchTestUtil.getLocalTermMatcherCache());

        TermMatcher termMatcher2 = new TermMatcherFactoryTaxonRanks()
                .createTermMatcher(MatchTestUtil.getLocalTermMatcherCache());

        assertFoundById(termMatcher);
        assertFoundById(termMatcher2);
    }



}