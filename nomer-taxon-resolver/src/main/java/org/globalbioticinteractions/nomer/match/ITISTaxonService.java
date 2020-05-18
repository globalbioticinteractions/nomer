package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.CacheService;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.TaxonCacheService;
import org.globalbioticinteractions.nomer.util.PropertyEnricherInfo;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@PropertyEnricherInfo(name = "itis-taxon-id", description = "Lookup ITIS taxon by id with ITIS:* prefix.")
public class ITISTaxonService implements PropertyEnricher {

    private static final Log LOG = LogFactory.getLog(ITISTaxonService.class);
    private static final String DENORMALIZED_NODES = "denormalizedNodes";
    private static final String MERGED_NODES = "mergedNodes";


    private final TermMatcherContext ctx;

    private BTreeMap<String, String> mergedNodes = null;
    private BTreeMap<String, Map<String, String>> itisDenormalizedNodes = null;
    private boolean temporaryCache = false;

    public ITISTaxonService(TermMatcherContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enriched = new TreeMap<>(properties);
        String externalId = properties.get(PropertyAndValueDictionary.EXTERNAL_ID);
        if (StringUtils.startsWith(externalId, TaxonomyProvider.ID_PREFIX_ITIS)) {
            if (needsInit()) {
                lazyInit();
            }
            String idForLookup = mergedNodes.getOrDefault(externalId, externalId);
            Map<String, String> enrichedProperties = itisDenormalizedNodes.get(idForLookup);
            enriched = enrichedProperties == null ? enriched : new TreeMap<>(enrichedProperties);
        }
        return enriched;
    }


    static void parseNodes(Map<String, Map<String, String>> taxonMap,
                           Map<String, String> childParent,
                           Map<String, String> rankIdNameMap,
                           InputStream resourceAsStream) throws PropertyEnricherException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "|");
                if (rowValues.length > 24) {
                    String taxId = rowValues[0];
                    String parentTaxId = rowValues[17];
                    String rankKingdomId = rowValues[20];
                    String rankId = rowValues[21];
                    String rankKey = rankKingdomId + "-" + rankId;
                    String rank = rankIdNameMap.getOrDefault(rankKey, rankKey);

                    String completeName = rowValues[25];

                    String externalId = TaxonomyProvider.ID_PREFIX_ITIS + taxId;
                    TaxonImpl taxon = new TaxonImpl(completeName, externalId);
                    taxon.setRank(StringUtils.equals(StringUtils.trim(rank), "no rank") ? "" : rank);
                    taxonMap.put(externalId, TaxonUtil.taxonToMap(taxon));
                    childParent.put(
                            TaxonomyProvider.ID_PREFIX_ITIS + taxId,
                            TaxonomyProvider.ID_PREFIX_ITIS + parentTaxId
                    );
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

    static void denormalizeTaxa(Map<String, Map<String, String>> taxonMap, Map<String, Map<String, String>> taxonMapDenormalized, Map<String, String> childParent) {
        Set<Map.Entry<String, Map<String, String>>> taxa = taxonMap.entrySet();
        for (Map.Entry<String, Map<String, String>> taxon : taxa) {
            denormalizeTaxa(taxonMap, taxonMapDenormalized, childParent, taxon);
        }
    }

    private static void denormalizeTaxa(Map<String, Map<String, String>> taxonMap,
                                        Map<String, Map<String, String>> taxonEnrichMap,
                                        Map<String, String> childParent,
                                        Map.Entry<String, Map<String, String>> taxon) {
        Map<String, String> childTaxon = taxon.getValue();
        List<String> pathNames = new ArrayList<>();
        List<String> pathIds = new ArrayList<>();
        List<String> path = new ArrayList<>();

        Taxon origTaxon = TaxonUtil.mapToTaxon(childTaxon);

        path.add(StringUtils.defaultIfBlank(origTaxon.getName(), ""));

        String externalId = origTaxon.getExternalId();
        pathIds.add(StringUtils.defaultIfBlank(externalId, ""));

        pathNames.add(StringUtils.defaultIfBlank(origTaxon.getRank(), ""));

        String parent = childParent.get(taxon.getKey());
        while (StringUtils.isNotBlank(parent) && !pathIds.contains(parent)) {
            Map<String, String> parentTaxonProperties = taxonMap.get(parent);
            if (parentTaxonProperties != null) {
                Taxon parentTaxon = TaxonUtil.mapToTaxon(parentTaxonProperties);
                path.add(StringUtils.defaultIfBlank(parentTaxon.getName(), ""));
                pathNames.add(StringUtils.defaultIfBlank(parentTaxon.getRank(), ""));
                pathIds.add(StringUtils.defaultIfBlank(parentTaxon.getExternalId(), ""));
            }
            parent = childParent.get(parent);
        }

        Collections.reverse(pathNames);
        Collections.reverse(pathIds);
        Collections.reverse(path);

        origTaxon.setPath(StringUtils.join(path, CharsetConstant.SEPARATOR));
        origTaxon.setPathIds(StringUtils.join(pathIds, CharsetConstant.SEPARATOR));
        origTaxon.setPathNames(StringUtils.join(pathNames, CharsetConstant.SEPARATOR));

        taxonEnrichMap.put(taxon.getKey(), TaxonUtil.taxonToMap(origTaxon));
    }

    static void parseMerged(Map<String, String> mergedMap, InputStream resourceAsStream) throws PropertyEnricherException {
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
                                TaxonomyProvider.ID_PREFIX_ITIS + oldTaxId,
                                TaxonomyProvider.ID_PREFIX_ITIS + newTaxId
                        );
                    }
                }
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse ITIS taxon dump", e);
        }
    }


    private void lazyInit() throws PropertyEnricherException {
        File cacheDir = getCacheDir();
        if (temporaryCache) {
            CacheService.createCacheDir(cacheDir);
        } else {
            if (!cacheDir.exists()) {
                if (!cacheDir.mkdirs()) {
                    throw new PropertyEnricherException("failed to create cache dir at [" + cacheDir.getAbsolutePath() + "]");
                }
            }
        }

        File taxonomyDir = new File(getCacheDir(), "itis");
        DB db = DBMaker
                .newFileDB(taxonomyDir)
                .mmapFileEnableIfSupported()
                .compressionEnable()
                .closeOnJvmShutdown()
                .transactionDisable()
                .make();

        if (db.exists(DENORMALIZED_NODES) && db.exists(MERGED_NODES)) {
            LOG.info("ITIS taxonomy already indexed at [" + taxonomyDir.getAbsolutePath() + "], no need to import.");
            itisDenormalizedNodes = db.getTreeMap(DENORMALIZED_NODES);
            mergedNodes = db.getTreeMap(MERGED_NODES);
        } else {
            LOG.info("ITIS taxonomy importing...");
            StopWatch watch = new StopWatch();
            watch.start();

            BTreeMap<String, String> rankIdNameMap = db
                    .createTreeMap("rankIdNameMap")
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();

            try {
                parseTaxonUnitTypes(rankIdNameMap, ctx.getResource(getTaxonUnitTypes()));
            } catch (IOException e) {
                throw new PropertyEnricherException("failed to parse NCBI taxon unit types", e);
            }

            BTreeMap<String, Map<String, String>> ncbiNodes = db
                    .createTreeMap("nodes")
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();

            BTreeMap<String, String> childParent = db
                    .createTreeMap("childParent")
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();

            try {
                parseNodes(ncbiNodes, childParent, rankIdNameMap, ctx.getResource(getNodesUrl()));
            } catch (IOException e) {
                throw new PropertyEnricherException("failed to parse NCBI nodes", e);
            }

            mergedNodes = db
                    .createTreeMap(MERGED_NODES)
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();


            try {
                parseMerged(mergedNodes, ctx.getResource(getMergedNodesUrl()));
            } catch (IOException e) {
                throw new PropertyEnricherException("failed to parse NCBI nodes", e);
            }

            itisDenormalizedNodes = db
                    .createTreeMap(DENORMALIZED_NODES)
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();
            denormalizeTaxa(ncbiNodes, itisDenormalizedNodes, childParent);

            watch.stop();
            TaxonCacheService.logCacheLoadStats(watch.getTime(), ncbiNodes.size(), LOG);
            LOG.info("ITIS taxonomy imported.");
        }
    }

    private boolean needsInit() {
        return itisDenormalizedNodes == null;
    }

    @Override
    public void shutdown() {

    }

    private File getCacheDir() {
        File cacheDir = new File(ctx.getCacheDir(), "itis");
        cacheDir.mkdirs();
        return cacheDir;
    }

    private String getNodesUrl() throws PropertyEnricherException {
        return ctx.getProperty("nomer.itis.taxonomic_units");
    }

    private String getTaxonUnitTypes() throws PropertyEnricherException {
        return ctx.getProperty("nomer.itis.taxon_unit_types");
    }

    private String getMergedNodesUrl() throws PropertyEnricherException {
        return ctx.getProperty("nomer.itis.synonym_links");
    }

    public void setTemporaryCache(boolean temporaryCache) {
        this.temporaryCache = temporaryCache;
    }
}
