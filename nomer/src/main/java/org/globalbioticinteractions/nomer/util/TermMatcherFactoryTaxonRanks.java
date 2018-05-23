package org.globalbioticinteractions.nomer.util;

import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.taxon.TermMatcher;

import java.net.URL;

public class TermMatcherFactoryTaxonRanks implements TermMatcherFactory {

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        URL terms = getClass().getResource("taxon_ranks.tsv");
        URL links = getClass().getResource("taxon_rank_links.tsv");
        return new TaxonCacheService(terms.toExternalForm(), links.toExternalForm());
    }

}