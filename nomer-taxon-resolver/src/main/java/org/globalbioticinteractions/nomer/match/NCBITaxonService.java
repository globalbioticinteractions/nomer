package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.CacheService;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.NCBIService;
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
import java.util.stream.Stream;

@PropertyEnricherInfo(name = "ncbi-taxon-id", description = "Lookup NCBI taxon by id with NCBI:* prefix.")
public class NCBITaxonService implements PropertyEnricher {

    private static final Log LOG = LogFactory.getLog(NCBITaxonService.class);
    public static final String DENORMALIZED_NODES = "denormalizedNodes";
    public static final String MERGED_NODES = "mergedNodes";


    private final TermMatcherContext ctx;

    private BTreeMap<String, String> mergedNodes = null;
    private BTreeMap<String, Map<String, String>> ncbiDenormalizedNodes = null;

    public NCBITaxonService(TermMatcherContext ctx) {
        this.ctx = ctx;
    }


    static void parseNodes(Map<String, String> childParent, InputStream resourceAsStream) throws PropertyEnricherException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] rowValues = StringUtils.splitByWholeSeparator(line, "\t|\t");
                String taxId = rowValues[0];
                String parentTaxId = rowValues[1];
                childParent.put(
                        TaxonomyProvider.ID_PREFIX_NCBI + taxId,
                        TaxonomyProvider.ID_PREFIX_NCBI + parentTaxId
                );
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse NCBI taxon dump", e);
        }
    }

    static void denormalizeTaxa(Map<String, Map<String, String>> taxonMap, Map<String, Map<String, String>> taxonMapDenormalized, Map<String, String> childParent, Map<String, String> taxonNames) {
        Set<Map.Entry<String, Map<String, String>>> taxa = taxonMap.entrySet();
        for (Map.Entry<String, Map<String, String>> taxon : taxa) {
            denormalizeTaxa(taxonMap, taxonMapDenormalized, childParent, taxonNames, taxon);
        }
    }

    private static void denormalizeTaxa(Map<String, Map<String, String>> taxonMap, Map<String, Map<String, String>> taxonEnrichMap, Map<String, String> childParent, Map<String, String> names, Map.Entry<String, Map<String, String>> taxon) {
        Map<String, String> childTaxon = taxon.getValue();
        List<String> pathNames = new ArrayList<>();
        List<String> pathIds = new ArrayList<>();
        List<String> path = new ArrayList<>();

        Taxon origTaxon = TaxonUtil.mapToTaxon(childTaxon);

        String str = names.get(origTaxon.getExternalId());
        origTaxon.setName(str);
        path.add(StringUtils.defaultIfBlank(str, ""));
        String externalId = origTaxon.getExternalId();
        origTaxon.setExternalId(externalId);
        pathIds.add(StringUtils.defaultIfBlank(externalId, ""));

        origTaxon.setRank(origTaxon.getRank());
        pathNames.add(StringUtils.defaultIfBlank(origTaxon.getRank(), ""));

        String parent = childParent.get(taxon.getKey());
        while (StringUtils.isNotBlank(parent)) {
            Map<String, String> stringStringMap = taxonMap.get(parent);
            Taxon parentTaxon = TaxonUtil.mapToTaxon(stringStringMap);
            pathNames.add(StringUtils.defaultIfBlank(parentTaxon.getRank(), ""));
            pathIds.add(StringUtils.defaultIfBlank(parentTaxon.getExternalId(), ""));
            path.add(StringUtils.defaultIfBlank(names.get(parentTaxon.getExternalId()), ""));
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

    static void parseNames(Map<String, String> nameMap, InputStream resourceAsStream) throws PropertyEnricherException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "\t|\t");
                if (rowValues.length > 3) {
                    String taxId = rowValues[0];
                    String taxonName = rowValues[1];
                    String rank = rowValues[2];
                    String taxonNameClass = StringUtils.replace(rowValues[3], "\t|", "");

                    Stream<String> knownNameClasses = Stream.of(
                            "acronym",
                            "anamorph",
                            "authority",
                            "blast name",
                            "common name",
                            "equivalent name",
                            "genbank acronym",
                            "genbank common name",
                            "genbank synonym",
                            "includes",
                            "in-part",
                            "scientific name",
                            "synonym",
                            "teleomorph",
                            "type material");

                    if (StringUtils.equals("scientific name", taxonNameClass)) {
                        nameMap.put(TaxonomyProvider.ID_PREFIX_NCBI + taxId, taxonName);
                    }
                }
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse NCBI taxon dump", e);
        }
    }

    static void parseMerged(Map<String, String> mergedMap, InputStream resourceAsStream) throws PropertyEnricherException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "\t|\t");
                if (rowValues.length > 1) {
                    String oldTaxId = rowValues[0];
                    String newTaxId = StringUtils.replace(rowValues[1], "\t|", "");
                    if (StringUtils.isNotBlank(oldTaxId) && StringUtils.isNotBlank(newTaxId)) {
                        mergedMap.put(TaxonomyProvider.ID_PREFIX_NCBI + oldTaxId, TaxonomyProvider.ID_PREFIX_NCBI + newTaxId);
                    }
                }
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse NCBI taxon dump", e);
        }
    }

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enriched = new TreeMap<String, String>(properties);
        String ncbiTaxonId = NCBIService.getNCBITaxonId(properties);
        if (StringUtils.isNotBlank(ncbiTaxonId)) {
            if (needsInit()) {
                lazyInit();
            }
            String externalId = TaxonomyProvider.ID_PREFIX_NCBI + ncbiTaxonId;
            String idForLookup = mergedNodes.getOrDefault(externalId, externalId);
            Map<String, String> enrichedProperties = ncbiDenormalizedNodes.get(idForLookup);
            enriched = enrichedProperties == null ? enriched : new TreeMap<>(enrichedProperties);
        }
        return enriched;
    }

    private void lazyInit() throws PropertyEnricherException {
        CacheService.createCacheDir(getCacheDir());
        File ncbiTaxonomyDir = new File(getCacheDir(), "ncbi");
        DB db = DBMaker
                .newFileDB(ncbiTaxonomyDir)
                .mmapFileEnableIfSupported()
                .closeOnJvmShutdown()
                .transactionDisable()
                .make();

        if (db.exists(DENORMALIZED_NODES) && db.exists(MERGED_NODES)) {
            LOG.info("NCBI taxonomy already indexed at [" + ncbiTaxonomyDir.getAbsolutePath() + "], no need to import.");
            ncbiDenormalizedNodes = db.getTreeMap(DENORMALIZED_NODES);
            mergedNodes = db.getTreeMap(MERGED_NODES);
        } else {
            LOG.info("NCBI taxonomy importing...");
            StopWatch watch = new StopWatch();
            watch.start();

            BTreeMap<String, Map<String, String>> ncbiNodes = db
                    .createTreeMap("nodes")
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();

            BTreeMap<String, String> childParent = db
                    .createTreeMap("childParent")
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();

            try {
                parseNodes(childParent, ctx.getResource(getNodesUrl()));
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

            BTreeMap<String, String> ncbiNames = db
                    .createTreeMap("names")
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();

            try {
                parseNames(ncbiNames, ctx.getResource(getNamesUrl()));
            } catch (IOException e) {
                throw new PropertyEnricherException("failed to parse NCBI nodes", e);
            }


            ncbiDenormalizedNodes = db
                    .createTreeMap(DENORMALIZED_NODES)
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();

            denormalizeTaxa(ncbiNodes, ncbiDenormalizedNodes, childParent, ncbiNames);

            watch.stop();
            TaxonCacheService.logCacheLoadStats(watch.getTime(), ncbiNodes.size(), LOG);
            LOG.info("NCBI taxonomy imported.");
        }
    }

    private boolean needsInit() {
        return ncbiDenormalizedNodes == null;
    }

    @Override
    public void shutdown() {

    }

    private File getCacheDir() {
        File cacheDir = new File(ctx.getCacheDir(), "ncbi");
        cacheDir.mkdirs();
        return cacheDir;
    }

    private String getNodesUrl() throws PropertyEnricherException {
        return ctx.getProperty("nomer.ncbi.nodes");
    }

    private String getMergedNodesUrl() throws PropertyEnricherException {
        return ctx.getProperty("nomer.ncbi.merged");
    }

    private String getNamesUrl() throws PropertyEnricherException {
        return ctx.getProperty("nomer.ncbi.names");
    }


}
