package org.globalbioticinteractions.nomer.match;

import org.globalbioticinteractions.nomer.util.MatchTestUtil;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class TermMatcherRegistryTest {

    @Test(expected = IllegalArgumentException.class)
    public void createNonExistingMatcher() {
        try {
            TermMatcherRegistry.termMatcherFor("this doesn't exist", new MatchTestUtil.TermMatcherContextDefault());
        } catch (Throwable ex) {
            assertThat(ex.getMessage(), is("unknown matcher [this doesn't exist]"));
            throw ex;
        }
    }

}