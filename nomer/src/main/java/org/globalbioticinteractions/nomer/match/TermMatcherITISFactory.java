package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public class TermMatcherITISFactory implements TermMatcherFactory {

    @Override
    public String getPreferredName() {
        return "itis-taxon-id";
    }

    @Override
    public String getDescription() {
        return "Lookup ITIS taxon by name or id using offline-enabled database dump";
    }

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new ITISTaxonService(ctx);
    }
}
