package org.globalbioticinteractions.nomer.match;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eol.globi.domain.NameType;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.eol.globi.domain.NameType.HAS_ACCEPTED_NAME;
import static org.eol.globi.domain.NameType.NONE;
import static org.eol.globi.domain.NameType.SAME_AS;
import static org.eol.globi.domain.NameType.SYNONYM_OF;

public class PBDBTaxonService extends CommonLongTaxonService {
    private static final Logger LOG = LoggerFactory.getLogger(PBDBTaxonService.class);

    private static final String AUTHORS = "author";
    private BTreeMap<Long, String> refIds;


    public PBDBTaxonService(TermMatcherContext ctx) {
        super(ctx);
    }

    @Override
    public TaxonomyProvider getTaxonomyProvider() {
        return TaxonomyProvider.PBDB;
    }

    void parseNodes(Map<Long, Map<String, String>> taxonMap,
                    Map<Long, Long> childParent,
                    Map<String, List<Long>> name2nodeIds,
                    Map<Long, String> authorIds,
                    BTreeMap<Long, Long> mergedNodes,
                    InputStream is) throws PropertyEnricherException {
        try {
            LabeledCSVParser labeledTSVParser = CSVTSVUtil.createLabeledTSVParser(is);

            while (labeledTSVParser.getLine() != null) {


                String providedId = labeledTSVParser.getValueByLabel("taxon_no");
                String providedName = labeledTSVParser.getValueByLabel("taxon_name");
                String providedRank = labeledTSVParser.getValueByLabel("taxon_rank");
                String providedParentId = labeledTSVParser.getValueByLabel("parent_no");
                Taxon providedTaxon = new TaxonImpl(
                        providedName,
                        TaxonomyProvider.PBDB.getIdPrefix() + providedId
                );
                providedTaxon.setRank(providedRank);

                String acceptedId = labeledTSVParser.getValueByLabel("accepted_no");
                String acceptedName = labeledTSVParser.getValueByLabel("accepted_name");
                String acceptedRank = labeledTSVParser.getValueByLabel("accepted_rank");
                Taxon acceptedTaxon = new TaxonImpl(
                        acceptedName,
                        TaxonomyProvider.PBDB.getIdPrefix() + acceptedId
                );
                acceptedTaxon.setRank(acceptedRank);

                String different = labeledTSVParser.getValueByLabel("difference");

                Map<String, NameType> mapping = new TreeMap<String, NameType>() {{
                    put("corrected to", HAS_ACCEPTED_NAME);
                    put("invalid subgroup of", NONE);
                    put("misspelling of", HAS_ACCEPTED_NAME);
                    put("nomen dubium", SAME_AS);
                    put("nomen nudum", SAME_AS);
                    put("nomen oblitum", SAME_AS);
                    put("nomen vanum", SAME_AS);
                    put("objective synonym of", SYNONYM_OF);
                    put("obsolete variant of", HAS_ACCEPTED_NAME);
                    put("reassigned as", HAS_ACCEPTED_NAME);
                    put("recombined as", SAME_AS);
                    put("replaced by", HAS_ACCEPTED_NAME);
                    put("subjective synonym of", SYNONYM_OF);
                }};

                String authorship = authorIds.get(Long.parseLong(labeledTSVParser.getValueByLabel("reference_no")));

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

                    if (mapping.containsKey(different)) {
                        mergedNodes.put(Long.parseLong(providedId), Long.parseLong(acceptedId));
                    }
                }


            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse PBDB taxon dump", e);
        }
    }

    private static void parseReferences(Map<Long, String> refIdMap, InputStream resourceAsStream) throws PropertyEnricherException {
        try {
            LabeledCSVParser parser = CSVTSVUtil.createLabeledTSVParser(resourceAsStream);
            while (parser.getLine() != null) {
                String refId = parser.getValueByLabel("reference_no");
                String year = parser.getValueByLabel("pubyr");
                StringBuilder builder = new StringBuilder();
                appendIfNotBlank(parser, builder, "author1init", " ", "");
                appendIfNotBlank(parser, builder, "author1last", " ", "");
                appendIfNotBlank(parser, builder, "author2init", " ", "and ");
                appendIfNotBlank(parser, builder, "author2last", " ", "");

                refIdMap.put(Long.parseLong(refId), builder.toString() + year);
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse ITIS taxon unit types", e);
        }
    }

    private static String appendIfNotBlank(LabeledCSVParser parser, StringBuilder builder, String author1init, String suffix, String prefix) {
        String author1First = parser.getValueByLabel(author1init);
        if (StringUtils.isNotBlank(author1First)) {
            builder.append(prefix);
            builder.append(author1First);
            builder.append(suffix);
        }
        return author1First;
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
            refIds = db.getTreeMap(AUTHORS);
        } else {
            indexITIS(db);
        }
    }

    private void indexITIS(DB db) throws PropertyEnricherException {
        LOG.info("ITIS taxonomy importing...");
        StopWatch watch = new StopWatch();
        watch.start();

        refIds = db
                .createTreeMap(AUTHORS)
                .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                .valueSerializer(Serializer.STRING)
                .make();

        try {
            InputStream resource = getCtx().retrieve(getReferences());
            if (resource == null) {
                throw new PropertyEnricherException("init failure: failed to find [" + getReferences() + "]");
            }
            parseReferences(refIds, resource);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse references", e);
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
                    refIds,
                    mergedNodes, getCtx().retrieve(getNodesUrl())
            );
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
        return CacheUtil.getValueURI(getCtx(), "nomer.pbdb.taxa");
    }

    private URI getReferences() throws PropertyEnricherException {
        return CacheUtil.getValueURI(getCtx(), "nomer.pbdb.refs");
    }

}
