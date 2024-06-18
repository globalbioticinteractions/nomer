package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public class TermMatcherWorldRegisterOfMarineSpeciesFactory implements TermMatcherFactory {

    @Override
    public String getPreferredName() {
        return "worms";
    }

    @Override
    public String getDescription() {
        return "Lookup World Register of Marine Species by name or WORMS:* prefixed ids using offline-enabled database dump";
    }

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new WorldRegisterOfMarineSpeciesTaxonService(ctx);
    }
}
