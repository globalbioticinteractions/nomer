package org.globalbioticinteractions.nomer.match;

import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatcher;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.Collections;
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
        termMatcher.findTermsForNames(Collections.singletonList("copepods"), (nodeId, name, taxon, nameType) -> {
            assertThat(taxon.getName(), Is.is("Copepoda"));
            found.set(true);
        });
        assertTrue(found.get());
    }

    @Test
    public void correctSingleTermWithStopword() throws PropertyEnricherException {
        TermMatcher termMatcher = new TermMatcherCorrectFactory().createTermMatcher(null);
        AtomicBoolean found = new AtomicBoolean(false);
        termMatcher.findTermsForNames(Collections.singletonList("unidentified copepod"), (nodeId, name, taxon, nameType) -> {
            assertThat(taxon.getName(), Is.is("Copepoda"));
            found.set(true);
        });
        assertTrue(found.get());
    }

    @Test
    public void correctSingleTermWithStopwordAndDash() throws PropertyEnricherException {
        TermMatcher termMatcher = new TermMatcherCorrectFactory().createTermMatcher(null);
        AtomicBoolean found = new AtomicBoolean(false);
        termMatcher.findTermsForNames(Collections.singletonList("unidentified amphipod 1.0-1.9 mm"), (nodeId, name, taxon, nameType) -> {
            assertThat(taxon.getName(), Is.is("Amphipoda"));
            found.set(true);
        });
        assertTrue(found.get());
    }
    @Test
    public void correctSingleTermWithStopwordAndDash2() throws PropertyEnricherException {
        TermMatcher termMatcher = new TermMatcherCorrectFactory().createTermMatcher(null);
        AtomicBoolean found = new AtomicBoolean(false);
        termMatcher.findTermsForNames(Collections.singletonList("unidentified ostracod 2.0-2.9 mm"), (nodeId, name, taxon, nameType) -> {
            assertThat(taxon.getName(), Is.is("Ostracoda"));
            found.set(true);
        });
        assertTrue(found.get());
    }

}