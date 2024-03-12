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
        new AppenderJSON().appendLinesForRow(row, provided, taxon1 -> NameType.SAME_AS, Stream.of(resolved), new PrintStream(out));
        assertThat(out.toString(), is("{\"norank\":{\"@id\":\"resolvedId\",\"name\":\"resolvedName\",\"equivalent_to\":{\"@id\":\"providedId\",\"name\":\"providedName\"}}}\n"));
    }

    @Test
    public void appendNullRank() {
        String[] row = {"col1", "col2"};
        TaxonImpl provided = new TaxonImpl("providedName", "providedId");
        TaxonImpl resolved = new TaxonImpl("resolvedName", "resolvedId");
        resolved.setPathNames(null);
        resolved.setPathIds("id1 | id2");
        resolved.setPath("name1 | name2");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new AppenderJSON()
                .appendLinesForRow(
                        row,
                        provided,
                        taxon1 -> NameType.SAME_AS, Stream.of(resolved),
                        new PrintStream(out)
                );

        assertThat(out.toString(),
                is("{\"norank\":{\"@id\":\"resolvedId\",\"name\":\"resolvedName\"" +
                        ",\"equivalent_to\":{\"@id\":\"providedId\",\"name\":\"providedName\"}},\"path\":{\"names\":[\"name1\",\"name2\"],\"ids\":[\"id1\",\"id2\"]}}\n"));
    }

    @Test
    public void appendNoRankRanks() {
        String[] row = {"col1", "col2"};
        TaxonImpl provided = new TaxonImpl("providedName", "providedId");
        TaxonImpl resolved = new TaxonImpl("resolvedName", "resolvedId");
        resolved.setPathNames("norank | norank");
        resolved.setPathIds("id1 | id2");
        resolved.setPath("name1 | name2");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new AppenderJSON()
                .appendLinesForRow(
                        row,
                        provided,
                        taxon1 -> NameType.SAME_AS, Stream.of(resolved),
                        new PrintStream(out)
                );

        assertThat(out.toString(),
                is("{\"norank\":{\"@id\":\"resolvedId\",\"name\":\"resolvedName\"" +
                        ",\"equivalent_to\":{\"@id\":\"providedId\",\"name\":\"providedName\"}},\"path\":{\"names\":[\"name1\",\"name2\"],\"ids\":[\"id1\",\"id2\"],\"ranks\":[\"norank\",\"norank\"]}}\n"));
    }

}