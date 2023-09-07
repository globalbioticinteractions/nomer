package org.globalbioticinteractions.nomer.match;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.TaxonCacheService;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CatalogueOfLifeTaxonService extends CommonStringTaxonService {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogueOfLifeTaxonService.class);
    private static final String DATASET_KEY = "datasetKey";
    private static final String LABEL_TAXON_ID = "col:ID";
    private static final String LABEL_TAXON_ID_PARENT = "col:parentID";
    private static final String LABEL_TAXON_STATUS_NAME = "col:status";
    private static final String LABEL_TAXON_SCIENTIFIC_NAME = "col:scientificName";
    private static final String LABEL_TAXON_AUTHORSHIP = "col:authorship";
    private static final String LABEL_TAXON_RANK_NAME = "col:rank";

    private boolean reverseSorted;


    public CatalogueOfLifeTaxonService(TermMatcherContext ctx) {
        super(ctx);
        reverseSorted = ctx != null && (StringUtils.equalsIgnoreCase("true", ctx.getProperty("nomer.col.name_usage.reverse_sorted")));
    }

    @Override
    public TaxonomyProvider getTaxonomyProvider() {
        return TaxonomyProvider.CATALOGUE_OF_LIFE;
    }

    @Override
    protected void lazyInit() throws PropertyEnricherException {
        File cacheDir = getCacheDir();

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
                && db.exists(CHILD_PARENT)
                && db.exists(MERGED_NODES)
                && db.exists(NAME_TO_NODE_IDS)
        ) {
            LOG.debug("[Catalogue of Life] taxonomy already indexed at [" + taxonomyDir.getAbsolutePath() + "], no need to import.");
            nodes = db.getTreeMap(NODES);
            childParent = db.getTreeMap(CHILD_PARENT);
            mergedNodes = db.getTreeMap(MERGED_NODES);
            name2nodeIds = db.getTreeMap(NAME_TO_NODE_IDS);
            if (db.exists(DATASET_KEY)) {
                datasetKey = db.getAtomicLong(DATASET_KEY);
            } else {
                datasetKey = null;
            }
        } else {
            LOG.info("[" + getTaxonomyProvider().name() + "] taxonomy importing...");
            StopWatch watch = new StopWatch();
            watch.start();
            indexDatasetKey(db);

            if (reverseSorted) {
                LOG.info("indexing taxon names...");
                nodes = populateNodes(db, watch);
                watch.reset();
                watch.start();
                LOG.info("indexing taxon hierarchies...");
                childParent = populateChildParent(db, watch);
                watch.reset();
                watch.start();
                LOG.info("indexing names to node ids...");
                populateName2NodeIds(db, watch);
                watch.reset();
                watch.start();

                LOG.info("indexing synonyms...");
                mergedNodes = populatedMergedNodes(db, watch);
                watch.reset();
                watch.start();
            } else {

                try (InputStream resource = getNameUsageStream()) {
                    nodes = db
                            .createTreeMap(NODES)
                            .keySerializer(BTreeKeySerializer.STRING)
                            .valueSerializer(Serializer.JAVA)
                            .make();

                    childParent = db
                            .createTreeMap(CHILD_PARENT)
                            .keySerializer(BTreeKeySerializer.STRING)
                            .valueSerializer(Serializer.STRING)
                            .make();

                    name2nodeIds = db
                            .createTreeMap(NAME_TO_NODE_IDS)
                            .keySerializer(BTreeKeySerializer.STRING)
                            .valueSerializer(Serializer.JAVA)
                            .make();

                    mergedNodes = db
                            .createTreeMap(MERGED_NODES)
                            .keySerializer(BTreeKeySerializer.STRING)
                            .valueSerializer(Serializer.STRING)
                            .make();

                    NameUsageListener nameUsageListener = new NameUsageListenerImpl(
                            mergedNodes,
                            nodes,
                            childParent
                    );
                    parseNameUsage(resource, nameUsageListener);
                } catch (IOException e) {
                    throw new PropertyEnricherException("failed to index [" + getNameUsageResource() + "]", e);
                }
            }

            TaxonCacheService.logCacheLoadStats(watch.getTime(), nodes.size(), LOG);

            watch.stop();
            LOG.info("[" + getTaxonomyProvider().name() + "] taxonomy imported.");

        }
    }

    private void populateName2NodeIds(DB db, StopWatch watch) throws PropertyEnricherException {
        name2nodeIds = db
                .createTreeMap(NAME_TO_NODE_IDS)
                .keySerializer(BTreeKeySerializer.STRING)
                .valueSerializer(Serializer.JAVA)
                .make();
        NameUsageListener nameUsageListenerName2Id = new NameUsageListenerName2Id();
        try (InputStream resource = getNameUsageStream()) {
            parseNameUsage(resource, nameUsageListenerName2Id);
        } catch(IOException ex) {
            throw new PropertyEnricherException("failed to index [" + getNameUsageResource() + "]", ex);
        }
        TaxonCacheService.logCacheLoadStats(watch.getTime(), name2nodeIds.size(), LOG);
    }

    private void indexDatasetKey(DB db) throws PropertyEnricherException {
        String propertyValue = getCtx().getProperty("nomer.col.metadata.url");
        URI metadata = URI.create(propertyValue);
        final Pattern compile = Pattern.compile("^key:[ ]+(?<datasetKey>[0-9]+)$");
        try (InputStream resource = getCtx().retrieve(metadata)) {
            BufferedReader reader = IOUtils.toBufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8));
            Long key = reader
                    .lines()
                    .filter(line -> compile.matcher(line).matches())
                    .findFirst()
                    .map(line -> {
                        Matcher matcher = compile.matcher(line);
                        matcher.matches();
                        return Long.parseLong(matcher.group("datasetKey"));
                    }).orElseThrow(new Supplier<Throwable>() {
                        @Override
                        public Throwable get() {
                            return new PropertyEnricherException("failed to locate dataset key in [" + propertyValue + "]");
                        }
                    });
            datasetKey = db
                    .createAtomicLong(DATASET_KEY, -1L);
            datasetKey.set(key);
        } catch (Throwable e) {
            throw new PropertyEnricherException("failed to read metadata at [" + metadata + "]", e);
        }
    }

    private BTreeMap<String, Map<String, String>> populateNodes(DB db, StopWatch watch) throws PropertyEnricherException {
        BTreeMap<String, Map<String, String>> nodes;
        InputStream is = getNameUsageStream();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is));

        try {
            final Map<String, Integer> schema = parseFirstLine(reader);
            nodes = db
                    .createTreeMap(NODES)
                    .keySerializer(BTreeKeySerializer.STRING)
                    .valueSerializer(Serializer.JAVA)
                    .pumpSource(new Iterator<Fun.Tuple2<String, Map<String, String>>>() {
                        String taxonStatus;
                        String taxonChildId;
                        String taxonParentId;
                        Taxon taxonObj;

                        @Override
                        public boolean hasNext() {
                            AtomicBoolean hasNext = new AtomicBoolean(false);
                            try {
                                String line = reader.readLine();
                                if (StringUtils.isNoneBlank(line)) {
                                    parseLine(new NameUsageListener() {
                                        @Override
                                        public void handle(String status, String childTaxId, String parentTaxId, Taxon taxon) {
                                            taxonStatus = status;
                                            taxonParentId = parentTaxId;
                                            taxonChildId = childTaxId;
                                            taxonObj = taxon;
                                            hasNext.set(true);
                                        }
                                    }, line, schema);
                                }
                            } catch (IOException e) {
                                // ignore
                            }

                            return hasNext.get();
                        }

                        @Override
                        public Fun.Tuple2<String, Map<String, String>> next() {
                            return new Fun.Tuple2<>(
                                    taxonChildId,
                                    TaxonUtil.taxonToMap(taxonObj)
                            );
                        }
                    })
                    .make();
            TaxonCacheService.logCacheLoadStats(watch.getTime(), nodes.size(), LOG);
            return nodes;
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to skip first line", e);
        }
    }


    private BTreeMap<String, String> populateChildParent(DB db, StopWatch watch) throws PropertyEnricherException {
        BTreeMap<String, String> childParent;
        InputStream is = getNameUsageStream();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is));

        try {
            final Map<String, Integer> schema = parseFirstLine(reader);
            childParent = db
                    .createTreeMap(CHILD_PARENT)
                    .keySerializer(BTreeKeySerializer.STRING)
                    .valueSerializer(Serializer.STRING)
                    .pumpSource(new Iterator<Fun.Tuple2<String, String>>() {
                        String taxonChildId;
                        String taxonParentId;

                        @Override
                        public boolean hasNext() {
                            AtomicBoolean hasNext = new AtomicBoolean(false);
                            try {
                                String line = reader.readLine();
                                if (StringUtils.isNoneBlank(line)) {
                                    parseLine(new NameUsageListener() {
                                        @Override
                                        public void handle(String status, String childTaxId, String parentTaxId, Taxon taxon) {
                                            taxonParentId = parentTaxId;
                                            taxonChildId = childTaxId;
                                            hasNext.set(true);
                                        }
                                    }, line, schema);
                                }
                            } catch (IOException e) {
                                // ignore
                            }

                            return hasNext.get();
                        }

                        @Override
                        public Fun.Tuple2<String, String> next() {
                            return new Fun.Tuple2<>(
                                    taxonChildId,
                                    taxonParentId
                            );
                        }
                    })
                    .make();
            TaxonCacheService.logCacheLoadStats(watch.getTime(), childParent.size(), LOG);
            return childParent;
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to skip first line", e);
        }
    }

    private BTreeMap<String, String> populatedMergedNodes(DB db, StopWatch watch) throws PropertyEnricherException {
        BTreeMap<String, String> mergedNodes;
        InputStream is = getNameUsageStream();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is));

        try {
            final Map<String, Integer> schema = parseFirstLine(reader);
            mergedNodes = db
                    .createTreeMap(MERGED_NODES)
                    .keySerializer(BTreeKeySerializer.STRING)
                    .valueSerializer(Serializer.STRING)
                    .pumpSource(new Iterator<Fun.Tuple2<String, String>>() {
                        String taxonParentId;
                        String taxonChildId;

                        @Override
                        public boolean hasNext() {
                            AtomicBoolean hasNext = new AtomicBoolean(false);
                            try {
                                String line;
                                while (StringUtils.isNoneBlank(line = reader.readLine())) {
                                    parseLine(new NameUsageListener() {
                                        @Override
                                        public void handle(String status, String childTaxId, String parentTaxId, Taxon taxon) {
                                            if (StringUtils.contains(status, "synonym")) {
                                                taxonParentId = parentTaxId;
                                                taxonChildId = childTaxId;
                                                hasNext.set(true);
                                            }
                                        }
                                    }, line, schema);
                                    if (hasNext.get()) {
                                        break;
                                    }
                                }
                            } catch (IOException e) {
                                // ignore
                            }

                            return hasNext.get();
                        }

                        @Override
                        public Fun.Tuple2<String, String> next() {
                            return new Fun.Tuple2<>(
                                    taxonChildId,
                                    taxonParentId
                            );
                        }
                    })
                    .make();
            TaxonCacheService.logCacheLoadStats(watch.getTime(), mergedNodes.size(), LOG);
            return mergedNodes;
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to skip first line", e);
        }
    }

    private InputStream getNameUsageStream() throws PropertyEnricherException {
        InputStream resource;
        try {
            resource = getCtx().retrieve(getNameUsageResource());
            if (resource == null) {
                throw new PropertyEnricherException("Catalogue of Life init failure: failed to find [" + getNameUsageResource() + "]");
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse [" + getTaxonomyProvider().name() + "]", e);
        }
        return resource;
    }

    private void parseNameUsage(InputStream resource, NameUsageListener nameUsageListener) throws PropertyEnricherException {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource));

            final Map<String, Integer> schema = parseFirstLine(reader);

            String line;
            while ((line = reader.readLine()) != null) {
                parseLine(nameUsageListener, line, schema);
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse [Catalogue of Life] taxon dump", e);
        }
    }

    private void parseLine(NameUsageListener nameUsageListener, String line, Map<String, Integer> schema) {
        String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "\t");
        if (rowValues.length > 8) {

            String taxId = prefixIdWithDatasetKey(rowValues[schema.get(LABEL_TAXON_ID)]);
            String parentTaxId = prefixIdWithDatasetKey(rowValues[schema.get(LABEL_TAXON_ID_PARENT)]);
            String status = rowValues[schema.get(LABEL_TAXON_STATUS_NAME)];
            String completeName = RegExUtils.replaceAll(rowValues[schema.get(LABEL_TAXON_SCIENTIFIC_NAME)], "[ ]+\\(.*\\)[ ]+", " ");
            String authorship = rowValues[schema.get(LABEL_TAXON_AUTHORSHIP)];
            String rank = rowValues[schema.get(LABEL_TAXON_RANK_NAME)];

            String idPrefix = getIdPrefix();
            TaxonImpl taxon = new TaxonImpl(completeName, idPrefix + taxId);
            if (StringUtils.isNoneBlank(authorship)) {
                taxon.setAuthorship(StringUtils.trim(authorship));
            }

            taxon.setRank(StringUtils.equals(StringUtils.trim(rank), "no rank") ? "" : rank);
            nameUsageListener.handle(status, taxId, parentTaxId, taxon);

        }
    }

    private String prefixIdWithDatasetKey(String rowValue) {
        return StringUtils.isBlank(rowValue) || datasetKey == null
                ? rowValue
                : datasetKey.get() + ":" + rowValue;
    }


    public void setReverseSorted(boolean reverseSorted) {
        this.reverseSorted = reverseSorted;
    }

    interface NameUsageListener {
        void handle(String status, String childTaxId, String parentTaxId, Taxon taxon);
    }

    private NameType getNameType(String statusValue) {
        return StringUtils.contains(statusValue, "synonym")
                ? NameType.SYNONYM_OF
                : NameType.HAS_ACCEPTED_NAME;
    }

    private Map<String, Integer> parseFirstLine(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        List<String> headers = Arrays.asList(StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "\t"));

        Map<String, Integer> schema = new TreeMap<>();
        List<String> headerLabels = Arrays.asList(
                LABEL_TAXON_ID,
                LABEL_TAXON_ID_PARENT,
                LABEL_TAXON_SCIENTIFIC_NAME,
                LABEL_TAXON_RANK_NAME,
                LABEL_TAXON_STATUS_NAME,
                LABEL_TAXON_AUTHORSHIP
        );
        headerLabels.forEach(label -> mapHeader(headers, schema, label));

        Collection<String> missingHeaders = CollectionUtils.disjunction(schema.keySet(), headerLabels);
        if (missingHeaders.size() > 0) {
            throw new IOException("missing headers [" + StringUtils.join(missingHeaders) + "] in header [" + line + "]");
        }

        return Collections.unmodifiableMap(schema);
    }

    private void mapHeader(List<String> headers, Map<String, Integer> schema, String labelTaxonRankName) {
        int index = headers.indexOf(labelTaxonRankName);
        if (index >= 0) {
            schema.put(labelTaxonRankName, index);
        }
    }

    private URI getNameUsageResource() {
        String propertyValue = getCtx().getProperty("nomer.col.name_usage.url");
        return URI.create(propertyValue);
    }

    private class NameUsageListenerImpl implements NameUsageListener {
        private final Map<String, String> mergedNodes;
        private final Map<String, Map<String, String>> nodes;
        private final Map<String, String> childParent;
        private final NameUsageListener name2nodeIdListener;

        public NameUsageListenerImpl(Map<String, String> mergedNodes,
                                     Map<String, Map<String, String>> nodes,
                                     Map<String, String> childParent) {
            this.mergedNodes = mergedNodes;
            this.nodes = nodes;
            this.childParent = childParent;
            this.name2nodeIdListener = new NameUsageListenerName2Id();
        }

        @Override
        public void handle(String status, String childTaxId, String parentTaxId, Taxon taxon) {
            name2nodeIdListener.handle(status, childTaxId, parentTaxId, taxon);

            NameType nameType = getNameType(status);
            if (NameType.SYNONYM_OF.equals(nameType)) {
                mergedNodes.put(childTaxId, parentTaxId);
            }
            if (StringUtils.isNoneBlank(childTaxId)) {
                nodes.put(childTaxId, TaxonUtil.taxonToMap(taxon));
                if (StringUtils.isNoneBlank(parentTaxId)) {
                    childParent.put(
                            childTaxId,
                            parentTaxId
                    );
                }
            }

        }

    }

    public class NameUsageListenerName2Id implements NameUsageListener {

        @Override
        public void handle(String status, String childTaxId, String parentTaxId, Taxon taxon) {
            registerIdForName(childTaxId, taxon, CatalogueOfLifeTaxonService.this.name2nodeIds);
        }
    }
}
