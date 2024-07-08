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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PropertyEnricherInfo(name = "eol-taxon-id", description = "Lookup EOL pages by id with EOL:* prefix using offline-enabled database dump")
public class EOLTaxonService extends CommonLongTaxonService {

    private static final Logger LOG = LoggerFactory.getLogger(EOLTaxonService.class);

    private static final String INTERNAL_ID = "internalId";
    private static final String INTERNAL_ID_PATTERN_STRING = "(?<prefix>EOL-[0]+)(?<" + INTERNAL_ID + ">[0-9]+)";
    private static final Pattern PATTERN_INTERNAL_ID
            = Pattern.compile(INTERNAL_ID_PATTERN_STRING);

    private static final String ID_TO_PAGEID = "id2pageId";
    private BTreeMap<Long, Long> id2pageId;
    private static final String PAGEID_TO_ID = "pageId2Id";
    private BTreeMap<Long, Long> pageId2Id;



    private BTreeMap<String, Map<String, String>> eolDenormalizedNodes = null;

    public EOLTaxonService(TermMatcherContext ctx) {
        super(ctx);
    }


    @Override
    public TaxonomyProvider getTaxonomyProvider() {
        return TaxonomyProvider.EOL;
    }

    @Override
    public Long getIdOrNull(Taxon key, TaxonomyProvider matchingTaxonomyProvider) {
        TaxonomyProvider taxonomyProvider = ExternalIdUtil.taxonomyProviderFor(key.getExternalId());
        String idString = ExternalIdUtil.stripPrefix(matchingTaxonomyProvider, key.getExternalId());
        return (matchingTaxonomyProvider.equals(taxonomyProvider)
                && NumberUtils.isCreatable(idString))
                ? pageId2Id.get(Long.parseLong(idString))
                : null;
    }



    void parseNodes(Map<Long, Map<String, String>> taxonMap,
                           Map<Long, Long> childParent,
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
                    Matcher idMatcher = PATTERN_INTERNAL_ID.matcher(taxId);

                    if (idMatcher.matches() && NumberUtils.isDigits(pageId)) {

                        String externalId = StringUtils.isBlank(pageId)
                                ? ""
                                : (TaxonomyProvider.ID_PREFIX_EOL + pageId);

                        TaxonImpl taxon = new TaxonImpl(canonicalName, externalId);
                        taxon.setRank(rank);
                        taxon.setExternalId(externalId);

                        long pageIdNumber = Long.parseLong(pageId);
                        Long internalTaxonId = Long.parseLong(idMatcher.group(INTERNAL_ID));
                        id2pageId.put(internalTaxonId, pageIdNumber);
                        pageId2Id.put(pageIdNumber, internalTaxonId);
                        registerIdForName(internalTaxonId, taxon, name2nodeIds);
                        taxonMap.put(internalTaxonId, TaxonUtil.taxonToMap(taxon));

                        Matcher parentIdMatcher = PATTERN_INTERNAL_ID.matcher(parentTaxId);
                        if (idMatcher.matches() && parentIdMatcher.matches()) {
                            childParent.put(
                                    Long.parseLong(idMatcher.group(INTERNAL_ID)),
                                    Long.parseLong(parentIdMatcher.group(INTERNAL_ID))
                            );
                        }

                    }
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
            LOG.debug("ITIS taxonomy already indexed at [" + taxonomyDir.getAbsolutePath() + "], no need to import.");
            nodes = db.getTreeMap(NODES);
            childParent = db.getTreeMap(CHILD_PARENT);
            mergedNodes = db.getTreeMap(MERGED_NODES);
            name2nodeIds = db.getTreeMap(NAME_TO_NODE_IDS);
            id2pageId = db.getTreeMap(ID_TO_PAGEID);
            pageId2Id = db.getTreeMap(ID_TO_PAGEID);
        } else {
            LOG.info("EOL taxonomy indexing...");
            StopWatch watch = new StopWatch();
            watch.start();

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

            id2pageId = db
                    .createTreeMap(ID_TO_PAGEID)
                    .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                    .valueSerializer(Serializer.LONG)
                    .make();

            pageId2Id = db
                    .createTreeMap(PAGEID_TO_ID)
                    .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                    .valueSerializer(Serializer.LONG)
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
