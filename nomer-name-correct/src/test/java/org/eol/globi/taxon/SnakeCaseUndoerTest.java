package org.eol.globi.taxon;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class SnakeCaseUndoerTest {

    @Test
    public void undo() {
        String bla_bla = new SnakeCaseUndoer().suggest("bla_bla");
        assertThat(bla_bla, is("Bla bla"));
    }

    @Test
    public void undo2() {
        String bla_bla = new SnakeCaseUndoer().suggest("bla_bla_bla");
        assertThat(bla_bla, is("Bla bla bla"));
    }

    @Test
    public void undo3() {
        String bla_bla = new SnakeCaseUndoer().suggest("bla");
        assertThat(bla_bla, is("Bla"));
    }

    @Test
    public void notUndo3() {
        String bla_bla = new SnakeCaseUndoer().suggest("Homo sapiens");
        assertThat(bla_bla, is("Homo sapiens"));
    }

}