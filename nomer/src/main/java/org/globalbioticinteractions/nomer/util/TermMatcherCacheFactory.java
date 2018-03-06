package org.globalbioticinteractions.nomer.util;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.taxon.TermMatcher;

import java.io.File;

public class TermMatcherCacheFactory implements TermMatcherFactory {

    private static final String DEPOT_PREFIX = "https://depot.globalbioticinteractions.org/snapshot/target/data/taxa/";
    private final static String TAXON_MAP_DEFAULT_URL = DEPOT_PREFIX +"taxonMap.tsv.gz";
    private final static String TAXON_CACHE_DEFAULT_URL = DEPOT_PREFIX + "taxonCache.tsv.gz";
    private final static int TAXON_MAP_MAX_LINKS = 125;

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        String termMapUrl = ctx.getProperty("nomer.term.map.url");
        String termCacheUrl = ctx.getProperty("nomer.term.cache.url");
        String termMapMaxLinks = ctx.getProperty("nomer.term.map.maxLinksPerTerm");

        TaxonCacheService cacheService = new TaxonCacheService(
                StringUtils.isBlank(termCacheUrl) ? TAXON_CACHE_DEFAULT_URL : termCacheUrl,
                StringUtils.isBlank(termMapUrl) ? TAXON_MAP_DEFAULT_URL : termMapUrl);
        cacheService.setCacheDir(new File(ctx.getCacheDir(), "term-cache"));
        cacheService.setTemporary(false);
        cacheService.setMaxTaxonLinks(StringUtils.isNumeric(termMapMaxLinks)
                ? Integer.parseInt(termMapMaxLinks)
                : TAXON_MAP_MAX_LINKS);
        return cacheService;
    }
}
