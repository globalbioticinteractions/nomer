package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public class TermMatcherBatNamesTaxonFactory implements TermMatcherFactory {

    @Override
    public String getPreferredName() {
        return "batnames";
    }

    @Override
    public String getDescription() {
        return "Lookup BatNames taxa by name, synonym using offline-enabled database dump";
    }

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new BatNamesTaxonService(ctx);
    }
}
