package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.TaxonCacheService;
import org.globalbioticinteractions.nomer.util.CacheUtil;
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
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ITISTaxonService extends CommonLongTaxonService {
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
                           InputStream is) throws PropertyEnricherException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

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
            InputStream resource = getCtx().retrieve(getTaxonUnitTypes());
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
            parseNodes(itisNodes, childParent, rankIdNameMap, getCtx().retrieve(getNodesUrl()));
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse ITIS nodes", e);
        }

        mergedNodes = db
                .createTreeMap(MERGED_NODES)
                .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                .make();


        try {
            parseMerged(mergedNodes, getCtx().retrieve(getMergedNodesUrl()));
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

    private URI getNodesUrl() throws PropertyEnricherException {
        return CacheUtil.getValueURI(getCtx(), "nomer.itis.taxonomic_units");
    }

    private URI getTaxonUnitTypes() throws PropertyEnricherException {
        return CacheUtil.getValueURI(getCtx(), "nomer.itis.taxon_unit_types");
    }

    private URI getMergedNodesUrl() throws PropertyEnricherException {
        return CacheUtil.getValueURI(getCtx(), "nomer.itis.synonym_links");
    }

}
