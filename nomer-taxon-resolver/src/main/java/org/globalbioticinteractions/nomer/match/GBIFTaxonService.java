package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.GBIFService;
import org.eol.globi.taxon.GBIFUtil;
import org.eol.globi.taxon.PropertyEnricherSimple;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.CacheUtil;
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
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GBIFTaxonService extends PropertyEnricherSimple implements TermMatcher {

    private static final Logger LOG = LoggerFactory.getLogger(GBIFTaxonService.class);
    public static final String ID_NAME_RANK = "idNameRanks";
    public static final String ID_RELATION = "idRelations";
    public static final String NAME_ID = "nameId";

    private final TermMatcherContext ctx;

    private BTreeMap<Long, Pair<GBIFNameRelationType, Long>> idRelations = null;
    private BTreeMap<Long, Pair<String, GBIFRank>> idNameRanks = null;
    private BTreeMap<String, List<Long>> nameIds = null;

    public GBIFTaxonService(TermMatcherContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void match(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException {
        lazyInitIfNeeded();

        for (Term term : terms) {
            List<Taxon> matchedTaxa = new ArrayList<>();

            String gbifId = GBIFService.getGBIFId(term.getId());
            if (StringUtils.isNotBlank(gbifId)) {
                Map<String, String> taxonMap = lookupTaxonById(gbifId);
                if (taxonMap != null) {
                    matchedTaxa.add(TaxonUtil.mapToTaxon(taxonMap));
                }
            } else if (StringUtils.isNotBlank(term.getName())) {
                List<Map<String, String>> matched = lookupTaxaByName(term.getName());
                matched.forEach(taxonForName -> {
                    matchedTaxa.add(TaxonUtil.mapToTaxon(taxonForName));
                });
            }


            if (matchedTaxa.isEmpty()) {
                termMatchListener.foundTaxonForTerm(null,
                        term,
                        NameType.NONE,
                        new TaxonImpl(term.getName(), term.getId())
                );
            } else {
                matchedTaxa.forEach(matchedTerm -> {
                    termMatchListener.foundTaxonForTerm(
                            null,
                            term,
                            NameType.SAME_AS,
                            matchedTerm
                    );
                });
            }
        }
    }

    private List<Map<String, String>> lookupTaxaByName(String name) {
        StopWatch watch = new StopWatch();
        watch.start();
        List<Long> matchedIds = nameIds.get(name);
        watch.stop();
        watch.reset();
        watch.start();
        List<Map<String, String>> maps =
                matchedIds == null || matchedIds.size() == 0
                        ? Collections.emptyList()
                        : matchedIds
                        .stream()
                        .map(id -> lookupTaxonById(id.toString())
                        ).collect(Collectors.toList());
        watch.stop();
        return maps;

    }

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        lazyInitIfNeeded();

        Map<String, String> enriched = null;

        String gbifTaxonId = GBIFService.getGBIFTaxonId(properties);
        if (gbifTaxonId != null) {
            enriched = lookupTaxonById(gbifTaxonId);
        }

        if (enriched == null) {
            Taxon taxon = TaxonUtil.mapToTaxon(properties);
            if (StringUtils.isNotBlank(taxon.getName())) {
                List<Map<String, String>> enriched1 = lookupTaxaByName(taxon.getName());
                if (enriched1.size() > 0) {
                    enriched = enriched1.get(0);
                }
            }
        }

        return enriched == null
                ? properties
                : enriched;
    }

    private Map<String, String> lookupTaxonById(String gbifTaxonId) {
        StopWatch watch = new StopWatch();
        watch.start();
        Map<String, String> enrichedProperties = null;
        if (StringUtils.isNotBlank(gbifTaxonId)) {
            Taxon taxon = GBIFUtil.resolveTaxonId(idNameRanks, idRelations, Long.parseLong(gbifTaxonId));
            if (taxon != null) {
                enrichedProperties = TaxonUtil.taxonToMap(taxon);
            }
        }
        watch.stop();
        return enrichedProperties;
    }

    private void lazyInitIfNeeded() throws PropertyEnricherException {
        if (needsInit()) {
            lazyInit();
        }
    }

    private void lazyInit() throws PropertyEnricherException {
        File cacheDir = getCacheDir();
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                throw new PropertyEnricherException("failed to create cache dir at [" + cacheDir.getAbsolutePath() + "]");
            }
        }

        StopWatch watch = new StopWatch();
        watch.start();

        indexIfNeeded(watch);


        LOG.info("reading taxa done.");

        LOG.info("GBIF taxonomy imported.");
    }

    private void indexIfNeeded(StopWatch watch) throws PropertyEnricherException {
        File gbifTaxonomyDir = new File(getCacheDir(), "gbif");
        DB db = DBMaker
                .newFileDB(gbifTaxonomyDir)
                .mmapFileEnableIfSupported()
                .compressionEnable()
                .closeOnJvmShutdown()
                .transactionDisable()
                .make();


        if (db.exists(ID_NAME_RANK) && db.exists(ID_RELATION) && db.exists(NAME_ID)) {
            LOG.info("GBIF taxonomy ids already indexed at [" + gbifTaxonomyDir.getAbsolutePath() + "], no need to import.");
            idNameRanks = db.getTreeMap(ID_NAME_RANK);
            idRelations = db.getTreeMap(ID_RELATION);
            nameIds = db.getTreeMap(NAME_ID);
        } else {
            URI taxonUrl = CacheUtil.getValueURI(ctx, "nomer.gbif.ids");
            LOG.info("indexing GBIF taxonomy ids...");

            idNameRanks = db
                    .createTreeMap(ID_NAME_RANK)
                    .pumpSource(new Iterator<Fun.Tuple2<Long, Pair<String, GBIFRank>>>() {
                        BufferedReader reader = null;
                        Triple<Long, String, GBIFRank> idNameRank = null;

                        @Override
                        public boolean hasNext() {
                            try {
                                if (reader == null) {
                                    reader = new BufferedReader(new InputStreamReader(ctx.retrieve(taxonUrl), StandardCharsets.UTF_8));
                                }

                                if (idNameRank == null) {
                                    String line = reader.readLine();
                                    idNameRank =
                                            StringUtils.isBlank(line) ? null : GBIFUtil.parseIdNameRank(line);
                                }
                                return idNameRank != null;
                            } catch (IOException e) {
                                throw new RuntimeException("failed to access [" + taxonUrl + "]", e);
                            }
                        }

                        @Override
                        public Fun.Tuple2<Long, Pair<String, GBIFRank>> next() {
                            Fun.Tuple2<Long, Pair<String, GBIFRank>> current = idNameRank == null
                                    ? null
                                    : new Fun.Tuple2<>(idNameRank.getLeft(), Pair.of(idNameRank.getMiddle(), idNameRank.getRight()));
                            idNameRank = null;
                            return current;
                        }


                    })
                    .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                    .make();

            watch.stop();
            TaxonCacheService.logCacheLoadStats(watch.getTime(), idNameRanks.size(), LOG);

            watch.reset();
            watch.start();


            idRelations = db
                    .createTreeMap(ID_RELATION)
                    .pumpSource(new Iterator<Fun.Tuple2<Long, Pair<GBIFNameRelationType, Long>>>() {
                        BufferedReader reader = null;
                        Pair<Long, Pair<GBIFNameRelationType, Long>> idRelation = null;

                        @Override
                        public boolean hasNext() {
                            try {
                                if (reader == null) {
                                    reader = new BufferedReader(new InputStreamReader(ctx.retrieve(taxonUrl), StandardCharsets.UTF_8));
                                }
                                if (idRelation == null) {
                                    String line = reader.readLine();
                                    idRelation = GBIFUtil.parseIdRelation(line);
                                }
                                return idRelation != null;
                            } catch (IOException e) {
                                throw new RuntimeException("failed to access [" + taxonUrl + "]", e);
                            }
                        }

                        @Override
                        public Fun.Tuple2<Long, Pair<GBIFNameRelationType, Long>> next() {
                            Fun.Tuple2<Long, Pair<GBIFNameRelationType, Long>> next
                                    = idRelation == null ? null : new Fun.Tuple2<>(idRelation.getLeft(), idRelation.getRight());
                            idRelation = null;
                            return next;

                        }


                    })
                    .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                    .make();

            watch.stop();
            TaxonCacheService.logCacheLoadStats(watch.getTime(), idRelations.size(), LOG);

            watch.reset();
            watch.start();


            URI nameUrl = CacheUtil.getValueURI(ctx, "nomer.gbif.names");

            nameIds = db
                    .createTreeMap(NAME_ID)
                    .pumpSource(new Iterator<Fun.Tuple2<String, List<Long>>>() {
                        BufferedReader reader = null;
                        Pair<String, Long> nameId = null;

                        @Override
                        public boolean hasNext() {
                            try {
                                if (reader == null) {
                                    reader = new BufferedReader(new InputStreamReader(ctx.retrieve(nameUrl), StandardCharsets.UTF_8));
                                }

                                if (nameId == null) {
                                    readNext();
                                }
                                return nameId != null;
                            } catch (IOException e) {
                                throw new RuntimeException("failed to access [" + nameUrl + "]", e);
                            }
                        }

                        public void readNext() throws IOException {
                            String line = reader.readLine();
                            nameId =
                                    StringUtils.isBlank(line) ? null : GBIFUtil.parseNameId(line);
                        }

                        @Override
                        public Fun.Tuple2<String, List<Long>> next() {
                            List<Long> idsForName = new ArrayList<>();

                            String currentName = null;
                            if (nameId != null) {
                                currentName = nameId.getLeft();
                                do {
                                    idsForName.add(nameId.getRight());
                                    try {
                                        readNext();
                                    } catch (IOException e) {
                                        throw new RuntimeException("failed to access [" + nameUrl + "]", e);
                                    }
                                }
                                while (nameId != null && StringUtils.equals(currentName, nameId.getLeft()));
                            }

                            return StringUtils.isNotBlank(currentName)
                                    && idsForName.size() == 0
                                    ? null
                                    : new Fun.Tuple2<>(currentName, idsForName);
                        }


                    })
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();

            watch.stop();
            TaxonCacheService.logCacheLoadStats(watch.getTime(), idNameRanks.size(), LOG);


        }
    }

    private boolean needsInit() {
        return idNameRanks == null || idRelations == null || nameIds == null;
    }

    @Override
    public void shutdown() {

    }

    private File getCacheDir() {
        if (ctx == null) {
            throw new RuntimeException("enricher context not set, cannot create [" + GBIFTaxonService.class.getName() + "]");
        }
        File cacheDir = new File(ctx.getCacheDir(), "gbif");
        cacheDir.mkdirs();
        return cacheDir;
    }

}
