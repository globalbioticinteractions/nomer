package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public class TermMatcherGBIFTaxonFactory implements TermMatcherFactory {

    @Override
    public String getName() {
        return "gbif-taxon";
    }

    @Override
    public String getDescription() {
        return "Lookup GBIF taxa by name, synonym or id using offline-enabled database dump";
    }

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new GBIFTaxonService(ctx);
    }
}
