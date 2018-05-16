package org.globalbioticinteractions.nomer.util;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.taxon.TaxonCacheService;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MatchTestUtil {

    public static TermMatcherContextDefault getLocalTermMatcherCache() {
        return new TermMatcherContextDefault() {

            @Override
            public String getProperty(String key) {
                Map<String, String> props = new TreeMap<>();
                props.put("nomer.term.map.url", getClass().getResource("/org/eol/globi/taxon/taxonMap.tsv.gz").toString());
                props.put("nomer.term.cache.url", getClass().getResource("/org/eol/globi/taxon/taxonCache.tsv.gz").toString());
                return props.get(key);
            }

        };
    }

    static TaxonCacheService createTaxonCacheService() {
        return new TaxonCacheService("classpath:/org/eol/globi/taxon/taxonCache.tsv", "classpath:/org/eol/globi/taxon/taxonMap.tsv");
    }

    static class TermMatcherContextDefault implements TermMatcherContext {

        @Override
        public String getCacheDir() {
            return "target/cache-dir";
        }

        @Override
        public String getProperty(String key) {
            return null;
        }

        @Override
        public InputStream getResource(String uri) throws IOException {
            return null;
        }

        @Override
        public List<String> getMatchers() {
            return null;
        }

        @Override
        public Map<Integer, String> getInputSchema() {
            return new TreeMap<Integer, String>() {{
                put(0, PropertyAndValueDictionary.EXTERNAL_ID);
                put(1, PropertyAndValueDictionary.NAME);
            }};
        }

    }
}
