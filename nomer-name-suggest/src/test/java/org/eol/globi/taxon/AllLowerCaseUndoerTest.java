package org.eol.globi.taxon;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class AllLowerCaseUndoerTest {

    @Test
    public void undoLowerCase() {
        String corrected = new AllLowerCaseUndoer().suggest("homo sapiens");
        assertThat(corrected, is("Homo sapiens"));
    }

    @Test
    public void undoLowerCase2() {
        String corrected = new AllLowerCaseUndoer().suggest("io");
        assertThat(corrected, is("Io"));
    }

    @Test
    public void doNothing() {
        String corrected = new AllLowerCaseUndoer().suggest("Homo Sapiens");
        assertThat(corrected, is("Homo Sapiens"));
    }

}