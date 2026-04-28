package org.globalbioticinteractions.nomer.match;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.util.CSVTSVUtil;
import org.globalbioticinteractions.nomer.util.CacheUtil;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class IRMNGTaxonService extends CommonLongTaxonService {
    private static final Logger LOG = LoggerFactory.getLogger(IRMNGTaxonService.class);

    public IRMNGTaxonService(TermMatcherContext ctx) {
        super(ctx);
    }

    @Override
    public TaxonomyProvider getTaxonomyProvider() {
        return TaxonomyProvider.INTERIM_REGISTER_OF_MARINE_AND_NONMARINE_GENERA;
    }

    void parseNodes(Map<Long, Map<String, String>> taxonMap,
                    Map<Long, Long> childParent,
                    Map<String, List<Long>> name2nodeIds,
                    BTreeMap<Long, Long> mergedNodes,
                    InputStream is) throws PropertyEnricherException {
        try {
            LabeledCSVParser labeledTSVParser = CSVTSVUtil.createLabeledTSVParser(is);

            while (labeledTSVParser.getLine() != null) {
                String providedId = getTaxonID(labeledTSVParser.getValueByLabel("taxonID"));
                String providedName = labeledTSVParser.getValueByLabel("scientificName");
                String providedRank = StringUtils.lowerCase(labeledTSVParser.getValueByLabel("taxonRank"));
                String providedParentId = getTaxonID(labeledTSVParser.getValueByLabel("parentNameUsageID"));
                Taxon providedTaxon = new TaxonImpl(
                        providedName,
                        TaxonomyProvider.INTERIM_REGISTER_OF_MARINE_AND_NONMARINE_GENERA.getIdPrefix() + providedId
                );
                providedTaxon.setRank(providedRank);

                String acceptedId = getTaxonID(labeledTSVParser.getValueByLabel("acceptedNameUsageID"));
                String acceptedName = labeledTSVParser.getValueByLabel("acceptedNameUsage");
                Taxon acceptedTaxon = new TaxonImpl(
                        acceptedName,
                        acceptedId
                );

                String authorship = labeledTSVParser.getValueByLabel("scientificNameAuthorship");

                providedTaxon.setAuthorship(authorship);

                if (NumberUtils.isCreatable(providedId)) {
                    Long taxonKey = Long.parseLong(providedId);
                    registerIdForName(taxonKey, providedTaxon, name2nodeIds);
                    taxonMap.put(taxonKey, TaxonUtil.taxonToMap(providedTaxon));
                    if (NumberUtils.isCreatable(providedParentId)) {
                        childParent.put(
                                taxonKey,
                                Long.parseLong(providedParentId)
                        );
                    }

                    if (!StringUtils.equals(providedId, acceptedId)) {
                        mergedNodes.put(Long.parseLong(providedId), Long.parseLong(acceptedId));
                    }
                }


            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse IRMNG taxon dump", e);
        }
    }

    private static @Nullable String getTaxonID(String taxonID) {
        return StringUtils.replace(taxonID, "urn:lsid:irmng.org:taxname:", "");
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
                && db.exists(NAME_TO_NODE_IDS)) {
            LOG.debug("taxonomy already indexed at [" + taxonomyDir.getAbsolutePath() + "], no need to import.");
            nodes = db.getTreeMap(NODES);
            childParent = db.getTreeMap(CHILD_PARENT);
            mergedNodes = db.getTreeMap(MERGED_NODES);
            name2nodeIds = db.getTreeMap(NAME_TO_NODE_IDS);
        } else {
            index(db);
        }
    }

    private void index(DB db) throws PropertyEnricherException {
        LOG.info("taxonomy importing...");
        StopWatch watch = new StopWatch();
        watch.start();

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

        mergedNodes = db
                .createTreeMap(MERGED_NODES)
                .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                .valueSerializer(Serializer.LONG)
                .make();

        try {
            parseNodes(
                    nodes,
                    childParent,
                    name2nodeIds,
                    mergedNodes,
                    getCtx().retrieve(getNodesUrl())
            );
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse nodes", e);
        }

        watch.stop();
        TaxonCacheService.logCacheLoadStats(watch.getTime(), nodes.size(), LOG);
        LOG.info("[" + getTaxonomyProvider().name() + "] taxonomy imported.");
    }

    private URI getNodesUrl() throws PropertyEnricherException {
        return CacheUtil.getValueURI(getCtx(), "nomer.irmng.taxa");
    }

}
