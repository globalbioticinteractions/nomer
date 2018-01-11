package org.globalbioticinteractions.nomer.util;

import org.eol.globi.taxon.GlobalNamesService;
import org.eol.globi.taxon.GlobalNamesSources;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.taxon.TermMatcher;

import java.util.Arrays;

public class TermMatcherFactoryGlobalNames implements TermMatcherFactory {

    private static final String DEPOT_PREFIX = "https://depot.globalbioticinteractions.org/snapshot/target/data/taxa/";
    private final static String TAXON_MAP_DEFAULT_URL = DEPOT_PREFIX +"taxonMap.tsv.gz";
    private final static String TAXON_CACHE_DEFAULT_URL = DEPOT_PREFIX + "taxonCache.tsv.gz";

    @Override
    public TermMatcher createTermMatcher() {
        return  new GlobalNamesService(Arrays.asList(GlobalNamesSources.values()));
    }
}
