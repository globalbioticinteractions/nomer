package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.MatchTestUtil;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TermMatcherRegistryTest {

    @Test
    public void createTermMatcher() throws PropertyEnricherException {
        TermMatcher itisService = TermMatcherRegistry.termMatcherFor("itis-taxon-id-web", null);
        AtomicBoolean found = new AtomicBoolean(false);
        itisService.match(Collections.singletonList(new TermImpl("ITIS:180547", null)), new TermMatchListener() {

            @Override
            public void foundTaxonForTerm(Long nodeId, Term name, Taxon taxon, NameType nameType) {
                assertThat(taxon.getName(), is("Enhydra lutris"));
                found.set(true);
            }
        });
        assertThat(found.get(), is(true));
    }

    @Test
    public void createDefaultTermMatcher() {
        TermMatcher matcher = TermMatcherRegistry.termMatcherFor("this doesn't exist", new MatchTestUtil.TermMatcherContextDefault());
        assertThat(matcher.getClass().getName(), is(TaxonCacheService.class.getName()));
    }

}