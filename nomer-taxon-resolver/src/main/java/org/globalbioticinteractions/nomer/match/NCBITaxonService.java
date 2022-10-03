package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.NCBIService;
import org.eol.globi.taxon.PropertyEnricherSimple;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.CacheUtil;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class NCBITaxonService extends PropertyEnricherSimple implements TermMatcher {

    private static final Logger LOG = LoggerFactory.getLogger(NCBITaxonService.class);
    private static final String DENORMALIZED_NODES = "denormalizedNodes";
    private static final String MERGED_NODES = "mergedNodes";
    private static final String NAME_IDS = "nameIds";
    private static final String SYNONYM_IDS = "synonymIds";
    private static final String COMMON_NAME_IDS = "commonNamesIds";


    private final TermMatcherContext ctx;

    private BTreeMap<String, List<String>> nameIds = null;
    private BTreeMap<String, List<String>> synonymIds = null;
    private BTreeMap<String, List<String>> commonNameIds = null;
    private BTreeMap<String, String> mergedNodes = null;
    private BTreeMap<String, Map<String, String>> ncbiDenormalizedNodes = null;

    public NCBITaxonService(TermMatcherContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void match(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException {
        lazyInitIfNeeded();

        for (Term term : terms) {
            List<Taxon> matchedTaxa = new ArrayList<>();
            List<Taxon> matchedSynonyms = new ArrayList<>();
            List<Taxon> matchedCommonNames = new ArrayList<>();

            String ncbiId = NCBIService.getNCBIId(term.getId());
            if (StringUtils.isNotBlank(ncbiId)) {
                Map<String, String> taxonMap = lookupTaxonById(ncbiId);
                if (taxonMap != null) {
                    matchedTaxa.add(TaxonUtil.mapToTaxon(taxonMap));
                }
            } else if (StringUtils.isNotBlank(term.getName())) {
                List<Map<String, String>> matched = lookupTaxaByName(term.getName());
                matched.forEach(taxonForName -> {
                    matchedTaxa.add(TaxonUtil.mapToTaxon(taxonForName));
                });

                List<Map<String, String>> synonyms = lookupTaxaBySynonym(term.getName());
                synonyms.forEach(taxonForName -> {
                    matchedSynonyms.add(TaxonUtil.mapToTaxon(taxonForName));
                });

                List<Map<String, String>> commonNames = lookupTaxaByCommonName(term.getName());
                commonNames.forEach(taxonForName -> {
                    matchedCommonNames.add(TaxonUtil.mapToTaxon(taxonForName));
                });

            }


            if (matchedTaxa.isEmpty() && matchedSynonyms.isEmpty() && matchedCommonNames.isEmpty()) {
                termMatchListener.foundTaxonForTerm(null,
                        term,
                        NameType.NONE,
                        new TaxonImpl(term.getName(), term.getId())
                );
            } else {
                matchedTaxa.forEach(matchedTerm -> {
                    termMatchListener.foundTaxonForTerm(
                            null,
                            term,
                            NameType.SAME_AS,
                            matchedTerm
                    );
                });
                matchedSynonyms.forEach(matchedTerm -> {
                    termMatchListener.foundTaxonForTerm(
                            null,
                            term,
                            NameType.SYNONYM_OF,
                            matchedTerm
                    );
                });
                matchedCommonNames.forEach(matchedTerm -> {
                    termMatchListener.foundTaxonForTerm(
                            null,
                            term,
                            NameType.COMMON_NAME_OF,
                            matchedTerm
                    );
                });
            }
        }
    }

    private List<Map<String, String>> lookupTaxaBySynonym(String name) {
        List<Map<String, String>> matches = new ArrayList<>();
        List<String> ids = synonymIds.get(name);
        if (ids != null) {
            for (String id : ids) {
                Map<String, String> taxonMap = ncbiDenormalizedNodes.get(id);
                if (taxonMap != null) {
                    matches.add(taxonMap);
                }
            }
        }
        return matches;
    }

    private List<Map<String, String>> lookupTaxaByCommonName(String name) {
        List<Map<String, String>> matches = new ArrayList<>();
        List<String> ids = commonNameIds.get(name);
        if (ids != null) {
            for (String id : ids) {
                Map<String, String> taxonMap = ncbiDenormalizedNodes.get(id);
                if (taxonMap != null) {
                    matches.add(taxonMap);
                }
            }
        }
        return matches;
    }

    private List<Map<String, String>> lookupTaxaByName(String name) {
        List<Map<String, String>> matches = new ArrayList<>();
        List<String> ids = nameIds.get(name);
        if (ids != null) {
            for (String id : ids) {
                Map<String, String> taxonMap = ncbiDenormalizedNodes.get(id);
                if (taxonMap != null) {
                    matches.add(taxonMap);
                }
            }
        }
        return matches;
    }

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        String ncbiTaxonId = NCBIService.getNCBITaxonId(properties);
        Map<String, String> enriched = lookupTaxonById(ncbiTaxonId);
        return enriched == null
                ? properties
                : enriched;
    }

    private Map<String, String> lookupTaxonById(String ncbiTaxonId) throws PropertyEnricherException {
        Map<String, String> enrichedProperties = null;
        if (StringUtils.isNotBlank(ncbiTaxonId)) {
            lazyInitIfNeeded();
            String externalId = TaxonomyProvider.ID_PREFIX_NCBI + ncbiTaxonId;
            String idForLookup = mergedNodes.getOrDefault(externalId, externalId);
            enrichedProperties = ncbiDenormalizedNodes.get(idForLookup);
        }
        return enrichedProperties;
    }

    private void lazyInitIfNeeded() throws PropertyEnricherException {
        if (needsInit()) {
            lazyInit();
        }
    }

    private void lazyInit() throws PropertyEnricherException {
        File cacheDir = getCacheDir();
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                throw new PropertyEnricherException("failed to create cache dir at [" + cacheDir.getAbsolutePath() + "]");
            }
        }

        File ncbiTaxonomyDir = new File(getCacheDir(), "ncbi");
        DB db = DBMaker
                .newFileDB(ncbiTaxonomyDir)
                .mmapFileEnableIfSupported()
                .mmapFileCleanerHackDisable()
                .compressionEnable()
                .closeOnJvmShutdown()
                .transactionDisable()
                .make();

        if (db.exists(DENORMALIZED_NODES)
                && db.exists(MERGED_NODES)) {
            LOG.info("NCBI taxonomy already indexed at [" + ncbiTaxonomyDir.getAbsolutePath() + "], no need to import.");
            ncbiDenormalizedNodes = db.getTreeMap(DENORMALIZED_NODES);
            mergedNodes = db.getTreeMap(MERGED_NODES);
            nameIds = db.getTreeMap(NAME_IDS);
            synonymIds = db.getTreeMap(SYNONYM_IDS);
            commonNameIds = db.getTreeMap(COMMON_NAME_IDS);
        } else {
            LOG.info("NCBI taxonomy importing...");
            StopWatch watch = new StopWatch();
            watch.start();

            BTreeMap<String, Map<String, String>> ncbiNodes = db
                    .createTreeMap("nodes")
                    .keySerializer(BTreeKeySerializer.STRING)
                    .valueSerializer(Serializer.JAVA)
                    .make();

            BTreeMap<String, String> childParent = db
                    .createTreeMap("childParent")
                    .keySerializer(BTreeKeySerializer.STRING)
                    .valueSerializer(Serializer.STRING)
                    .make();

            try {
                parseNodes(ncbiNodes, childParent, ctx.retrieve(getNodesUrl()));
            } catch (IOException e) {
                throw new PropertyEnricherException("failed to parse NCBI nodes", e);
            }

            mergedNodes = db
                    .createTreeMap(MERGED_NODES)
                    .keySerializer(BTreeKeySerializer.STRING)
                    .valueSerializer(Serializer.STRING)
                    .make();

            nameIds = db
                    .createTreeMap(NAME_IDS)
                    .keySerializer(BTreeKeySerializer.STRING)
                    .valueSerializer(Serializer.JAVA)
                    .make();

            synonymIds = db
                    .createTreeMap(SYNONYM_IDS)
                    .keySerializer(BTreeKeySerializer.STRING)
                    .valueSerializer(Serializer.JAVA)
                    .make();

            commonNameIds = db
                    .createTreeMap(COMMON_NAME_IDS)
                    .keySerializer(BTreeKeySerializer.STRING)
                    .valueSerializer(Serializer.JAVA)
                    .make();


            try {
                parseMerged(mergedNodes, ctx.retrieve(getMergedNodesUrl()));
            } catch (IOException e) {
                throw new PropertyEnricherException("failed to parse NCBI nodes", e);
            }

            BTreeMap<String, String> ncbiNames = db
                    .createTreeMap("names")
                    .keySerializer(BTreeKeySerializer.STRING)
                    .valueSerializer(Serializer.STRING)
                    .make();

            try {
                parseNames(ctx.retrieve(getNamesUrl()), ncbiNames, nameIds, commonNameIds, synonymIds);
            } catch (IOException e) {
                throw new PropertyEnricherException("failed to parse NCBI nodes", e);
            }


            ncbiDenormalizedNodes = db
                    .createTreeMap(DENORMALIZED_NODES)
                    .keySerializer(BTreeKeySerializer.STRING)
                    .valueSerializer(Serializer.JAVA)
                    .make();
            denormalizeTaxa(ncbiNodes, ncbiDenormalizedNodes, childParent, ncbiNames);

            watch.stop();
            TaxonCacheService.logCacheLoadStats(watch.getTime(), ncbiNodes.size(), LOG);
            LOG.info("NCBI taxonomy imported.");
        }
    }

    private boolean needsInit() {
        return ncbiDenormalizedNodes == null;
    }

    @Override
    public void shutdown() {

    }

    private File getCacheDir() {
        File cacheDir = new File(ctx.getCacheDir(), "ncbi");
        cacheDir.mkdirs();
        return cacheDir;
    }

    private URI getNodesUrl() throws PropertyEnricherException {
        return CacheUtil.getValueURI(ctx, "nomer.ncbi.nodes");
    }

    private URI getMergedNodesUrl() throws PropertyEnricherException {
        return CacheUtil.getValueURI(ctx, "nomer.ncbi.merged");
    }

    private URI getNamesUrl() throws PropertyEnricherException {
        return CacheUtil.getValueURI(ctx, "nomer.ncbi.names");
    }

    static void parseNodes(Map<String, Map<String, String>> taxonMap, Map<String, String> childParent, InputStream resourceAsStream) throws PropertyEnricherException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "\t|\t");
                String taxId = rowValues[0];
                String parentTaxId = rowValues[1];
                String rank = rowValues[2];

                String externalId = TaxonomyProvider.ID_PREFIX_NCBI + taxId;
                TaxonImpl taxon = new TaxonImpl(null, externalId);
                taxon.setRank(StringUtils.equals(StringUtils.trim(rank), "no rank") ? "" : rank);
                taxonMap.put(externalId, TaxonUtil.taxonToMap(taxon));
                childParent.put(TaxonomyProvider.ID_PREFIX_NCBI + taxId, TaxonomyProvider.ID_PREFIX_NCBI + parentTaxId);
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse NCBI taxon dump", e);
        }
    }

    static void denormalizeTaxa(Map<String, Map<String, String>> taxonMap, Map<String, Map<String, String>> taxonMapDenormalized, Map<String, String> childParent, Map<String, String> taxonNames) {
        Set<Map.Entry<String, Map<String, String>>> taxa = taxonMap.entrySet();
        for (Map.Entry<String, Map<String, String>> taxon : taxa) {
            denormalizeTaxa(taxonMap, taxonMapDenormalized, childParent, taxonNames, taxon);
        }
    }

    private static void denormalizeTaxa(Map<String, Map<String, String>> taxonMap, Map<String, Map<String, String>> taxonEnrichMap, Map<String, String> childParent, Map<String, String> names, Map.Entry<String, Map<String, String>> taxon) {
        Map<String, String> childTaxon = taxon.getValue();
        List<String> pathNames = new ArrayList<>();
        List<String> pathIds = new ArrayList<>();
        List<String> path = new ArrayList<>();

        Taxon origTaxon = TaxonUtil.mapToTaxon(childTaxon);

        String str = names.get(origTaxon.getExternalId());
        origTaxon.setName(str);
        path.add(StringUtils.defaultIfBlank(str, ""));
        String externalId = origTaxon.getExternalId();
        origTaxon.setExternalId(externalId);
        pathIds.add(StringUtils.defaultIfBlank(externalId, ""));

        origTaxon.setRank(origTaxon.getRank());
        pathNames.add(StringUtils.defaultIfBlank(origTaxon.getRank(), ""));

        String parent = childParent.get(taxon.getKey());
        while (StringUtils.isNotBlank(parent) && !pathIds.contains(parent)) {
            Map<String, String> stringStringMap = taxonMap.get(parent);
            if (stringStringMap != null) {
                Taxon parentTaxon = TaxonUtil.mapToTaxon(stringStringMap);
                pathNames.add(StringUtils.defaultIfBlank(parentTaxon.getRank(), ""));
                pathIds.add(StringUtils.defaultIfBlank(parentTaxon.getExternalId(), ""));
                path.add(StringUtils.defaultIfBlank(names.get(parentTaxon.getExternalId()), ""));
            }
            parent = childParent.get(parent);
        }

        Collections.reverse(pathNames);
        Collections.reverse(pathIds);
        Collections.reverse(path);

        origTaxon.setPath(StringUtils.join(path, CharsetConstant.SEPARATOR));
        origTaxon.setPathIds(StringUtils.join(pathIds, CharsetConstant.SEPARATOR));
        origTaxon.setPathNames(StringUtils.join(pathNames, CharsetConstant.SEPARATOR));

        taxonEnrichMap.put(taxon.getKey(), TaxonUtil.taxonToMap(origTaxon));
    }

    static void parseNames(InputStream resourceAsStream, Map<String, String> nameMap,
                           Map<String, List<String>> nameIds,
                           Map<String, List<String>> commonNameIds,
                           Map<String, List<String>> synonymIds) throws PropertyEnricherException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "\t|\t");
                if (rowValues.length > 3) {
                    String taxId = rowValues[0];
                    String taxonName = rowValues[1];
                    String rank = rowValues[2];
                    String taxonNameClass = StringUtils.replace(rowValues[3], "\t|", "");

                    Stream<String> knownNameClasses = Stream.of(
                            "acronym",
                            "anamorph",
                            "authority",
                            "blast name",
                            "common name",
                            "equivalent name",
                            "genbank acronym",
                            "genbank common name",
                            "genbank synonym",
                            "includes",
                            "in-part",
                            "scientific name",
                            "synonym",
                            "teleomorph",
                            "type material");

                    if (StringUtils.equals("scientific name", taxonNameClass)) {
                        String ncbiTaxonId = TaxonomyProvider.ID_PREFIX_NCBI + taxId;
                        nameMap.put(ncbiTaxonId, taxonName);
                        addIdMapEntry(nameIds, taxonName, ncbiTaxonId);
                    } else if (StringUtils.equals("synonym", taxonNameClass)) {
                        String ncbiTaxonId = TaxonomyProvider.ID_PREFIX_NCBI + taxId;
                        addIdMapEntry(synonymIds, taxonName, ncbiTaxonId);
                    } else if (Arrays.asList("genbank common name", "common name").contains(taxonNameClass)) {
                        String ncbiTaxonId = TaxonomyProvider.ID_PREFIX_NCBI + taxId;
                        addIdMapEntry(commonNameIds, taxonName, ncbiTaxonId);
                    }

                }
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse NCBI taxon dump", e);
        }
    }

    private static void addIdMapEntry(Map<String, List<String>> nameIds,
                                      String taxonName,
                                      String key) {
        List<String> ids = nameIds.get(taxonName);
        if (ids == null) {
            ids = new ArrayList<>();
        }
        if (!ids.contains(key)) {
            ids.add(key);
        }
        nameIds.put(taxonName, ids);
    }

    static void parseMerged(Map<String, String> mergedMap, InputStream resourceAsStream) throws PropertyEnricherException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "\t|\t");
                if (rowValues.length > 1) {
                    String oldTaxId = rowValues[0];
                    String newTaxId = StringUtils.replace(rowValues[1], "\t|", "");
                    if (StringUtils.isNotBlank(oldTaxId) && StringUtils.isNotBlank(newTaxId)) {
                        mergedMap.put(TaxonomyProvider.ID_PREFIX_NCBI + oldTaxId, TaxonomyProvider.ID_PREFIX_NCBI + newTaxId);
                    }
                }
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse NCBI taxon dump", e);
        }
    }


}
