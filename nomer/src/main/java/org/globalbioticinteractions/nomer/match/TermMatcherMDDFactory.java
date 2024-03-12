package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public class TermMatcherMDDFactory implements TermMatcherFactory {

    @Override
    public String getPreferredName() {
        return "mdd";
    }

    @Override
    public String getDescription() {
        return "Lookup Mammal Diversity Database (MDD) taxon by name or id using offline-enabled database dump";
    }

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new MDDTaxonService(ctx);
    }
}
