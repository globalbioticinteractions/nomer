package org.globalbioticinteractions.nomer.cmd;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertThat;

public class CmdDefaultParamsTest {

    @Test
    public void testSchema() {
        List<Pair<Integer, Integer>> pairs =
                CmdDefaultParams.parseSchema("[[1,2,3], [4,5]]");
        assertThat(pairs.size(), Is.is(2));
        assertThat(pairs.get(0), Is.is(new ImmutablePair<>(1,2)));
        assertThat(pairs.get(1), Is.is(new ImmutablePair<>(4,5)));
    }

   @Test
    public void testInvalidSchema() {
        List<Pair<Integer, Integer>> pairs =
                CmdDefaultParams.parseSchema("{[1,2,3], [4,5]]");
        assertThat(pairs.size(), Is.is(1));
        assertThat(pairs.get(0), Is.is(new ImmutablePair<>(1,2)));
    }

}