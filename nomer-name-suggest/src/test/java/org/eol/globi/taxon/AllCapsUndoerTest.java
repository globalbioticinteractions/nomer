package org.eol.globi.taxon;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AllCapsUndoerTest {

    @Test
    public void undoAllCaps() {
        assertThat(new AllCapsUndoer().suggest("HOMO SAPIENS"), is("Homo sapiens"));
    }

    @Test
    public void undoAllCapsNoMatch() {
        assertThat(new AllCapsUndoer().suggest("HOMO (Bla) SAPIENS"), is("HOMO (Bla) SAPIENS"));
    }

    @Test
    public void undoAllCapsNoMatch2() {
        assertThat(new AllCapsUndoer().suggest("homo sapiens"), is("homo sapiens"));
    }

}