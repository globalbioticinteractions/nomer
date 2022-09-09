package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public class TermMatcherWorldOfFloraOnlineFactory implements TermMatcherFactory {

    @Override
    public String getPreferredName() {
        return "wfo";
    }

    @Override
    public String getDescription() {
        return "Lookup World of Flora Online taxon by name or WFO:* prefixed ids using offline-enabled database dump";
    }

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new WorldOfFloraOnlineTaxonService(ctx);
    }
}
