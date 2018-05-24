package org.globalbioticinteractions.nomer.util;

import org.apache.commons.io.FileUtils;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TermMatcherFactoryTaxonRanksTest {

    private TermMatcher termMatcher;

    @Before
    public void init() {
        FileUtils.deleteQuietly(new File(new MatchTestUtil.TermMatcherContextDefault().getCacheDir()));
        termMatcher = new TermMatcherFactoryTaxonRanks().createTermMatcher(MatchTestUtil.getLocalTermMatcherCache());
    }

    @Test
    public void ranks() throws PropertyEnricherException {
        List<Term> bla = Collections.singletonList(new TermImpl("", "genus"));
        AtomicBoolean found = new AtomicBoolean(false);
        termMatcher.findTerms(bla, (aLong, s, taxon, nameType) -> {
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
        termMatcher.findTerms(bla, (aLong, s, taxon, nameType) -> {
            assertThat(taxon.getExternalId(), is("WD:Q7432"));
            assertThat(taxon.getName(), is("species"));
            found.set(true);
        });
        assertTrue(found.get());
    }



}