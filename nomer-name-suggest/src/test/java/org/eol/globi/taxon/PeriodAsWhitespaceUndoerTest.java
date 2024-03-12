package org.eol.globi.taxon;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PeriodAsWhitespaceUndoerTest {

    @Test
    public void undo() {
        String bla_bla = new PeriodAsWhitespaceUndoer().suggest("bla.bla");
        assertThat(bla_bla, is("Bla bla"));
    }

    @Test
    public void undoSubspecies() {
        String bla_bla = new PeriodAsWhitespaceUndoer().suggest("bla.bla.sub");
        assertThat(bla_bla, is("Bla bla sub"));
    }

    @Test
    public void doNotUndo() {
        String bla_bla = new PeriodAsWhitespaceUndoer().suggest("bla. bla");
        assertThat(bla_bla, is("bla. bla"));
    }



}