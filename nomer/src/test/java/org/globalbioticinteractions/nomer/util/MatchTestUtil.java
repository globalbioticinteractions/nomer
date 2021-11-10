package org.globalbioticinteractions.nomer.util;

import org.eol.globi.taxon.TaxonCacheService;

import java.util.Map;
import java.util.TreeMap;

public class MatchTestUtil {

    public static TestTermMatcherContextDefault getLocalTermMatcherCache() {
        return new TestTermMatcherContextDefault() {

            @Override
            public String getProperty(String key) {
                Map<String, String> props = new TreeMap<>();
                props.put("nomer.term.map.url", getClass().getResource("/org/eol/globi/taxon/taxonMap.tsv.gz").toString());
                props.put("nomer.term.cache.url", getClass().getResource("/org/eol/globi/taxon/taxonCache.tsv.gz").toString());
                return props.get(key);
            }

        };
    }

    public static TaxonCacheService createTaxonCacheService() {
        return new TaxonCacheService("classpath:/org/eol/globi/taxon/taxonCache.tsv", "classpath:/org/eol/globi/taxon/taxonMap.tsv");
    }

}
