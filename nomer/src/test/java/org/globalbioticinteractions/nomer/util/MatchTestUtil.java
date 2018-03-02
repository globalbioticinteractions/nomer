package org.globalbioticinteractions.nomer.util;

import org.apache.commons.collections4.MapUtils;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.util.DateUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertThat;

public class MatchTestUtil {

    static class PropertyEnricherPassThrough implements PropertyEnricher {

        @Override
        public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
            return MapUtils.unmodifiableMap(new TreeMap<String, String>(properties) {{
                put(PropertyAndValueDictionary.NAME_SOURCE, "A name source");
                put(PropertyAndValueDictionary.NAME_SOURCE_URL, "http://example.org");
                put(PropertyAndValueDictionary.NAME_SOURCE_ACCESSED_AT, DateUtil.printDate(new Date(0)));
            }});
        }

        @Override
        public void shutdown() {

        }
    }

    static class PropertyEnricherMatch implements PropertyEnricher {

        @Override
        public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
            return MapUtils.unmodifiableMap(new TreeMap<String, String>(properties) {{
                put(PropertyAndValueDictionary.NAME_SOURCE, "A name source");
                put(PropertyAndValueDictionary.NAME_SOURCE_URL, "http://example.org");
                put(PropertyAndValueDictionary.NAME_SOURCE_ACCESSED_AT, DateUtil.printDate(new Date(0)));
                put(PropertyAndValueDictionary.PATH, "one | two");
            }});
        }

        @Override
        public void shutdown() {

        }
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
