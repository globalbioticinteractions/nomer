package org.globalbioticinteractions.nomer.util;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TermMatcherCorrectFactoryTest {

    @Test
    public void init() {
        assertNotNull(new TermMatcherCorrectFactory().createTermMatcher(null));
    }

    @Test
    public void correctSingleTerm() throws PropertyEnricherException {
        TermMatcher termMatcher = new TermMatcherCorrectFactory().createTermMatcher(null);
        AtomicBoolean found = new AtomicBoolean(false);
        termMatcher.findTermsForNames(Arrays.asList("copepods"), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long nodeId, String name, Taxon taxon, NameType nameType) {
                assertThat(taxon.getName(), Is.is("Copepoda"));
                found.set(true);
            }
        });
        assertTrue(found.get());
    }

    @Test
    public void correctSingleTermWithStopword() throws PropertyEnricherException {
        TermMatcher termMatcher = new TermMatcherCorrectFactory().createTermMatcher(null);
        AtomicBoolean found = new AtomicBoolean(false);
        termMatcher.findTermsForNames(Arrays.asList("unidentified copepod"), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long nodeId, String name, Taxon taxon, NameType nameType) {
                assertThat(taxon.getName(), Is.is("Copepoda"));
                found.set(true);
            }
        });
        assertTrue(found.get());
    }

}