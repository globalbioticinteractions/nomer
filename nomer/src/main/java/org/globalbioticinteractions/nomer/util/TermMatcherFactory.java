package org.globalbioticinteractions.nomer.util;

import org.eol.globi.taxon.TermMatcher;

public interface TermMatcherFactory {

    TermMatcher createTermMatcher(TermMatcherContext ctx);

}
