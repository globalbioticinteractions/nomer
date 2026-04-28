package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public class TermMatcherIIRMNGFactory implements TermMatcherFactory {

    @Override
    public String getPreferredName() {
        return "irmng";
    }

    @Override
    public String getDescription() {
        return "Lookup Interim Register for Marine and Nonmagine Genera (IRMNG) taxon by name or id using offline-enabled database dump";
    }

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new IRMNGTaxonService(ctx);
    }
}
