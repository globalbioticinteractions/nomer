package org.globalbioticinteractions.nomer.util;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.TaxonImpl;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
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

    @Test
    public void appendWithPath() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        appendTo(new AppenderTSV(), out);
        assertThat(out.toString(), is("col1\tcol2\tSAME_AS\tresolvedId\tresolvedName\tresolvedRank\t\tpath1 | path2\tpathId1 | pathId2\tpathName1 | pathName2\t\t\n"));
    }

    @Test
    public void appendWithSeparateRanks() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        appendTo(new AppenderTSV(), out);
        assertThat(out.toString(), is("col1\tcol2\tSAME_AS\tresolvedId\tresolvedName\tresolvedRank\t\tpath1 | path2\tpathId1 | pathId2\tpathName1 | pathName2\t\t\n"));
    }

    @Test
    public void appendWithSeparateRanksWithSchema() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        appendTo(new AppenderTSV(new HashMap<Integer, String>() {{
            put(0, "path.pathName1.name");
            put(1, "path.pathName1.id");
            put(2, "path.pathName2.name");
            put(3, "path.pathName2.id");
        }}), out);
        assertThat(out.toString(), is("col1\tcol2\tSAME_AS\tpath1\tpathId1\tpath2\tpathId2\n"));
    }

    @Test
    public void appendWithSeparateRanksWithSchemaAndId() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        appendTo(new AppenderTSV(new HashMap<Integer, String>() {{
            put(0, "path.pathName1.name");
            put(1, "path.pathName1.id");
            put(2, "name");
            put(3, "id");
            put(4, "rank");
        }}), out);
        assertThat(out.toString(), is("col1\tcol2\tSAME_AS\tpath1\tpathId1\tresolvedName\tresolvedId\tresolvedRank\n"));
    }

    @Test
    public void appendWithSeparateRanksWithSchemaWithNullPathNames() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String[] row = {"col1", "col2"};
        TaxonImpl provided = new TaxonImpl("providedName", "providedId");
        TaxonImpl resolved = new TaxonImpl("resolvedName", "resolvedId");
        resolved.setPath(null);
        resolved.setPathIds(null);
        resolved.setPathNames(null);
        new AppenderTSV(new HashMap<Integer, String>() {{
            put(0, "path.pathName1.name");
            put(1, "path.pathName1.id");
            put(2, "path.pathName2.name");
            put(3, "path.pathName2.id");
        }}).appendLinesForRow(row, provided, Stream.of(resolved), new PrintStream(out), taxon1 -> NameType.SAME_AS);
        assertThat(out.toString(), is("col1\tcol2\tSAME_AS\t\t\t\t\n"));
    }

    @Test
    public void appendWithSeparateRanksWithSchema2() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        appendTo(new AppenderTSV(new HashMap<Integer, String>() {{
            put(0, "path.pathName4.name");
            put(1, "path.pathName4.id");
            put(2, "path.pathName4");
        }}), out);
        assertThat(out.toString(), is("col1\tcol2\tSAME_AS\t\t\t\n"));
    }

    private void appendTo(Appender appender, ByteArrayOutputStream out) {
        String[] row = {"col1", "col2"};
        TaxonImpl provided = new TaxonImpl("providedName", "providedId");
        TaxonImpl resolved = new TaxonImpl("resolvedName", "resolvedId");
        resolved.setPath("path1 | path2");
        resolved.setPathIds("pathId1 | pathId2");
        resolved.setPathNames("pathName1 | pathName2");
        resolved.setRank("resolvedRank");
        appender.appendLinesForRow(row, provided, Stream.of(resolved), new PrintStream(out), taxon1 -> NameType.SAME_AS);
    }

}