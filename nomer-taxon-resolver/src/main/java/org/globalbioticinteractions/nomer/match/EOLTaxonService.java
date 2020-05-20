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
import org.eol.globi.service.CacheServiceUtil;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.PropertyEnricherSimple;
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

@PropertyEnricherInfo(name = "eol-taxon-id", description = "Lookup EOL pages by id with EOL:* prefix using offline-enabled database dump")
public class EOLTaxonService extends PropertyEnricherSimple {

    private static final Log LOG = LogFactory.getLog(EOLTaxonService.class);
    private static final String DENORMALIZED_NODES = "denormalizedNodes";
    private static final String MERGED_NODES = "mergedNodes";


    private final TermMatcherContext ctx;

    private BTreeMap<String, Map<String, String>> eolDenormalizedNodes = null;

    public EOLTaxonService(TermMatcherContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enriched = new TreeMap<>(properties);
        String externalId = properties.get(PropertyAndValueDictionary.EXTERNAL_ID);
        if (StringUtils.startsWith(externalId, TaxonomyProvider.ID_PREFIX_EOL)) {
            if (needsInit()) {
                if (ctx == null) {
                    throw new PropertyEnricherException("context needed to initialize");
                }
                lazyInit();
            }
            Map<String, String> enrichedProperties = eolDenormalizedNodes.get(externalId);
            enriched = enrichedProperties == null ? enriched : new TreeMap<>(enrichedProperties);
        }
        return enriched;
    }


    static void parseNodes(Map<String, Map<String, String>> taxonMap,
                           Map<String, String> childParent,
                           InputStream resourceAsStream) throws PropertyEnricherException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "\t");
                if (rowValues.length > 12) {
                    String taxId = rowValues[0];
                    String parentTaxId = rowValues[4];
                    String rank = rowValues[7];

                    String canonicalName = rowValues[11];
                    String pageId = rowValues[12];

                    String externalId = StringUtils.isBlank(pageId)
                            ? ""
                            : (TaxonomyProvider.ID_PREFIX_EOL + pageId);

                    TaxonImpl taxon = new TaxonImpl(canonicalName, externalId);
                    taxon.setRank(rank);
                    taxon.setExternalId(externalId);

                    taxonMap.put(taxId, TaxonUtil.taxonToMap(taxon));

                    childParent.put(
                            taxId,
                            parentTaxId
                    );
                }
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse EOL taxon dump", e);
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

        taxonEnrichMap.put(externalId, TaxonUtil.taxonToMap(origTaxon));
    }

    private void lazyInit() throws PropertyEnricherException {
        File cacheDir = getCacheDir(ctx);
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                throw new PropertyEnricherException("failed to create cache dir at [" + cacheDir.getAbsolutePath() + "]");
            }
        }

        File taxonomyDir = new File(cacheDir, "eol");
        DB db = DBMaker
                .newFileDB(taxonomyDir)
                .mmapFileEnableIfSupported()
                .compressionEnable()
                .closeOnJvmShutdown()
                .transactionDisable()
                .make();

        if (db.exists(DENORMALIZED_NODES)) {
            LOG.info("EOL taxonomy already indexed at [" + taxonomyDir.getAbsolutePath() + "], no need to import.");
            eolDenormalizedNodes = db.getTreeMap(DENORMALIZED_NODES);
        } else {
            LOG.info("EOL taxonomy importing...");
            StopWatch watch = new StopWatch();
            watch.start();

            BTreeMap<String, Map<String, String>> eolNodes = db
                    .createTreeMap("nodes")
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();

            BTreeMap<String, String> childParent = db
                    .createTreeMap("childParent")
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();

            try {
                String taxonUrl = ctx.getProperty("nomer.eol.taxon");
                if (taxonUrl == null) {
                    throw new PropertyEnricherException("no url for taxon resource [nomer.eol.taxon] found");
                }
                InputStream resource = ctx.getResource(taxonUrl);
                parseNodes(eolNodes, childParent, resource);
            } catch (IOException e) {
                throw new PropertyEnricherException("failed to parse EOL nodes", e);
            }

            eolDenormalizedNodes = db
                    .createTreeMap(DENORMALIZED_NODES)
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();
            denormalizeTaxa(eolNodes, eolDenormalizedNodes, childParent);

            watch.stop();
            TaxonCacheService.logCacheLoadStats(watch.getTime(), eolNodes.size(), LOG);
            LOG.info("EOL taxonomy imported.");
        }
    }

    private boolean needsInit() {
        return eolDenormalizedNodes == null;
    }

    @Override
    public void shutdown() {

    }

    private File getCacheDir(TermMatcherContext ctx) {
        File cacheDir = new File(ctx.getCacheDir(), "eol");
        cacheDir.mkdirs();
        return cacheDir;
    }

}
