package org.globalbioticinteractions.nomer.util;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.PropertyEnricherFactory;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.GlobalNamesService;
import org.eol.globi.taxon.GlobalNamesSources;
import org.eol.globi.taxon.RowHandler;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.taxon.TermMatcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;

public class MatchUtil {
    private static final String DEPOT_PREFIX = "https://depot.globalbioticinteractions.org/snapshot/target/data/taxa/";
    private final static String TAXON_MAP_DEFAULT_URL = DEPOT_PREFIX +"taxonMap.tsv.gz";
    private final static String TAXON_CACHE_DEFAULT_URL = DEPOT_PREFIX + "taxonCache.tsv.gz";

    public static void match(String[] args, boolean shouldReplace) {
        TaxonCacheService cacheService = null;
        try {
            String taxonCacheURI = StringUtils.defaultIfBlank(args.length > 0 ? args[0] : "", TAXON_CACHE_DEFAULT_URL);
            String taxonMapURI = StringUtils.defaultIfBlank(args.length > 1 ? args[1] : "", TAXON_MAP_DEFAULT_URL);
            cacheService = new TaxonCacheService(taxonCacheURI, taxonMapURI);
            cacheService.setTemporary(false);

            TermMatcher termMatcher = new GlobalNamesService(Arrays.asList(GlobalNamesSources.values()));
            termMatcher = PropertyEnricherFactory.createTaxonMatcher();
            termMatcher = cacheService;
            resolve(System.in, new TermMatchingRowHandler(shouldReplace, System.out, termMatcher));
        } catch (IOException | PropertyEnricherException e) {
            throw new RuntimeException("failed to resolve taxon", e);
        } finally {
            if (cacheService != null) {
                cacheService.shutdown();
            }
        }
    }

    public static void resolve(InputStream is, RowHandler rowHandler) throws IOException, PropertyEnricherException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        long counter = 0;
        while ((line = reader.readLine()) != null) {
            String[] row = line.split("\t");
            rowHandler.onRow(row);
            counter++;
            if (counter % 25 == 0) {
                System.err.print(".");
            }
            if (counter % (25 * 50) == 0) {
                System.err.println();
            }
        }
    }

    static Taxon resolveTaxon(PropertyEnricher enricher, Taxon taxonProvided) throws PropertyEnricherException {
        Map<String, String> enriched = enricher.enrich(TaxonUtil.taxonToMap(taxonProvided));
        return TaxonUtil.mapToTaxon(enriched);
    }

}
