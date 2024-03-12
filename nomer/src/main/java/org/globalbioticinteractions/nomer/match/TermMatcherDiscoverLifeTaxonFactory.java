package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public class TermMatcherDiscoverLifeTaxonFactory implements TermMatcherFactory {

    @Override
    public String getPreferredName() {
        return "discoverlife-taxon";
    }

    @Override
    public String getDescription() {
        return "Lookup DiscoverLife taxa by name, synonym using offline-enabled database dump";
    }

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new DiscoverLifeTaxonService(ctx);
    }
}
