package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.taxon.TaxonCacheParser;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.taxon.TaxonMapParser;
import org.eol.globi.taxon.TermMatcher;
import org.eol.globi.taxon.TermResource;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.globalbioticinteractions.nomer.util.TermValidatorPredicates;

import java.io.File;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public class TermMatcherCacheFactory implements TermMatcherFactory {

    private static final String DEPOT_PREFIX = "https://depot.globalbioticinteractions.org/snapshot/target/data/taxa/";
    private final static String TAXON_MAP_DEFAULT_URL = DEPOT_PREFIX + "taxonMap.tsv.gz";
    private final static String TAXON_CACHE_DEFAULT_URL = DEPOT_PREFIX + "taxonCache.tsv.gz";
    private final static int TAXON_MAP_MAX_LINKS = 125;

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        TaxonCacheService cacheService = createCacheService(ctx);
        cacheService.setCacheDir(new File(ctx.getCacheDir(), "term_cache"));
        cacheService.setMaxTaxonLinks(getMaxTermLinks(ctx));
        return cacheService;
    }

    @Override
    public String getPreferredName() {
        return "globi-taxon-cache";
    }

    @Override
    public String getDescription() {
        return "Uses GloBI's Taxon Graph to lookup terms by id or name across many taxonomies / ontologies. Caches a copy locally on first use to allow for subsequent offline usage. Use properties [nomer.term.cache.url] and [nomer.term.map.url] to override default cache and map locations. See https://doi.org/10.5281/zenodo.755513 for more information.";
    }

    public static TaxonCacheService createCacheService(TermMatcherContext ctx) {
        TermResource<Taxon> terms = new TermResource<Taxon>() {

            @Override
            public String getResource() {
                return getTermCacheUrl(ctx);
            }

            @Override
            public Function<String, Taxon> getParser() {
                return TaxonCacheParser::parseLine;
            }

            @Override
            public Predicate<String> getValidator() {
                return TermValidatorPredicates.PATH_EXISTS;
            }
        };

        TermResource<Triple<Taxon, NameType, Taxon>> links = new TermResource<Triple<Taxon, NameType, Taxon>>() {

            @Override
            public String getResource() {
                return getTermMapUrl(ctx);
            }

            @Override
            public Function<String, Triple<Taxon, NameType, Taxon>> getParser() {
                return TaxonMapParser::parse;
            }

            @Override
            public Predicate<String> getValidator() {
                return Objects::nonNull;
            }
        };
        return new TaxonCacheService(terms, links);
    }

    public static String getTermMapUrl(TermMatcherContext ctx) {
        String taxonMapUrl = ctx.getProperty("nomer.term.map.url");
        return StringUtils.isBlank(taxonMapUrl) ? TAXON_MAP_DEFAULT_URL : taxonMapUrl;
    }

    public static String getTermCacheUrl(TermMatcherContext ctx) {
        String taxonCacheUrl = ctx.getProperty("nomer.term.cache.url");
        return StringUtils.isBlank(taxonCacheUrl) ? TAXON_CACHE_DEFAULT_URL : taxonCacheUrl;
    }

    protected int getMaxTermLinks(TermMatcherContext ctx) {
        String maxTermLinks = ctx.getProperty("nomer.term.map.maxLinksPerTerm");
        return StringUtils.isNumeric(maxTermLinks)
                ? Integer.parseInt(maxTermLinks)
                : TAXON_MAP_MAX_LINKS;
    }

}
