package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public class TermMatcherPlaziFactory implements TermMatcherFactory {

    @Override
    public String getPreferredName() {
        return "plazi";
    }

    @Override
    public String getDescription() {
        return "Lookup Plazi taxon treatment by name or id using offline-enabled database dump";
    }

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new PlaziService(ctx);
    }
}
