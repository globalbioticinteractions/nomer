package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public class TermMatcherHesperomysFactory implements TermMatcherFactory {

    @Override
    public String getPreferredName() {
        return "hesperomys";
    }

    @Override
    public String getDescription() {
        return "Lookup Hesperomys taxa by name or id using offline-enabled database dump";
    }

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new HesperomysTaxonService(ctx);
    }
}
