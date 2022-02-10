package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eol.globi.domain.NameType;
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
import org.mapdb.Fun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class CatalogueOfLifeTaxonService extends CommonTaxonService<String> {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogueOfLifeTaxonService.class);
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
    public String getIdOrNull(String key, TaxonomyProvider matchingTaxonomyProvider) {
        TaxonomyProvider taxonomyProvider = ExternalIdUtil.taxonomyProviderFor(key);
        String idString = ExternalIdUtil.stripPrefix(matchingTaxonomyProvider, key);
        return (matchingTaxonomyProvider.equals(taxonomyProvider)
                && StringUtils.isNoneBlank(idString))
                ? idString
                : null;
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
                && db.exists(MERGED_NODES)
        ) {
            LOG.info("[Catalogue of Life] taxonomy already indexed at [" + taxonomyDir.getAbsolutePath() + "], no need to import.");
            denormalizedNodes = db.getTreeMap(DENORMALIZED_NODES);
            denormalizedNodeIds = db.getTreeMap(DENORMALIZED_NODE_IDS);
            mergedNodes = db.getTreeMap(MERGED_NODES);
        } else {
            LOG.info("[" + getTaxonomyProvider().name() + "] taxonomy importing...");
            StopWatch watch = new StopWatch();
            watch.start();
            BTreeMap<String, Map<String, String>> nodes;
            BTreeMap<String, String> childParent;
            if (reverseSorted) {
                LOG.info("indexing taxon names...");
                nodes = populateNodes(db, watch);
                watch.reset();
                watch.start();
                LOG.info("indexing taxon hierarchies...");
                childParent = populateChildParent(db, watch);
                watch.reset();
                watch.start();
                LOG.info("indexing synonyms...");
                mergedNodes = populatedMergedNodes(db, watch);
                watch.reset();
                watch.start();
            } else {
                try (InputStream resource = getNameUsageStream()) {

                    nodes = db
                            .createTreeMap("nodes")
                            .keySerializer(BTreeKeySerializer.STRING)
                            .make();


                    childParent = db
                            .createTreeMap("childParent")
                            .keySerializer(BTreeKeySerializer.STRING)
                            .make();

                    mergedNodes = db
                            .createTreeMap(MERGED_NODES)
                            .keySerializer(BTreeKeySerializer.STRING)
                            .make();

                    NameUsageListener nameUsageListener = new NameUsageListenerImpl(mergedNodes, nodes, childParent);
                    parseNameUsage(resource, nameUsageListener);
                } catch (IOException e) {
                    throw new PropertyEnricherException("failed to parse taxon", e);
                }
            }

            denormalizedNodes = db
                    .createTreeMap(DENORMALIZED_NODES)
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();

            denormalizedNodeIds = db
                    .createTreeMap(DENORMALIZED_NODE_IDS)
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();


            LOG.info("building denormalized taxon lookups...");
            denormalizeTaxa(
                    nodes,
                    denormalizedNodes,
                    denormalizedNodeIds,
                    childParent);
            TaxonCacheService.logCacheLoadStats(watch.getTime(), nodes.size(), LOG);

            watch.stop();
            LOG.info("[" + getTaxonomyProvider().name() + "] taxonomy imported.");

        }
    }

    private BTreeMap<String, Map<String, String>> populateNodes(DB db, StopWatch watch) throws PropertyEnricherException {
        BTreeMap<String, Map<String, String>> nodes;
        InputStream is = getNameUsageStream();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is));

        try {
            skipFirstLine(reader);
            nodes = db
                    .createTreeMap("nodes")
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
                                    }, line);
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
                    .makeStringMap();
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
            skipFirstLine(reader);
            childParent = db
                    .createTreeMap("childParent")
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
                                    }, line);
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
                    .makeStringMap();
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
            skipFirstLine(reader);
            mergedNodes = db
                    .createTreeMap(MERGED_NODES)
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
                                    }, line);
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
                    .makeStringMap();
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

            String line;
            skipFirstLine(reader);
            while ((line = reader.readLine()) != null) {
                parseLine(nameUsageListener, line);
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse [Catalogue of Life] taxon dump", e);
        }
    }

    private void parseLine(NameUsageListener nameUsageListener, String line) {
        String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "\t");
        if (rowValues.length > 8) {
            String taxId = rowValues[0];
            String parentTaxId = rowValues[2];
            String status = rowValues[4];
            String completeName = rowValues[5];
            String authorship = rowValues[6];
            String rank = rowValues[7];

            String idPrefix = getTaxonomyProvider().getIdPrefix();
            TaxonImpl taxon = new TaxonImpl(completeName, idPrefix + taxId);
            if (StringUtils.isNoneBlank(authorship)) {
                taxon.setAuthorship(StringUtils.trim(authorship));
            }

            taxon.setRank(StringUtils.equals(StringUtils.trim(rank), "no rank") ? "" : rank);
            nameUsageListener.handle(status, taxId, parentTaxId, taxon);

        }
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

    private void skipFirstLine(BufferedReader reader) throws IOException {
        reader.readLine();
    }

    private URI getNameUsageResource() {
        String propertyValue = getCtx().getProperty("nomer.col.name_usage.url");
        return URI.create(propertyValue);
    }

    private class NameUsageListenerImpl implements NameUsageListener {
        private final Map<String, String> mergedNodes;
        private final Map<String, Map<String, String>> nodes;
        private final Map<String, String> childParent;

        public NameUsageListenerImpl(Map<String, String> mergedNodes, Map<String, Map<String, String>> nodes, Map<String, String> childParent) {
            this.mergedNodes = mergedNodes;
            this.nodes = nodes;
            this.childParent = childParent;
        }

        @Override
        public void handle(String status, String childTaxId, String parentTaxId, Taxon taxon) {
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
}
