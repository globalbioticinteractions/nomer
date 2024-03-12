package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public class TermMatcherIndexFungorumFactory implements TermMatcherFactory {

    @Override
    public String getPreferredName() {
        return "indexfungorum";
    }

    @Override
    public String getDescription() {
        return "Lookup Index Fungorum taxon by name or id using offline-enabled database dump";
    }

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new IndexFungorumTaxonService(ctx);
    }
}
