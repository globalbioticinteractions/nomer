package org.globalbioticinteractions.nomer.util;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class TermMatcherCorrectFactoryTest {

    @Test
    public void init() {
        assertNotNull(new TermMatcherCorrectFactory().createTermMatcher(null));
    }

}