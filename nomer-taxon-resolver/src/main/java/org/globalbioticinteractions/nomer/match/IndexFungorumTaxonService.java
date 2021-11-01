package org.globalbioticinteractions.nomer.match;

import com.Ostermiller.util.LabeledCSVParser;
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
import org.eol.globi.util.CSVTSVUtil;
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
import java.util.List;
import java.util.Map;

public class IndexFungorumTaxonService extends CommonTaxonService {
    private static final Logger LOG = LoggerFactory.getLogger(IndexFungorumTaxonService.class);

    public IndexFungorumTaxonService(TermMatcherContext ctx) {
        super(ctx);
    }

    @Override
    public TaxonomyProvider getTaxonomyProvider() {
        return TaxonomyProvider.INDEX_FUNGORUM;
    }

    static void parseNodes(Map<Long, Map<String, String>> taxonMap,
                           Map<Long, Long> acceptedNameMap,
                           InputStream resourceAsStream) throws PropertyEnricherException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));

        try {
            LabeledCSVParser labeledCSVParser = CSVTSVUtil.createLabeledCSVParser(reader);
            while (labeledCSVParser.getLine() != null) {
                String taxId = labeledCSVParser.getValueByLabel("RECORD NUMBER");
                String acceptedTaxId = labeledCSVParser.getValueByLabel("CURRENT NAME RECORD NUMBER");
                String author = labeledCSVParser.getValueByLabel("AUTHORS");
                String year = labeledCSVParser.getValueByLabel("YEAR OF PUBLICATION");



                String familyName = labeledCSVParser.getValueByLabel("Family name");
                String orderName = labeledCSVParser.getValueByLabel("Order name");
                String subclassName = labeledCSVParser.getValueByLabel("Subclass name");
                String className = labeledCSVParser.getValueByLabel("Class name");
                String subphylumName = labeledCSVParser.getValueByLabel("Subphylum name");
                String phylumName = labeledCSVParser.getValueByLabel("Phylum name");
                String kingdomName = labeledCSVParser.getValueByLabel("Kingdom name");

                String completeName = labeledCSVParser.getValueByLabel("NAME OF FUNGUS");

                TaxonImpl taxon = new TaxonImpl(completeName, TaxonomyProvider.INDEX_FUNGORUM.getIdPrefix() + taxId);

                if (StringUtils.isNoneBlank(author) && StringUtils.isNoneBlank(year)) {
                    String authorship = StringUtils.join(new String[]{author, year}, ", ");
                    taxon.setAuthorship(authorship);
                }
                taxon.setPath(StringUtils.join(new String[] { kingdomName, phylumName, subphylumName, className, subclassName, orderName, familyName}, CharsetConstant.SEPARATOR));
                taxon.setPathNames(StringUtils.join(new String[] { "kingdom", "phylum", "subphylum", "class", "subclass", "order", "family"}, CharsetConstant.SEPARATOR));
                if (NumberUtils.isCreatable(taxId)) {
                    taxonMap.put(Long.parseLong(taxId), TaxonUtil.taxonToMap(taxon));
                    if (NumberUtils.isCreatable(acceptedTaxId)) {
                        acceptedNameMap.put(
                                Long.parseLong(taxId),
                                Long.parseLong(acceptedTaxId)
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

        File taxonomyDir = new File(cacheDir, getProviderShortName());
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
            LOG.info("[" + getProviderShortName() + "] taxonomy already indexed at [" + taxonomyDir.getAbsolutePath() + "], no need to import.");
            denormalizedNodes = db.getTreeMap(DENORMALIZED_NODES);
            denormalizedNodeIds = db.getTreeMap(DENORMALIZED_NODE_IDS);
            mergedNodes = db.getTreeMap(MERGED_NODES);
        } else {
            index(db);
        }
    }

    private void index(DB db) throws PropertyEnricherException {
        LOG.info("[" + getProviderShortName() + "] taxonomy importing...");
        StopWatch watch = new StopWatch();
        watch.start();


        BTreeMap<Long, Map<String, String>> nodes = db
                .createTreeMap("nodes")
                .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                .make();

        mergedNodes = db
                .createTreeMap(MERGED_NODES)
                .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                .make();

        try {
            parseNodes(nodes, mergedNodes, getCtx().getResource(getNodesUrl()));
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

        nodes.forEach((id, record) -> {
            Taxon taxon = TaxonUtil.mapToTaxon(record);
            List<Map<String, String>> taxa = denormalizedNodes.getOrDefault(taxon.getName(), new ArrayList<>());
            taxa.add(record);
            denormalizedNodes.put(taxon.getName(), taxa);

            taxa = denormalizedNodeIds.getOrDefault(id, new ArrayList<>());
            taxa.add(record);
            denormalizedNodeIds.put(id, taxa);
        });

        watch.stop();
        TaxonCacheService.logCacheLoadStats(watch.getTime(), nodes.size(), LOG);
        LOG.info("[" + getProviderShortName() + "] taxonomy imported.");
    }

    private String getProviderShortName() {
        return StringUtils.lowerCase(getTaxonomyProvider().name());
    }

    @Override
    public void shutdown() {

    }

    private String getNodesUrl() throws PropertyEnricherException {
        return getCtx().getProperty("nomer.indexfungorum.export");
    }

}
