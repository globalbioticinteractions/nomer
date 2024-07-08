package org.globalbioticinteractions.nomer.match;

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
import org.eol.globi.util.ExternalIdUtil;
import org.globalbioticinteractions.nomer.util.CacheUtil;
import org.globalbioticinteractions.nomer.util.PropertyEnricherInfo;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@PropertyEnricherInfo(name = "eol-taxon-id", description = "Lookup EOL pages by id with EOL:* prefix using offline-enabled database dump")
public class EOLTaxonService extends CommonStringTaxonService {

    private static final Logger LOG = LoggerFactory.getLogger(EOLTaxonService.class);

    private static final String ID_TO_PAGEID = "id2pageId";
    public static final String TAXON_ID = "taxonID";
    public static final String SOURCE = "source";
    public static final String FURTHER_INFORMATION_URL = "furtherInformationURL";
    public static final String ACCEPTED_NAME_USAGE_ID = "acceptedNameUsageID";
    public static final String PARENT_NAME_USAGE_ID = "parentNameUsageID";
    public static final String SCIENTIFIC_NAME = "scientificName";
    public static final String HIGHER_CLASSIFICATION = "higherClassification";
    public static final String TAXON_RANK = "taxonRank";
    public static final String TAXONOMIC_STATUS = "taxonomicStatus";
    public static final String TAXON_REMARKS = "taxonRemarks";
    public static final String DATASET_ID = "datasetID";
    public static final String CANONICAL_NAME = "canonicalName";
    public static final String EOL_ID = "EOLid";
    public static final String EOL_ID_ANNOTATIONS = "EOLidAnnotations";
    public static final String LANDMARK = "Landmark";
    public static final String AUTHORITY = "authority";
    private BTreeMap<String, String> id2pageId;
    private static final String PAGEID_TO_ID = "pageId2Id";
    private BTreeMap<String, String> pageId2Id;

    private static final Map<String, String> HEADER_NORMALIZER = new TreeMap<String, String>() {{
        put("taxonID", TAXON_ID);
        put("source", SOURCE);
        put("furtherInformationURL", FURTHER_INFORMATION_URL);
        put("acceptedNameUsageID", ACCEPTED_NAME_USAGE_ID);
        put("parentNameUsageID", PARENT_NAME_USAGE_ID);
        put("scientificName", SCIENTIFIC_NAME);
        put("higherClassification", HIGHER_CLASSIFICATION);
        put("taxonRank", TAXON_RANK);
        put("taxonomicStatus", TAXONOMIC_STATUS);
        put("taxonRemarks", TAXON_REMARKS);
        put("datasetID", DATASET_ID);
        put("canonicalName", CANONICAL_NAME);
        put("EOLid", EOL_ID);
        put("eolID", EOL_ID);
        put("EOLidAnnotations", EOL_ID_ANNOTATIONS);
        put("Landmark", LANDMARK);
        put("authority", AUTHORITY);
    }};

    public EOLTaxonService(TermMatcherContext ctx) {
        super(ctx);
    }


    @Override
    public TaxonomyProvider getTaxonomyProvider() {
        return TaxonomyProvider.EOL;
    }

    @Override
    public String getIdOrNull(Taxon key, TaxonomyProvider matchingTaxonomyProvider) {
        TaxonomyProvider taxonomyProvider = ExternalIdUtil.taxonomyProviderFor(key.getExternalId());
        String idString = ExternalIdUtil.stripPrefix(matchingTaxonomyProvider, key.getExternalId());
        return (matchingTaxonomyProvider.equals(taxonomyProvider)
                && NumberUtils.isCreatable(idString))
                ? pageId2Id.get(idString)
                : null;
    }


    void parseNodes(Map<String, Map<String, String>> taxonMap,
                    Map<String, String> childParent,
                    InputStream resourceAsStream) throws PropertyEnricherException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));

        String line;
        try {
            Map<String, Integer> headerMap = new TreeMap<>();
            while ((line = reader.readLine()) != null) {

                if (headerMap.size() == 0) {
                    String fixedHeader = StringUtils.replace(line, "taxonID\tsource\tfurtherInformationURL\tacceptedNameUsageID\tparentNameUsageID\tscientificName\thigherClassification\ttaxonRank\ttaxonomicStatustaxonRemarks\tdatasetID\tcanonicalName\tEOLid\tEOLidAnnotations\tLandmark", "taxonID\tsource\tfurtherInformationURL\tacceptedNameUsageID\tparentNameUsageID\tscientificName\thigherClassification\ttaxonRank\ttaxonomicStatus\ttaxonRemarks\tdatasetID\tcanonicalName\tEOLid\tEOLidAnnotations\tLandmark");
                    String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(fixedHeader, "\t");
                    for (int i = 0; i < rowValues.length; i++) {
                        String normalizedLabel = HEADER_NORMALIZER.get(rowValues[i]);
                        if (StringUtils.isBlank(normalizedLabel)) {
                            throw new IllegalArgumentException("unmapped name [" + rowValues[i] + "]");
                        }
                        headerMap.put(normalizedLabel, i);
                    }
                } else {
                    String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "\t");
                    if (rowValues.length > 12
                            && headerMap.containsKey(TAXON_ID)
                            && headerMap.containsKey(PARENT_NAME_USAGE_ID)
                            && headerMap.containsKey(TAXON_RANK)
                            && headerMap.containsKey(CANONICAL_NAME)
                            && headerMap.containsKey(EOL_ID)) {
                        parseTaxonRow(taxonMap, childParent, headerMap, rowValues);
                    }
                }
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse EOL taxon dump", e);
        }
    }

    private void parseTaxonRow(Map<String, Map<String, String>> taxonMap, Map<String, String> childParent, Map<String, Integer> headerMap, String[] rowValues) {
        String taxId = rowValues[headerMap.get(TAXON_ID)];
        String parentTaxId = rowValues[headerMap.get(PARENT_NAME_USAGE_ID)];
        String rank = rowValues[headerMap.get(TAXON_RANK)];

        String canonicalName = rowValues[headerMap.get(CANONICAL_NAME)];
        String pageId = rowValues[headerMap.get(EOL_ID)];


        String externalId = "";
        if (NumberUtils.isDigits(pageId)) {
            id2pageId.put(taxId, pageId);
            pageId2Id.put(pageId, taxId);
            externalId = TaxonomyProvider.ID_PREFIX_EOL + pageId;
        }

        TaxonImpl taxon = new TaxonImpl(canonicalName, externalId);
        taxon.setRank(rank);
        taxon.setExternalId(externalId);
        if (headerMap.containsKey(AUTHORITY)) {
            String authorship = rowValues[headerMap.get(AUTHORITY)];
            taxon.setAuthorship(authorship);
        }

        if (headerMap.containsKey(ACCEPTED_NAME_USAGE_ID)) {
            String acceptedNameUsage = rowValues[headerMap.get(ACCEPTED_NAME_USAGE_ID)];
            if (StringUtils.isNotBlank(acceptedNameUsage)) {
                mergedNodes.put(taxId, acceptedNameUsage);
            }
        }

        registerIdForName(taxId, taxon, name2nodeIds);
        taxonMap.put(taxId, TaxonUtil.taxonToMap(taxon));

        childParent.put(
                taxId,
                parentTaxId
        );
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

    @Override
    protected void lazyInit() throws PropertyEnricherException {
        File cacheDir = getCacheDir();
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                throw new PropertyEnricherException("failed to create cache dir at [" + cacheDir.getAbsolutePath() + "]");
            }
        }

        File taxonomyDir = new File(cacheDir, "eol");
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
                && db.exists(PAGEID_TO_ID)
                && db.exists(ID_TO_PAGEID)
                && db.exists(NAME_TO_NODE_IDS)) {
            LOG.debug("EOL taxonomy already indexed at [" + taxonomyDir.getAbsolutePath() + "], no need to import.");
            nodes = db.getTreeMap(NODES);
            mergedNodes = db.getTreeMap(MERGED_NODES);
            childParent = db.getTreeMap(CHILD_PARENT);
            name2nodeIds = db.getTreeMap(NAME_TO_NODE_IDS);
            id2pageId = db.getTreeMap(ID_TO_PAGEID);
            pageId2Id = db.getTreeMap(PAGEID_TO_ID);
        } else {
            LOG.info("EOL taxonomy indexing...");
            StopWatch watch = new StopWatch();
            watch.start();

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

            mergedNodes = db
                    .createTreeMap(MERGED_NODES)
                    .keySerializer(BTreeKeySerializer.STRING)
                    .valueSerializer(Serializer.STRING)
                    .make();

            name2nodeIds = db
                    .createTreeMap(NAME_TO_NODE_IDS)
                    .keySerializer(BTreeKeySerializer.STRING)
                    .valueSerializer(Serializer.JAVA)
                    .make();

            id2pageId = db
                    .createTreeMap(ID_TO_PAGEID)
                    .keySerializer(BTreeKeySerializer.STRING)
                    .valueSerializer(Serializer.STRING)
                    .make();

            pageId2Id = db
                    .createTreeMap(PAGEID_TO_ID)
                    .keySerializer(BTreeKeySerializer.STRING)
                    .valueSerializer(Serializer.STRING)
                    .make();

            try {
                InputStream resource = getCtx().retrieve(CacheUtil.getValueURI(getCtx(), "nomer.eol.taxon"));
                parseNodes(nodes, childParent, resource);
            } catch (IOException e) {
                throw new PropertyEnricherException("failed to parse EOL nodes", e);
            }


            watch.stop();
            TaxonCacheService.logCacheLoadStats(watch.getTime(), nodes.size(), LOG);
            LOG.info("EOL taxonomy indexed.");
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

}
