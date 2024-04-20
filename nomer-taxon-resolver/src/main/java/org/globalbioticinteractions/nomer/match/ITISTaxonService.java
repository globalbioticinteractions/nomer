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
import org.mapdb.Serializer;
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

public class ITISTaxonService extends CommonLongTaxonService {
    private static final Logger LOG = LoggerFactory.getLogger(ITISTaxonService.class);

    private static final String AUTHORS = "author";
    private BTreeMap<Long, String> authorIds;


    public ITISTaxonService(TermMatcherContext ctx) {
        super(ctx);
    }

    @Override
    public TaxonomyProvider getTaxonomyProvider() {
        return TaxonomyProvider.ITIS;
    }

    void parseNodes(Map<Long, Map<String, String>> taxonMap,
                    Map<Long, Long> childParent,
                    Map<String, String> rankIdNameMap,
                    Map<String, List<Long>> name2nodeIds,
                    Map<Long, String> authorIds,
                    InputStream is) throws PropertyEnricherException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "|");
                if (rowValues.length > 24) {
                    String taxId = rowValues[0];
                    String parentTaxId = rowValues[17];
                    String authorIdString = rowValues[18];

                    String authorship = "ITIS:AUTHORSHIP:" + authorIdString;
                    if (StringUtils.isNumeric(authorIdString)) {
                        long authorId = Long.parseLong(authorIdString);
                        authorship = authorId == 0 ? "" : authorIds.getOrDefault(authorId, authorship);
                    }

                    String rankKingdomId = rowValues[20];

                    String rankId = rowValues[21];
                    String rankKey = rankKingdomId + "-" + rankId;
                    String rank = rankIdNameMap.getOrDefault(rankKey, rankKey);

                    String completeName = rowValues[25];

                    TaxonImpl taxon = new TaxonImpl(completeName, TaxonomyProvider.ID_PREFIX_ITIS + taxId);
                    taxon.setRank(StringUtils.equals(StringUtils.trim(rank), "no rank") ? "" : rank);

                    if (StringUtils.isNotBlank(authorship)) {
                        taxon.setAuthorship(authorship);
                    }

                    if (NumberUtils.isCreatable(taxId)) {
                        Long taxonKey = Long.parseLong(taxId);
                        registerIdForName(taxonKey, taxon, name2nodeIds);
                        taxonMap.put(taxonKey, TaxonUtil.taxonToMap(taxon));
                        if (NumberUtils.isCreatable(parentTaxId)) {
                            childParent.put(
                                    taxonKey,
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

    static void parseAuthors(Map<Long, String> authorIds, InputStream resourceAsStream) throws PropertyEnricherException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "|");
                if (rowValues.length > 3) {
                    String authorIdString = rowValues[0];
                    String authorship = rowValues[1];
                    authorIds.put(Long.parseLong(authorIdString), authorship);
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

        File taxonomyDir = new File(getCacheDir(), StringUtils.lowerCase(getTaxonomyProvider().name()));
        DB db = DBMaker
                .newFileDB(taxonomyDir)
                .mmapFileEnableIfSupported()
                .mmapFileCleanerHackDisable()
                .compressionEnable()
                .closeOnJvmShutdown()
                .transactionDisable()
                .make();

        if (db.exists(NODES)
                && db.exists(CHILD_PARENT)
                && db.exists(MERGED_NODES)
                && db.exists(NAME_TO_NODE_IDS)
                && db.exists(AUTHORS)) {
            LOG.debug("ITIS taxonomy already indexed at [" + taxonomyDir.getAbsolutePath() + "], no need to import.");
            nodes = db.getTreeMap(NODES);
            childParent = db.getTreeMap(CHILD_PARENT);
            mergedNodes = db.getTreeMap(MERGED_NODES);
            name2nodeIds = db.getTreeMap(NAME_TO_NODE_IDS);
            authorIds = db.getTreeMap(AUTHORS);
        } else {
            indexITIS(db);
        }
    }

    private void indexITIS(DB db) throws PropertyEnricherException {
        LOG.info("ITIS taxonomy importing...");
        StopWatch watch = new StopWatch();
        watch.start();

        BTreeMap<String, String> rankIdNameMap = DBMaker.newTempTreeMap();

        try {
            InputStream resource = getCtx().retrieve(getTaxonUnitTypes());
            if (resource == null) {
                throw new PropertyEnricherException("ITIS init failure: failed to find [" + getTaxonUnitTypes() + "]");
            }
            parseTaxonUnitTypes(rankIdNameMap, resource);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse ITIS taxon unit types", e);
        }

        authorIds = db
                .createTreeMap(AUTHORS)
                .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                .valueSerializer(Serializer.STRING)
                .make();

        try {
            InputStream resource = getCtx().retrieve(getTaxonAuthors());
            if (resource == null) {
                throw new PropertyEnricherException("ITIS init failure: failed to find [" + getTaxonAuthors() + "]");
            }
            parseAuthors(authorIds, resource);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse ITIS taxon unit types", e);
        }

        nodes = db
                .createTreeMap(NODES)
                .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                .valueSerializer(Serializer.JAVA)
                .make();

        childParent = db
                .createTreeMap(CHILD_PARENT)
                .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                .valueSerializer(Serializer.LONG)
                .make();

        name2nodeIds = db
                .createTreeMap(NAME_TO_NODE_IDS)
                .keySerializer(BTreeKeySerializer.STRING)
                .valueSerializer(Serializer.JAVA)
                .make();

        try {
            parseNodes(nodes, childParent, rankIdNameMap, name2nodeIds, authorIds, getCtx().retrieve(getNodesUrl()));
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse ITIS nodes", e);
        }

        mergedNodes = db
                .createTreeMap(MERGED_NODES)
                .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                .valueSerializer(Serializer.LONG)
                .make();


        try {
            parseMerged(mergedNodes, getCtx().retrieve(getMergedNodesUrl()));
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse ITIS nodes", e);
        }

        watch.stop();
        TaxonCacheService.logCacheLoadStats(watch.getTime(), nodes.size(), LOG);
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

    private URI getTaxonAuthors() throws PropertyEnricherException {
        return CacheUtil.getValueURI(getCtx(), "nomer.itis.taxon_authors_lkp");
    }

}
