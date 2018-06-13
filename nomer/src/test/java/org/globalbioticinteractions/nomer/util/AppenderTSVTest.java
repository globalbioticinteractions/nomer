package org.globalbioticinteractions.nomer.util;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.TaxonImpl;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.stream.Stream;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class AppenderTSVTest {

    @Test
    public void append() {
        String[] row = {"col1", "col2"};
        TaxonImpl provided = new TaxonImpl("providedName", "providedId");
        TaxonImpl resolved = new TaxonImpl("resolvedName", "resolvedId");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new AppenderTSV().appendLinesForRow(row, provided, Stream.of(resolved), new PrintStream(out), taxon1 -> NameType.SAME_AS);
        assertThat(out.toString(), is("col1\tcol2\tSAME_AS\tresolvedId\tresolvedName\t\t\t\t\t\t\t\n"));
    }

}