package org.globalbioticinteractions.nomer.match;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.list.TreeList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TermMatcherRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(TermMatcherRegistry.class);

    private static final TermMatcherCacheFactory MATCHER_FACTORY_DEFAULT = new TermMatcherCacheFactory();

    private final static List<TermMatcherFactory> matchers = Collections.unmodifiableList(new TreeList<TermMatcherFactory>(){{
        add(MATCHER_FACTORY_DEFAULT);
        add(new TermMatcherTranslateNamesFactory());
        add(new TermMatcherStopWordFactory());
        add(new TermMatcherCorrectFactory());
        add(new TermMatcherFactoryTaxonRanks());
        add(new TermMatcherFactoryEnsembleEnricher());
        add(new TermMatcherFactoryGlobalNames());
        add(new TermMatcherDOIFactory());
        add(new TermMatcherPMDID2DOIFactory());
        add(new TermMatcherWikidataFactory());
        add(new TermMatcherPlaziFactory());
        add(new TermMatcherOpenBiodivFactory());
        add(new TermMatcherNCBITaxonFactory());
        add(new TermMatcherGBIFTaxonFactory());
    }});

    public static Map<String, TermMatcherFactory> getRegistry(TermMatcherContext ctx) {
        Map<String, TermMatcherFactory> registryDynamic = new HashMap<>(registry);
        List<TermMatcherFactory> termMatchFactories = new TermMatcherFactoryEnricherFactory().createTermMatchFactories(ctx);
        for (TermMatcherFactory termMatchFactory : termMatchFactories) {
            registryDynamic.put(termMatchFactory.getName(), termMatchFactory);
        }
        return MapUtils.unmodifiableMap(registryDynamic);
    }

    private final static Map<String, TermMatcherFactory> registry = Collections.unmodifiableMap(new TreeMap<String, TermMatcherFactory>() {
        {
            for (TermMatcherFactory matcher : matchers) {
                put(matcher.getName(), matcher);
            }

        }
    });

    public static TermMatcher termMatcherFor(String id, TermMatcherContext ctx) {
        TermMatcherFactory factory = getRegistry(ctx).get(id);
        if (factory == null) {
            throw new IllegalArgumentException("unknown matcher [" + id + "]");
        } else {
            LOG.info("using matcher [" + factory.getName() + "]");
        }
        return factory.createTermMatcher(ctx);
    }

    public static TermMatcher defaultMatcher(TermMatcherContext ctx) {
        TermMatcherCacheFactory factory = new TermMatcherCacheFactory();
        LOG.info("using default matcher [" + factory.getName() + "]");
        return factory.createTermMatcher(ctx);
    }
}
