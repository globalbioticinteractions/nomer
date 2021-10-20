package org.globalbioticinteractions.nomer.util;

import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.match.MatchUtil;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

public class MatchUtilTest {

    @Test
    public void defaultOnEmpty() {
        TermMatcher termMatcher = MatchUtil.getTermMatcher(Collections.emptyList(), new MatchTestUtil.TermMatcherContextDefault());
        assertThat(termMatcher, is(notNullValue()));

    }

}