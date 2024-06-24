package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public class TermMatcherWikidataTaxonFactory implements TermMatcherFactory {

    @Override
    public String getPreferredName() {
        return "wikidata";
    }

    @Override
    public String getDescription() {
        return "Lookup Wikidata taxon by name or id using offline-enabled database dump";
    }

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new WikidataTaxonService(ctx);
    }
}
