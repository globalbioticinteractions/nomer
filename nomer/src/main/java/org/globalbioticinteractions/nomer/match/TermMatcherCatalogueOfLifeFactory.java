package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public class TermMatcherCatalogueOfLifeFactory implements TermMatcherFactory {

    @Override
    public String getPreferredName() {
        return "col";
    }

    @Override
    public String getDescription() {
        return "Lookup Catalogue of Life taxon by name or COL:* prefixed ids using offline-enabled database dump";
    }

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new CatalogueOfLifeTaxonService(ctx);
    }
}
