package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TermImpl;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;

public class TermMatchUtilTest {

    @Test
    public void createMatchAllRow() {
        Map<Integer, String> schema = new HashMap<>();

        schema.put(1, "bla");
        schema.put(2, "bla");
        schema.put(4, "bla");

        String[] row = TermMatchUtil.wildcardRowForSchema(schema);

        assertThat(row, Is.is(new String[]{".*", ".*", ".*", ".*", ".*"}));


    }


    @Test
    public void shouldMatchAllTerm() {
        Map<Integer, String> schema = new HashMap<>();

        schema.put(1, "bla");
        schema.put(2, "bla");
        schema.put(4, "bla");

        assertTrue(TermMatchUtil.shouldMatchAll(new TermImpl(".*", ".*"), schema));
    }

    @Test
    public void shouldMatchAllTaxon() {
        Map<Integer, String> schema = new HashMap<>();

        schema.put(1, "name");
        schema.put(2, "externalId");
        schema.put(4, "authorship");

        TaxonImpl taxon = new TaxonImpl(".*", ".*");
        taxon.setAuthorship(".*");
        assertTrue(TermMatchUtil.shouldMatchAll(taxon, schema));
    }


}