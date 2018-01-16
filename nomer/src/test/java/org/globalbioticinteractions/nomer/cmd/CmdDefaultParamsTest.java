package org.globalbioticinteractions.nomer.cmd;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.core.Is;
import org.junit.Test;

import static org.junit.Assert.assertThat;

public class CmdDefaultParamsTest {

    @Test
    public void testSchema() {
        Pair<Integer, Integer> pair =
                CmdDefaultParams.parseSchema("[2,3]");
        assertThat(pair, Is.is(new ImmutablePair<>(2,3)));
    }

   @Test
    public void testInvalidSchema() {
        Pair<Integer, Integer> pair =
                CmdDefaultParams.parseSchema("{[1,2,3], [4,5]]");
        assertThat(pair, Is.is(new ImmutablePair<>(1,2)));
    }

}