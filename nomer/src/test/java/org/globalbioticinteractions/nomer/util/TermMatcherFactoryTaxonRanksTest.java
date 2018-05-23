package org.globalbioticinteractions.nomer.util;

import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatcher;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TermMatcherFactoryTaxonRanksTest {

    @Test
    public void ranks() throws PropertyEnricherException {

        TermMatcher termMatcher = new TermMatcherFactoryTaxonRanks().createTermMatcher(null);

        List<Term> bla = Collections.singletonList(new TermImpl("", "genus"));
        AtomicBoolean found = new AtomicBoolean(false);
        termMatcher.findTerms(bla, (aLong, s, taxon, nameType) -> {
            assertThat(taxon.getExternalId(), is("WD:Q34740"));
            assertThat(taxon.getName(), is("genus"));
            found.set(true);
        });
        assertTrue(found.get());
    }



}