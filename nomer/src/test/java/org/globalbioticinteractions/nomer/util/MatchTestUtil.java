package org.globalbioticinteractions.nomer.util;

import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.util.ResourceServiceLocal;

import java.util.Map;
import java.util.TreeMap;

import static org.eol.globi.domain.PropertyAndValueDictionary.COMMON_NAMES;
import static org.eol.globi.domain.PropertyAndValueDictionary.EXTERNAL_ID;
import static org.eol.globi.domain.PropertyAndValueDictionary.EXTERNAL_URL;
import static org.eol.globi.domain.PropertyAndValueDictionary.NAME;
import static org.eol.globi.domain.PropertyAndValueDictionary.PATH;
import static org.eol.globi.domain.PropertyAndValueDictionary.PATH_IDS;
import static org.eol.globi.domain.PropertyAndValueDictionary.PATH_NAMES;
import static org.eol.globi.domain.PropertyAndValueDictionary.RANK;
import static org.eol.globi.domain.PropertyAndValueDictionary.THUMBNAIL_URL;

public class MatchTestUtil {

    public static TestTermMatcherContextDefault getLocalTermMatcherCache() {
        return new TestTermMatcherContextDefault() {

            @Override
            public String getProperty(String key) {
                Map<String, String> props = new TreeMap<>();
                props.put("nomer.term.map.url", getClass().getResource("/org/eol/globi/taxon/taxonMap.tsv").toString());
                props.put("nomer.term.cache.url", getClass().getResource("/org/eol/globi/taxon/taxonCache.tsv").toString());
                props.put("nomer.taxon.rank.wikidata.query", getClass().getResource("wikidata-ranks.json").toString());
                return props.get(key);
            }

        };
    }

    public static TaxonCacheService createTaxonCacheService() {
        return new TaxonCacheService(
                "classpath:/org/eol/globi/taxon/taxonCache.tsv",
                "classpath:/org/eol/globi/taxon/taxonMap.tsv",
                new ResourceServiceLocal());
    }

    public static TreeMap<Integer, String> appenderSchemaDefault() {
        return new TreeMap<Integer, String>() {{
            put(0, EXTERNAL_ID);
            put(1, NAME);
            put(2, RANK);
            put(3, COMMON_NAMES);
            put(4, PATH);
            put(5, PATH_IDS);
            put(6, PATH_NAMES);
            put(7, EXTERNAL_URL);
            put(8, THUMBNAIL_URL);
        }};
    }
}
