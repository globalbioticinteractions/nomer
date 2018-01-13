package org.globalbioticinteractions.nomer.util;

import org.eol.globi.taxon.TermMatcher;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class TermMatcherRegistry {

    public final static Map<String, TermMatcherFactory> registry = Collections.unmodifiableMap(new TreeMap<String, TermMatcherFactory>() {
        {
            put("default", new TermMatcherCacheFactory());
            put("globi-correct", new TermMatcherCorrectFactory());
            put("globi-cache", new TermMatcherCacheFactory());
            put("globi-enrich", new TermMatcherFactoryEnricher());
            put("globi-globalnames", new TermMatcherFactoryGlobalNames());
        }
    });

    public static TermMatcher termMatcherFor(String id, TermMatcherContext ctx) {
        TermMatcherFactory factory = registry.get(id);
        return factory == null ? defaultMatcher(ctx) : factory.createTermMatcher(ctx);
    }

    public static TermMatcher defaultMatcher(TermMatcherContext ctx) {
        return new TermMatcherCacheFactory().createTermMatcher(ctx);
    }
}
