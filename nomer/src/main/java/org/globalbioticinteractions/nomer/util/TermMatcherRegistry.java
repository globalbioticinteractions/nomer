package org.globalbioticinteractions.nomer.util;

import org.apache.commons.collections4.list.TreeList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.taxon.TermMatcher;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TermMatcherRegistry {
    private static final Log LOG = LogFactory.getLog(TermMatcherRegistry.class);

    public static final TermMatcherCacheFactory MATCHER_FACTORY_DEFAULT = new TermMatcherCacheFactory();
    private final static List<TermMatcherFactory> matchers = Collections.unmodifiableList(new TreeList<TermMatcherFactory>(){{
        add(MATCHER_FACTORY_DEFAULT);
        add(new TermMatcherCorrectFactory());
        add(new TermMatcherFactoryTaxonRanks());
        add(new TermMatcherFactoryEnricher());
        add(new TermMatcherFactoryGlobalNames());
    }});

    public final static Map<String, TermMatcherFactory> registry = Collections.unmodifiableMap(new TreeMap<String, TermMatcherFactory>() {
        {
            for (TermMatcherFactory matcher : matchers) {
                put(matcher.getName(), matcher);
                put("default", MATCHER_FACTORY_DEFAULT);
            }
        }
    });

    public static TermMatcher termMatcherFor(String id, TermMatcherContext ctx) {
        TermMatcherFactory factory = registry.get(id);
        if (factory != null) {
            LOG.info("using matcher [" + factory.getName() + "]");
        }
        return factory == null ? defaultMatcher(ctx) : factory.createTermMatcher(ctx);
    }

    public static TermMatcher defaultMatcher(TermMatcherContext ctx) {
        TermMatcherCacheFactory factory = new TermMatcherCacheFactory();
        LOG.info("using default matcher [" + factory.getName() + "]");
        return factory.createTermMatcher(ctx);
    }
}
