package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public interface TermMatcherFactory {

    TermMatcher createTermMatcher(TermMatcherContext ctx);

    String getName();

    String getDescription();

}
