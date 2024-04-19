package org.globalbioticinteractions.nomer.match;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.util.CSVTSVUtil;
import org.globalbioticinteractions.nomer.util.CacheUtil;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.mapdb.BTreeKeySerializer;
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
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IndexFungorumTaxonService extends CommonLongTaxonService {
    private static final Logger LOG = LoggerFactory.getLogger(IndexFungorumTaxonService.class);
    public static final String INDEXFUNGORUM_EXPORT_PROPERTY_NAME = "nomer.indexfungorum.export";

    public IndexFungorumTaxonService(TermMatcherContext ctx) {
        super(ctx);
    }

    @Override
    public TaxonomyProvider getTaxonomyProvider() {
        return TaxonomyProvider.INDEX_FUNGORUM;
    }

    private void parseNodes(Map<Long, Map<String, String>> nodes,
                                   Map<Long, Long> mergedNodes,
                                   InputStream resourceAsStream) throws PropertyEnricherException {

        if (resourceAsStream != null) {
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
                    taxon.setPath(StringUtils.join(new String[]{kingdomName, phylumName, subphylumName, className, subclassName, orderName, familyName, completeName}, CharsetConstant.SEPARATOR));
                    String[] ranks = {"kingdom", "phylum", "subphylum", "class", "subclass", "order", "family", ""};
                    taxon.setPathNames(StringUtils.join(ranks, CharsetConstant.SEPARATOR));
                    taxon.setPathAuthorships(Stream.of(ranks).map(r -> "").collect(Collectors.joining(CharsetConstant.SEPARATOR)));
                    if (NumberUtils.isCreatable(taxId)) {
                        Long taxonKey = Long.parseLong(taxId);
                        registerIdForName(taxonKey, taxon, name2nodeIds);
                        nodes.put(taxonKey, TaxonUtil.taxonToMap(taxon));
                        if (NumberUtils.isCreatable(acceptedTaxId)) {
                            mergedNodes.put(
                                    taxonKey,
                                    Long.parseLong(acceptedTaxId)
                            );
                        }
                    }
                }
            } catch (IOException e) {
                throw new PropertyEnricherException("failed to parse taxon dump", e);
            }
        }
    }


    @Override
    protected void lazyInit() throws PropertyEnricherException {
        File taxonomyDir = new File(getCacheDir(), getProviderShortName());
        DB db = DBMaker
                .newFileDB(taxonomyDir)
                .mmapFileEnableIfSupported()
                .mmapFileCleanerHackDisable()
                .compressionEnable()
                .closeOnJvmShutdown()
                .transactionDisable()
                .make();

        if (db.exists(NODES)
                && db.exists(NODES)
                && db.exists(MERGED_NODES)) {
            LOG.debug("[" + getProviderShortName() + "] taxonomy already indexed at [" + taxonomyDir.getAbsolutePath() + "], no need to import.");
            nodes = db.getTreeMap(NODES);
            childParent = db.getTreeMap(CHILD_PARENT);
            mergedNodes = db.getTreeMap(MERGED_NODES);
            name2nodeIds = db.getTreeMap(NAME_TO_NODE_IDS);
        } else {
            index(db);
        }
    }

    private void index(DB db) throws PropertyEnricherException {
        LOG.info("[" + getProviderShortName() + "] taxonomy importing...");
        StopWatch watch = new StopWatch();
        watch.start();


        nodes = db
                .createTreeMap(NODES)
                .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                .valueSerializer(Serializer.JAVA)
                .make();

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
            InputStream resource = getCtx().retrieve(getNodesUrl());
            if (resource == null) {
                throw new PropertyEnricherException("no discoverlife resource found at [" + getNodesUrl() + "], please configure property [" + INDEXFUNGORUM_EXPORT_PROPERTY_NAME + "]");
            }
            parseNodes(nodes, mergedNodes, resource);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse ITIS nodes", e);
        }

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

    private URI getNodesUrl() throws PropertyEnricherException {
        return CacheUtil.getValueURI(getCtx(), INDEXFUNGORUM_EXPORT_PROPERTY_NAME);
    }

}
