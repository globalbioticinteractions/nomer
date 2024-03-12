package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public class TermMatcherOpenTreeOfLifeFactory implements TermMatcherFactory {

    @Override
    public String getPreferredName() {
        return "ott";
    }

    @Override
    public String getDescription() {
        return "Lookup Open Tree of Life taxon by name or (OTT|GBIF|WORMS|IF|NCBI|IRMNG)* prefixed ids using offline-enabled database dump";
    }

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new OpenTreeTaxonService(ctx);
    }
}
