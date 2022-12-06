package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.GBIFUtil;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.util.CSVTSVUtil;
import org.globalbioticinteractions.nomer.util.CacheUtil;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.eol.globi.taxon.GBIFUtil.compileAuthorshipString;

public class GBIFTaxonService extends CommonLongTaxonService {

    private static final Logger LOG = LoggerFactory.getLogger(GBIFTaxonService.class);

    public GBIFTaxonService(TermMatcherContext ctx) {
        super(ctx);
    }

    @Override
    public TaxonomyProvider getTaxonomyProvider() {
        return TaxonomyProvider.GBIF;
    }

    @Override
    protected void lazyInit() throws PropertyEnricherException {
        File taxonomyDir = new File(getCacheDir(), StringUtils.lowerCase(getTaxonomyProvider().name()));
        DB db = DBMaker
                .newFileDB(taxonomyDir)
                .mmapFileEnableIfSupported()
                .mmapFileCleanerHackDisable()
                .closeOnJvmShutdown()
                .transactionDisable()
                .make();

        if (db.exists(NODES)
                && db.exists(CHILD_PARENT)
                && db.exists(MERGED_NODES)
                && db.exists(NAME_TO_NODE_IDS)
        ) {
            LOG.debug("[" + getTaxonomyProvider().name() + "] taxonomy already indexed at [" + taxonomyDir.getAbsolutePath() + "], no need to import.");
            nodes = db.getTreeMap(NODES);
            childParent = db.getTreeMap(CHILD_PARENT);
            mergedNodes = db.getTreeMap(MERGED_NODES);
            name2nodeIds = db.getTreeMap(NAME_TO_NODE_IDS);
        } else {
            LOG.info("[" + getTaxonomyProvider().name() + "] taxonomy importing...");
            StopWatch watch = new StopWatch();


            URI gbifNameResource = CacheUtil.getValueURI(getCtx(), "nomer.gbif.ids");

            watch.start();

            LOG.info("indexing taxon ids...");
            buildTaxonIndex(db, gbifNameResource);
            LOG.info("indexing taxon ids...done.");

            LOG.info("indexing taxon hierarchies...");
            childParent = buildRelationIndex(db, gbifNameResource, CHILD_PARENT, new ChildParentRelationParser());
            LOG.info("indexing taxon hierarchies...done.");

            LOG.info("indexing synonyms...");
            mergedNodes = buildRelationIndex(db, gbifNameResource, MERGED_NODES, new SynonymParser());
            LOG.info("indexing synonyms...done.");

            LOG.info("indexing names...");
            buildNameToIdIndex(db, gbifNameResource);
            LOG.info("indexing names...done.");

            watch.stop();
            TaxonCacheService.logCacheLoadStats(watch.getTime(), nodes.size(), LOG);
            LOG.info("[" + getTaxonomyProvider().name() + "] taxonomy imported.");
        }

    }

    private void buildNameToIdIndex(DB db, URI gbifNameResource) throws PropertyEnricherException {
        name2nodeIds = db
                .createTreeMap(NAME_TO_NODE_IDS)
                .keySerializer(BTreeKeySerializer.STRING)
                .valueSerializer(Serializer.JAVA)
                .make();
        handleResource(gbifNameResource, new LineHandler() {
            @Override
            public void handle(String[] rowValues, String taxIdString, long taxId, String relatedTaxIdString, String rank, String canonicalName) {
                mapNameToIds(taxId, canonicalName);
            }
        });
    }

    private void buildSynonymIndex(DB db, URI gbifNameResource) throws PropertyEnricherException {
        final BufferedReader reader;
        try {
            reader = getBufferedReader(gbifNameResource);


            mergedNodes = db
                    .createTreeMap(MERGED_NODES)
                    .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                    .valueSerializer(Serializer.LONG)
                    .pumpSource(new Iterator<Fun.Tuple2<Long, Long>>() {
                        private Fun.Tuple2<Long, Long> entry;

                        @Override
                        public boolean hasNext() {

                            if (entry == null) {
                                try {
                                    parseNext();
                                } catch (IOException e) {
                                    return false;
                                }
                            }
                            return entry != null;
                        }

                        private void parseNext() throws IOException {
                            String line;
                            while (entry == null && (line = reader.readLine()) != null) {
                                handleLine(line, (rowValues, taxIdString, taxId, relatedTaxIdString, rank, canonicalName) -> {

                                    entry = parseSynonymRelation(rowValues, relatedTaxIdString, taxId);
                                });
                            }
                        }

                        @Override
                        public Fun.Tuple2<Long, Long> next() {
                            Fun.Tuple2<Long, Long> currentEntry = this.entry;
                            if (currentEntry != null) {
                                this.entry = null;
                            }
                            return currentEntry;
                        }
                    })
                    .make();
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to retrieve [" + gbifNameResource + "]", e);
        }

    }

    private BTreeMap<Long, Long> buildRelationIndex(
            DB db,
            URI gbifNameResource,
            String indexName,
            RelationParser relationParser) throws PropertyEnricherException {

        final BufferedReader reader;
        try {
            reader = getBufferedReader(gbifNameResource);
            return db
                    .createTreeMap(indexName)
                    .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                    .valueSerializer(Serializer.LONG)
                    .pumpSource(new Iterator<Fun.Tuple2<Long, Long>>() {
                        private Fun.Tuple2<Long, Long> entry;

                        @Override
                        public boolean hasNext() {

                            if (entry == null) {
                                try {
                                    parseNext();
                                } catch (IOException e) {
                                    return false;
                                }
                            }
                            return entry != null;
                        }

                        private void parseNext() throws IOException {
                            String line;
                            while (entry == null && (line = reader.readLine()) != null) {
                                handleLine(line, (rowValues, taxIdString, taxId, relatedTaxIdString, rank, canonicalName) -> {
                                    entry = parseChildParentRelation(rowValues, relatedTaxIdString, taxId, relationParser);
                                });
                            }
                        }

                        @Override
                        public Fun.Tuple2<Long, Long> next() {
                            Fun.Tuple2<Long, Long> currentEntry = this.entry;
                            if (currentEntry != null) {
                                this.entry = null;
                            }
                            return currentEntry;
                        }
                    })
                    .make();
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to retrieve [" + gbifNameResource + "]", e);
        }
    }

    private void buildTaxonIndex(DB db, URI gbifNameResource) throws PropertyEnricherException {

        try {
            final BufferedReader reader = getBufferedReader(gbifNameResource);

            nodes = db
                    .createTreeMap(NODES)
                    .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                    .valueSerializer(Serializer.JAVA)
                    .pumpSource(new Iterator<Fun.Tuple2<Long, Map<String, String>>>() {
                        private Fun.Tuple2<Long, Map<String, String>> entry;

                        @Override
                        public boolean hasNext() {

                            if (entry == null) {
                                try {
                                    parseNext();
                                } catch (IOException e) {
                                    return false;
                                }
                            }

                            return entry != null;
                        }

                        private void parseNext() throws IOException {
                            String line = reader.readLine();
                            handleLine(line, (rowValues, taxIdString, taxId, relatedTaxIdString, rank, canonicalName) -> {
                                Taxon taxon = parseTaxon(rowValues, taxIdString, rank, canonicalName);
                                entry = new Fun.Tuple2<>(taxId, TaxonUtil.taxonToMap(taxon));
                            });
                        }

                        @Override
                        public Fun.Tuple2<Long, Map<String, String>> next() {
                            Fun.Tuple2<Long, Map<String, String>> currentEntry = this.entry;
                            if (currentEntry != null) {
                                this.entry = null;
                            }
                            return currentEntry;
                        }
                    })
                    .make();


        } catch (IOException e) {
            throw new PropertyEnricherException("failed to retrieve [" + gbifNameResource + "]");
        }


        handleResource(gbifNameResource, new LineHandler() {
            @Override
            public void handle(String[] rowValues, String taxIdString, long taxId, String relatedTaxIdString, String rank, String canonicalName) {
                putTaxonById(rowValues, taxIdString, taxId, rank, canonicalName);
            }
        });
    }

    private BufferedReader getBufferedReader(URI gbifNameResource) throws IOException, PropertyEnricherException {
        InputStream retrieve;
        retrieve = getCtx().retrieve(gbifNameResource);
        if (retrieve == null) {
            throw new PropertyEnricherException("failed to retrieve [" + gbifNameResource + "]");
        }

        return new BufferedReader(
                new InputStreamReader(
                        retrieve,
                        StandardCharsets.UTF_8
                )
        );
    }

    private void handleResource(URI gbifNameResource, LineHandler lineHandler) throws PropertyEnricherException {
        try {
            BufferedReader reader = getBufferedReader(gbifNameResource);

            String line;
            while ((line = reader.readLine()) != null) {
                handleLine(line, lineHandler);
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to retrieve [" + gbifNameResource + "]");
        }
    }

    private void handleLine(String line, LineHandler lineHandler) {
        System.out.println(line);
        String[] rowValues = CSVTSVUtil.splitTSV(line);

        if (rowValues != null && rowValues.length > 19) {
            String taxIdString = rowValues[0];
            long taxId = Long.parseLong(taxIdString);
            String relatedTaxIdString = rowValues[1];
            String rank = StringUtils.trim(rowValues[5]);
            String canonicalName = rowValues[19];
            lineHandler.handle(rowValues, taxIdString, taxId, relatedTaxIdString, rank, canonicalName);
        }

    }

    interface LineHandler {
        void handle(String[] rowValues, String taxIdString, long taxId, String relatedTaxIdString, String rank, String canonicalName);
    }

    private void putTaxonById(String[] rowValues, String taxIdString, long taxId, String rank, String canonicalName) {
        Taxon taxon = parseTaxon(rowValues, taxIdString, rank, canonicalName);

        nodes.put(taxId, TaxonUtil.taxonToMap(taxon));
    }

    private Taxon parseTaxon(String[] rowValues, String taxIdString, String rank, String canonicalName) {
        Taxon taxon = new TaxonImpl(canonicalName, TaxonomyProvider.ID_PREFIX_GBIF + taxIdString);
        taxon.setRank(StringUtils.lowerCase(rank));
        if (rowValues.length > 27) {
            String authorshipString = compileAuthorshipString(rowValues[26], rowValues[27]);
            if (StringUtils.isBlank(authorshipString)) {
                authorshipString = compileAuthorshipString(rowValues[24], rowValues[25]);
            } else {
                authorshipString = "(" + authorshipString + ")";
            }
            taxon.setAuthorship(authorshipString);
        }
        return taxon;
    }

    private void mapNameToIds(long taxId, String name) {
        List<Long> ids = name2nodeIds.get(name);
        List<Long> idsNew = ids == null
                ? new ArrayList<>()
                : new ArrayList<>(ids);
        idsNew.add(taxId);
        name2nodeIds.put(name, idsNew);
    }

    private Fun.Tuple2<Long, Long> parseChildParentRelation(String[] rowValues, String relatedTaxIdString, long taxId, RelationParser childParentRelationParser) {
        return childParentRelationParser.parseRelation(rowValues, relatedTaxIdString, taxId);
    }

    interface RelationParser {
        Fun.Tuple2<Long, Long> parseRelation(String[] rowValues, String relatedTaxIdString, long taxId);
    }

    private Fun.Tuple2<Long, Long> parseSynonymRelation(String[] rowValues, String relatedTaxIdString, long taxId) {
        return new SynonymParser().parseRelation(rowValues, relatedTaxIdString, taxId);

    }

    private static class SynonymParser implements RelationParser {


        @Override
        public Fun.Tuple2<Long, Long> parseRelation(String[] rowValues, String relatedTaxIdString, long taxId) {
            Fun.Tuple2<Long, Long> entry = null;
            if (StringUtils.isNumeric(relatedTaxIdString)) {
                long relatedTaxId = Long.parseLong(relatedTaxIdString);
                if (GBIFUtil.isSynonym(rowValues)) {
                    entry = new Fun.Tuple2<>(taxId, relatedTaxId);
                }
            }
            return entry;
        }
    }

    private static class ChildParentRelationParser implements RelationParser {

        @Override
        public Fun.Tuple2<Long, Long> parseRelation(String[] rowValues, String relatedTaxIdString, long taxId) {
            Fun.Tuple2<Long, Long> childParentRelation = null;
            if (StringUtils.isNumeric(relatedTaxIdString)) {
                long relatedTaxId = Long.parseLong(relatedTaxIdString);
                if (!GBIFUtil.isSynonym(rowValues)) {
                    childParentRelation = new Fun.Tuple2<>(taxId, relatedTaxId);
                }
            }
            return childParentRelation;
        }
    }
}


