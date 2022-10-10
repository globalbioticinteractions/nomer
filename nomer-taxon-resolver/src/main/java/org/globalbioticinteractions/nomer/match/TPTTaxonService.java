package org.globalbioticinteractions.nomer.match;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Triple;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.TaxonCacheService;
import org.globalbioticinteractions.nomer.util.CacheUtil;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.mapdb.BTreeKeySerializer;
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

public class TPTTaxonService extends CommonLongTaxonService {
    private static final Logger LOG = LoggerFactory.getLogger(TPTTaxonService.class);

    public TPTTaxonService(TermMatcherContext ctx) {
        super(ctx);
    }

    @Override
    public TaxonomyProvider getTaxonomyProvider() {
        return TaxonomyProvider.GBIF;
    }

    void parseNodes(Map<Long, Map<String, String>> taxonMap,
                    Map<Long, Long> mergedNodes,
                    Map<String, List<Long>> name2nodeIds,
                    InputStream is) throws PropertyEnricherException {

        try {
            LabeledCSVParser parser = new LabeledCSVParser(new CSVParser(is));


            while (parser.getLine() != null) {
                Triple<Taxon, NameType, Taxon> nameRelation
                        = TabularTaxonUtil.parseNameRelations(parser);


                Taxon taxon = nameRelation.getLeft();
                long taxonId = normalizeTaxonID(taxon);
                taxonMap.put(taxonId, TaxonUtil.taxonToMap(taxon));
                registerIdForName(
                        taxonId, taxon, name2nodeIds);

                if (!NameType.HAS_ACCEPTED_NAME.equals(nameRelation.getMiddle())) {
                    long parentId = normalizeTaxonID(nameRelation.getRight());
                    mergedNodes.put(taxonId, parentId);
                }

            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse", e);
        }
    }

    public static long normalizeTaxonID(Taxon taxon) {
        return Long.parseLong(
                RegExUtils.replaceAll(
                        taxon.getExternalId(),
                        "(GBIF:){0,1}(TPT:){0,1}([Aa]cari_)",
                        ""
                )
        );
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
                .mmapFileCleanerHackDisable()
                .compressionEnable()
                .closeOnJvmShutdown()
                .transactionDisable()
                .make();

        if (db.exists(NODES)
                && db.exists(MERGED_NODES)
                //&& db.exists(CHILD_PARENT)
                && db.exists(NAME_TO_NODE_IDS)) {
            LOG.info("DwC taxonomy already indexed at [" + taxonomyDir.getAbsolutePath() + "], no need to import.");
            nodes = db.getTreeMap(NODES);
            //childParent = db.getTreeMap(CHILD_PARENT);
            mergedNodes = db.getTreeMap(MERGED_NODES);
            name2nodeIds = db.getTreeMap(NAME_TO_NODE_IDS);
        } else {
            index(db);
        }
    }

    private void index(DB db) throws PropertyEnricherException {
        LOG.info("DWC taxonomy importing...");
        StopWatch watch = new StopWatch();
        watch.start();

        nodes = db
                .createTreeMap(NODES)
                .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                .valueSerializer(Serializer.JAVA)
                .make();

//        childParent = db
//                .createTreeMap(CHILD_PARENT)
//                .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
//                .valueSerializer(Serializer.LONG)
//                .make();

        mergedNodes = db
                .createTreeMap(MERGED_NODES)
                .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                .valueSerializer(Serializer.LONG)
                .make();

        name2nodeIds = db
                .createTreeMap(NAME_TO_NODE_IDS)
                .keySerializer(BTreeKeySerializer.STRING)
                .valueSerializer(Serializer.JAVA)
                .make();

        try {
            parseNodes(nodes, mergedNodes, name2nodeIds, getCtx().retrieve(getNodesUrl()));
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
        return CacheUtil.getValueURI(getCtx(), "nomer.tpt.taxon");
    }

}
