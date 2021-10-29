package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.util.ExternalIdUtil;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ITISTaxonService extends CommonTaxonService {
    private static final Logger LOG = LoggerFactory.getLogger(ITISTaxonService.class);

    public ITISTaxonService(TermMatcherContext ctx) {
        super(ctx);
    }

    @Override
    public TaxonomyProvider getTaxonomyProvider() {
        return TaxonomyProvider.ITIS;
    }

    static void parseNodes(Map<Long, Map<String, String>> taxonMap,
                           Map<Long, Long> childParent,
                           Map<String, String> rankIdNameMap,
                           InputStream resourceAsStream) throws PropertyEnricherException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "|");
                if (rowValues.length > 24) {
                    String taxId = rowValues[0];
                    String parentTaxId = rowValues[17];
                    String rankKingdomId = rowValues[20];
                    String rankId = rowValues[21];
                    String rankKey = rankKingdomId + "-" + rankId;
                    String rank = rankIdNameMap.getOrDefault(rankKey, rankKey);

                    String completeName = rowValues[25];

                    TaxonImpl taxon = new TaxonImpl(completeName, TaxonomyProvider.ID_PREFIX_ITIS + taxId);
                    taxon.setRank(StringUtils.equals(StringUtils.trim(rank), "no rank") ? "" : rank);
                    if (NumberUtils.isCreatable(taxId)) {
                        taxonMap.put(Long.parseLong(taxId), TaxonUtil.taxonToMap(taxon));
                        if (NumberUtils.isCreatable(parentTaxId)) {
                            childParent.put(
                                    Long.parseLong(taxId),
                                    Long.parseLong(parentTaxId)
                            );
                        }
                    }


                }
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse ITIS taxon dump", e);
        }
    }

    static void parseTaxonUnitTypes(Map<String, String> rankIdMap, InputStream resourceAsStream) throws PropertyEnricherException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "|");
                if (rowValues.length > 3) {
                    String kingdomId = rowValues[0];
                    String rankId = rowValues[1];
                    String rankName = rowValues[2];
                    rankIdMap.put(kingdomId + "-" + rankId, StringUtils.lowerCase(rankName));
                }
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse ITIS taxon unit types", e);
        }
    }

    static void denormalizeTaxa(Map<Long, Map<String, String>> taxonMap,
                                Map<String, List<Map<String, String>>> taxonMapDenormalized,
                                Map<Long, List<Map<String, String>>> taxonMapIdDenormalized,
                                Map<Long, Long> childParent) {
        Set<Map.Entry<Long, Map<String, String>>> taxa = taxonMap.entrySet();
        for (Map.Entry<Long, Map<String, String>> taxon : taxa) {
            denormalizeTaxa(taxonMap, taxonMapDenormalized, taxonMapIdDenormalized, childParent, taxon);
        }
    }

    private static void denormalizeTaxa(Map<Long, Map<String, String>> taxonMap,
                                        Map<String, List<Map<String, String>>> taxonEnrichMap,
                                        Map<Long, List<Map<String, String>>> taxonEnrichIdMap,
                                        Map<Long, Long> childParent,
                                        Map.Entry<Long, Map<String, String>> taxon) {
        Map<String, String> childTaxon = taxon.getValue();
        List<String> pathNames = new ArrayList<>();
        List<String> pathIds = new ArrayList<>();
        List<String> path = new ArrayList<>();

        Taxon origTaxon = TaxonUtil.mapToTaxon(childTaxon);

        path.add(StringUtils.defaultIfBlank(origTaxon.getName(), ""));

        pathIds.add(origTaxon.getExternalId());

        pathNames.add(StringUtils.defaultIfBlank(origTaxon.getRank(), ""));

        Long parent = childParent.get(taxon.getKey());
        while (parent != null && !pathIds.contains(TaxonomyProvider.ID_PREFIX_ITIS + parent)) {
            Map<String, String> parentTaxonProperties = taxonMap.get(parent);
            if (parentTaxonProperties != null) {
                Taxon parentTaxon = TaxonUtil.mapToTaxon(parentTaxonProperties);
                path.add(StringUtils.defaultIfBlank(parentTaxon.getName(), ""));
                pathNames.add(StringUtils.defaultIfBlank(parentTaxon.getRank(), ""));
                pathIds.add(parentTaxon.getExternalId());
            }
            parent = childParent.get(parent);
        }

        Collections.reverse(pathNames);
        Collections.reverse(pathIds);
        Collections.reverse(path);

        origTaxon.setPath(StringUtils.join(path, CharsetConstant.SEPARATOR));
        origTaxon.setPathIds(StringUtils.join(pathIds, CharsetConstant.SEPARATOR));
        origTaxon.setPathNames(StringUtils.join(pathNames, CharsetConstant.SEPARATOR));

        updateId(taxonEnrichIdMap,
                taxon.getKey(),
                origTaxon);
        update(taxonEnrichMap, origTaxon.getName(), origTaxon);
    }

    private static void update(Map<String, List<Map<String, String>>> taxonEnrichMap,
                               String key,
                               Taxon origTaxon) {
        List<Map<String, String>> existing = taxonEnrichMap.get(key);

        List<Map<String, String>> updated = existing == null
                ? new ArrayList<>()
                : new ArrayList<>(existing);

        updated.add(TaxonUtil.taxonToMap(origTaxon));
        taxonEnrichMap.put(key, updated);
    }

    private static void updateId(
            Map<Long, List<Map<String, String>>> taxonEnrichIdMap,
            Long key,
            Taxon origTaxon
    ) {
        List<Map<String, String>> existing = taxonEnrichIdMap.get(key);

        List<Map<String, String>> updated = existing == null
                ? new ArrayList<>()
                : new ArrayList<>(existing);

        updated.add(TaxonUtil.taxonToMap(origTaxon));
        taxonEnrichIdMap.put(key, updated);
    }

    static void parseMerged(Map<Long, Long> mergedMap, InputStream resourceAsStream) throws PropertyEnricherException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "|");
                if (rowValues.length > 1) {
                    String oldTaxId = rowValues[0];
                    String newTaxId = rowValues[1];
                    if (StringUtils.isNotBlank(oldTaxId) && StringUtils.isNotBlank(newTaxId)) {
                        mergedMap.put(
                                Long.parseLong(oldTaxId),
                                Long.parseLong(newTaxId)
                        );
                    }
                }
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse ITIS taxon dump", e);
        }
    }

    @Override
    protected void lazyInit() throws PropertyEnricherException {
        File cacheDir = getCacheDir();
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                throw new PropertyEnricherException("failed to create cache dir at [" + cacheDir.getAbsolutePath() + "]");
            }
        }

        File taxonomyDir = new File(cacheDir, StringUtils.lowerCase(getTaxonomyProvider().name()));
        DB db = DBMaker
                .newFileDB(taxonomyDir)
                .mmapFileEnableIfSupported()
                .compressionEnable()
                .closeOnJvmShutdown()
                .transactionDisable()
                .make();

        if (db.exists(DENORMALIZED_NODES)
                && db.exists(DENORMALIZED_NODE_IDS)
                && db.exists(MERGED_NODES)) {
            LOG.info("ITIS taxonomy already indexed at [" + taxonomyDir.getAbsolutePath() + "], no need to import.");
            denormalizedNodes = db.getTreeMap(DENORMALIZED_NODES);
            denormalizedNodeIds = db.getTreeMap(DENORMALIZED_NODE_IDS);
            mergedNodes = db.getTreeMap(MERGED_NODES);
        } else {
            indexITIS(db);
        }
    }

    private void indexITIS(DB db) throws PropertyEnricherException {
        LOG.info("ITIS taxonomy importing...");
        StopWatch watch = new StopWatch();
        watch.start();

        BTreeMap<String, String> rankIdNameMap = db
                .createTreeMap("rankIdNameMap")
                .keySerializer(BTreeKeySerializer.STRING)
                .make();

        try {
            InputStream resource = getCtx().getResource(getTaxonUnitTypes());
            if (resource == null) {
                throw new PropertyEnricherException("ITIS init failure: failed to find [" + getTaxonUnitTypes() + "]");
            }
            parseTaxonUnitTypes(rankIdNameMap, resource);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse ITIS taxon unit types", e);
        }

        BTreeMap<Long, Map<String, String>> itisNodes = db
                .createTreeMap("nodes")
                .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                .make();

        BTreeMap<Long, Long> childParent = db
                .createTreeMap("childParent")
                .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                .make();

        try {
            parseNodes(itisNodes, childParent, rankIdNameMap, getCtx().getResource(getNodesUrl()));
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse ITIS nodes", e);
        }

        mergedNodes = db
                .createTreeMap(MERGED_NODES)
                .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                .make();


        try {
            parseMerged(mergedNodes, getCtx().getResource(getMergedNodesUrl()));
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse ITIS nodes", e);
        }

        denormalizedNodes = db
                .createTreeMap(DENORMALIZED_NODES)
                .keySerializer(BTreeKeySerializer.STRING)
                .make();

        denormalizedNodeIds = db
                .createTreeMap(DENORMALIZED_NODE_IDS)
                .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                .make();

        denormalizeTaxa(
                itisNodes,
                denormalizedNodes,
                denormalizedNodeIds,
                childParent);

        watch.stop();
        TaxonCacheService.logCacheLoadStats(watch.getTime(), itisNodes.size(), LOG);
        LOG.info("[" + getTaxonomyProvider().name() + "] taxonomy imported.");
    }

    @Override
    public void shutdown() {

    }

    private String getNodesUrl() throws PropertyEnricherException {
        return getCtx().getProperty("nomer.itis.taxonomic_units");
    }

    private String getTaxonUnitTypes() throws PropertyEnricherException {
        return getCtx().getProperty("nomer.itis.taxon_unit_types");
    }

    private String getMergedNodesUrl() throws PropertyEnricherException {
        return getCtx().getProperty("nomer.itis.synonym_links");
    }

}
