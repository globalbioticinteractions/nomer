package org.globalbioticinteractions.nomer.match;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.list.TreeList;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TermMatcherRegistry {

    public static final List<String> SUPPORTED_SHORT_NAMES = Collections.unmodifiableList(Arrays.asList(
            "ala",
            "bold-web",
            "crossref-doi",
            "discoverlife",
            "envo",
            "eol",
            "gbif",
            "gbif-web",
            "globi-correct",
            "globi-enrich",
            "globalnames",
            "globi",
            "globi-rank",
            "gulfbase",
            "inaturalist-id",
            "itis",
            "itis-web",
            "nbn",
            "ncbi",
            "ncbi-web",
            "nodc",
            "openbiodiv",
            "plazi",
            "pmid-doi",
            "remove-stop-words",
            "translate-names",
            "wikidata-web",
            "worms",
            "col",
            "wfo",
            "ott",
            "orcid-web"));

    static final Map<String, String> MATCH_NAME_MAPPER = Collections.unmodifiableMap(new TreeMap<String, String>() {
        {
            put("ala-taxon", "ala");
            put("bold-web", "bold-web");
            put("crossref-doi", "crossref-doi");
            put("discoverlife-taxon", "discoverlife");
            put("envo-term", "envo");
            put("eol-taxon-id", "eol");
            put("gbif-taxon", "gbif");
            put("gbif-taxon-web", "gbif-web");
            put("globi-correct", "globi-correct");
            put("globi-enrich", "globi-enrich");
            put("globi-globalnames", "globalnames");
            put("globi-taxon-cache", "globi");
            put("globi-taxon-rank", "globi-rank");
            put("gulfbase-taxon", "gulfbase");
            put("inaturalist-taxon-id", "inaturalist-id");
            put("itis-taxon-id", "itis");
            put("itis-taxon-id-web", "itis-web");
            put("nbn-taxon-id", "nbn");
            put("ncbi-taxon", "ncbi");
            put("ncbi-taxon-id", "ncbi");
            put("ncbi-taxon-id-web", "ncbi-web");
            put("nodc-taxon-id", "nodc");
            put("openbiodiv", "openbiodiv");
            put("plazi", "plazi");
            put("pmid-doi", "pmid-doi");
            put("remove-stop-words", "remove-stop-words");
            put("translate-names", "translate-names");
            put("wikidata-taxon-id-web", "wikidata-web");
            put("worms-taxon", "worms");
            put("indexfungorum", "indexfungorum");
            put("col", "col");
            put("wfo", "wfo");
            put("gn-parse", "gn-parse");
            put("uksi-current-name", "uksi-current-name");
            put("gbif-parse", "gbif-parse");
            put("ott", "ott");
            put("orcid-web", "orcid-web");
            put("batnames", "batnames");
        }
    });

    private static final List<String> DEPRECATED_LONG_NAMES = Arrays.asList(
            "ncbi-taxon-id"
    );
    private static final Logger LOG = LoggerFactory.getLogger(TermMatcherRegistry.class);

    private static final TermMatcherCacheFactory MATCHER_FACTORY_DEFAULT = new TermMatcherCacheFactory();

    private final static List<TermMatcherFactory> matchers = Collections.unmodifiableList(new TreeList<TermMatcherFactory>() {{
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
        add(new TermMatcherDiscoverLifeTaxonFactory());
        add(new TermMatcherITISFactory());
        add(new TermMatcherIndexFungorumFactory());
        add(new TermMatcherCatalogueOfLifeFactory());
        add(new TermMatcherWorldOfFloraOnlineFactory());
        add(new TermMatcherGNParseFactory());
        add(new TermMatcherUSKIFactory());
        add(new TermMatcherGBIFParseFactory());
        add(new TermMatcherOpenTreeOfLifeFactory());
        add(new TermMatcherBatNamesTaxonFactory());
    }});

    public static Map<String, TermMatcherFactory> getRegistry(TermMatcherContext ctx) {
        Map<String, TermMatcherFactory> registryDynamic = new HashMap<>(registry);
        List<TermMatcherFactory> termMatchFactories = new TermMatcherFactoryEnricherFactory().createTermMatchFactories(ctx);
        for (TermMatcherFactory termMatchFactory : termMatchFactories) {
            registryDynamic.put(termMatchFactory.getPreferredName(), termMatchFactory);
        }
        return MapUtils.unmodifiableMap(registryDynamic);
    }

    private final static Map<String, TermMatcherFactory> registry = Collections.unmodifiableMap(new TreeMap<String, TermMatcherFactory>() {
        {
            for (TermMatcherFactory matcher : matchers) {
                put(matcher.getPreferredName(), matcher);
            }

        }
    });

    public static TermMatcher termMatcherFor(String id, TermMatcherContext ctx) {
        TermMatcherFactory factory = getRegistry(ctx).get(id);
        if (factory == null) {
            factory = getRegistry(ctx).get(getMatcherLongName(id));
        }

        if (factory == null) {
            throw new IllegalArgumentException("unknown matcher [" + id + "]");
        } else {
            LOG.info("using matcher [" + factory.getPreferredName() + "]");
        }
        return factory.createTermMatcher(ctx);
    }

    public static TermMatcher defaultMatcher(TermMatcherContext ctx) {
        TermMatcherCacheFactory factory = new TermMatcherCacheFactory();
        LOG.info("using default matcher [" + factory.getPreferredName() + "]");
        return factory.createTermMatcher(ctx);
    }

    public static String getMatcherLongName(String shortName) {
        Stream<Map.Entry<String, String>> entryStream
                = MATCH_NAME_MAPPER
                .entrySet()
                .stream()
                .filter(e -> StringUtils.equals(e.getValue(), shortName));

        List<String> matchingNames = entryStream
                .map(Map.Entry::getKey)
                .filter(longName -> !DEPRECATED_LONG_NAMES.contains(longName))
                .collect(Collectors.toList());

        if (matchingNames.size() > 1) {
            throw new IllegalArgumentException("ambiguous mapping for [" + shortName + "]");
        }
        return matchingNames.size() == 1 ? matchingNames.get(0) : null;
    }

    public static String getMatcherShortName(String longName) {
        String shortName = MATCH_NAME_MAPPER.get(longName);

        if (StringUtils.isBlank(shortName)) {
            shortName = SUPPORTED_SHORT_NAMES.contains(longName) ? longName : null;
        }

        return shortName;
    }
}
