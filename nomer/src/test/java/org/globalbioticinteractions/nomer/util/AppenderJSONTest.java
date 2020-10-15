package org.globalbioticinteractions.nomer.util;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.TaxonImpl;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.stream.Stream;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AppenderJSONTest {

    @Test
    public void append() {
        String[] row = {"col1", "col2"};
        TaxonImpl provided = new TaxonImpl("providedName", "providedId");
        TaxonImpl resolved = new TaxonImpl("resolvedName", "resolvedId");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new AppenderJSON().appendLinesForRow(row, provided, Stream.of(resolved), new PrintStream(out), taxon1 -> NameType.SAME_AS);
        assertThat(out.toString(), is("{\"norank\":{\"@id\":\"resolvedId\",\"name\":\"resolvedName\",\"equivalent_to\":{\"@id\":\"providedId\",\"name\":\"providedName\"}}}\n"));
    }

}