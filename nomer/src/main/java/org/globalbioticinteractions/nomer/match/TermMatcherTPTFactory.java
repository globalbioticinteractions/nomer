package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public class TermMatcherTPTFactory implements TermMatcherFactory {

    @Override
    public String getPreferredName() {
        return "tpt";
    }

    @Override
    public String getDescription() {
        return "Lookup TPT taxon by name or id using offline-enabled database dump";
    }

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new TPTTaxonService(ctx);
    }
}
