package org.globalbioticinteractions.nomer.match;

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
import org.eol.globi.util.ExternalIdUtil;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.mapdb.BTreeKeySerializer;
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
import java.util.Map;

public class WorldOfFloraOnlineTaxonService extends CommonTaxonService<Long> {
    private static final Logger LOG = LoggerFactory.getLogger(WorldOfFloraOnlineTaxonService.class);

    public static final String ACCEPTED_LABEL = "ACCEPTED";
    public static final String SYNONYM_LABEL = "SYNONYM";
    public static final String UNCHECKED_LABEL = "UNCHECKED";


    public WorldOfFloraOnlineTaxonService(TermMatcherContext ctx) {
        super(ctx);
    }

    @Override
    public TaxonomyProvider getTaxonomyProvider() {
        return TaxonomyProvider.WORLD_OF_FLORA_ONLINE;
    }

    @Override
    public Long getIdOrNull(Taxon key, TaxonomyProvider matchingTaxonomyProvider) {
        TaxonomyProvider taxonomyProvider = ExternalIdUtil.taxonomyProviderFor(key.getExternalId());
        String idString = ExternalIdUtil.stripPrefix(matchingTaxonomyProvider, key.getExternalId());
        return (matchingTaxonomyProvider.equals(taxonomyProvider)
                && StringUtils.isNoneBlank(idString))
                ? Long.parseLong(idString)
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
            LOG.info("[" + getTaxonomyProvider().name() + "] taxonomy already indexed at [" + taxonomyDir.getAbsolutePath() + "], no need to import.");
            nodes = db.getTreeMap(NODES);
            childParent = db.getTreeMap(CHILD_PARENT);
            mergedNodes = db.getTreeMap(MERGED_NODES);
            name2nodeIds = db.getTreeMap(NAME_TO_NODE_IDS);
        } else {
            LOG.info("[" + getTaxonomyProvider().name() + "] taxonomy importing...");
            StopWatch watch = new StopWatch();
            watch.start();

            try (InputStream resource = getClassificationStream()) {

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

                NameUsageListener<Long> nameUsageListener = new NameUsageListenerImpl(mergedNodes, nodes, childParent);
                parseNameUsage(resource, nameUsageListener);
            } catch (IOException e) {
                throw new PropertyEnricherException("failed to parse taxon", e);
            }

            TaxonCacheService.logCacheLoadStats(watch.getTime(), nodes.size(), LOG);

            watch.stop();
            LOG.info("[" + getTaxonomyProvider().name() + "] taxonomy imported.");

        }
    }


    private InputStream getClassificationStream() throws PropertyEnricherException {
        InputStream resource;
        try {
            resource = getCtx().retrieve(getClassificationResource());
            if (resource == null) {
                throw new PropertyEnricherException("Catalogue of Life init failure: failed to find [" + getClassificationResource() + "]");
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse [" + getTaxonomyProvider().name() + "]", e);
        }
        return resource;
    }

    private void parseNameUsage(InputStream resource, NameUsageListener<Long> nameUsageListener) throws PropertyEnricherException {
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

    private void parseLine(NameUsageListener<Long> nameUsageListener, String line) {
        String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "\t");
        if (rowValues.length > 8) {
            String taxIdString = StringUtils.remove(rowValues[0], "wfo-");
            Long taxId = getIdAsLong(taxIdString);
            String parentTaxIdString = StringUtils.remove(rowValues[5], "wfo-");
            Long parentTaxId = getIdAsLong(parentTaxIdString);
            String acceptedNameUsageIdString = StringUtils.remove(rowValues[19], "wfo-");
            Long acceptedNameUsageId = getIdAsLong(acceptedNameUsageIdString);
            String status = rowValues[18];
            String completeName = rowValues[3];
            String authorship = rowValues[6];
            String rank = StringUtils.lowerCase(rowValues[4]);

            String idPrefix = getTaxonomyProvider().getIdPrefix();
            TaxonImpl taxon = new TaxonImpl(completeName, idPrefix + taxIdString);
            if (StringUtils.isNoneBlank(authorship)) {
                taxon.setAuthorship(StringUtils.trim(authorship));
            }

            taxon.setRank(StringUtils.equals(StringUtils.trim(rank), "no rank") ? "" : rank);

            nameUsageListener.handle(
                    status,
                    taxId,
                    acceptedNameUsageId == null ? parentTaxId : acceptedNameUsageId,
                    taxon
            );

        }
    }

    private static Long getIdAsLong(String stripped) {
        return NumberUtils.isDigits(stripped) ? Long.parseLong(stripped) : null;
    }

    interface NameUsageListener<T> {
        void handle(String status, T childTaxId, T parentTaxId, Taxon taxon);
    }

    private NameType getNameType(String statusValue) {
        NameType nameType = NameType.NONE;
        if (StringUtils.equals(statusValue, SYNONYM_LABEL)) {
            nameType = NameType.SYNONYM_OF;
        } else if (StringUtils.equals(statusValue, ACCEPTED_LABEL)) {
            nameType = NameType.HAS_ACCEPTED_NAME;
        } else if (StringUtils.equals(statusValue, UNCHECKED_LABEL)) {
            nameType = NameType.HAS_ACCEPTED_NAME;
        }
        return nameType;
    }

    private void skipFirstLine(BufferedReader reader) throws IOException {
        reader.readLine();
    }

    private URI getClassificationResource() {
        String propertyValue = getCtx().getProperty("nomer.wfo.classification.url");
        return URI.create(propertyValue);
    }

    private class NameUsageListenerImpl implements NameUsageListener<Long> {
        private final Map<Long, Long> mergedNodes;
        private final Map<Long, Map<String, String>> nodes;
        private final Map<Long, Long> childParent;

        public NameUsageListenerImpl(Map<Long, Long> mergedNodes,
                                     Map<Long, Map<String, String>> nodes,
                                     Map<Long, Long> childParent) {
            this.mergedNodes = mergedNodes;
            this.nodes = nodes;
            this.childParent = childParent;
        }

        @Override
        public void handle(String status, Long childTaxId, Long parentTaxId, Taxon taxon) {
            registerIdForName(childTaxId, taxon, WorldOfFloraOnlineTaxonService.this.name2nodeIds);

            NameType nameType = getNameType(status);
            if (NameType.SYNONYM_OF.equals(nameType)
                    && childTaxId != null
                    && parentTaxId != null) {
                mergedNodes.put(childTaxId, parentTaxId);
            }

            if (childTaxId != null) {
                nodes.put(childTaxId, TaxonUtil.taxonToMap(taxon));
                if (parentTaxId != null) {
                    childParent.put(
                            childTaxId,
                            parentTaxId
                    );
                }
            }

        }
    }
}
