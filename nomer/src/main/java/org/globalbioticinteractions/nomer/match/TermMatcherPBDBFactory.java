package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public class TermMatcherPBDBFactory implements TermMatcherFactory {

    @Override
    public String getPreferredName() {
        return "pbdb";
    }

    @Override
    public String getDescription() {
        return "Lookup Paleobio Database taxon by name or id using offline-enabled database dump";
    }

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new PBDBTaxonService(ctx);
    }
}
