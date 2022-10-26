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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class OpenTreeTaxonService extends CommonTaxonService<String> {
    private static final Logger LOG = LoggerFactory.getLogger(OpenTreeTaxonService.class);


    public OpenTreeTaxonService(TermMatcherContext ctx) {
        super(ctx);
    }

    @Override
    public TaxonomyProvider getTaxonomyProvider() {
        return TaxonomyProvider.OPEN_TREE_OF_LIFE;
    }

    @Override
    protected boolean isIdSchemeSupported(String externalId) {
        return Arrays.asList(
                TaxonomyProvider.OPEN_TREE_OF_LIFE,
                TaxonomyProvider.GBIF,
                TaxonomyProvider.WORMS,
                TaxonomyProvider.NCBI,
                TaxonomyProvider.INTERIM_REGISTER_OF_MARINE_AND_NONMARINE_GENERA,
                TaxonomyProvider.INDEX_FUNGORUM)
                .contains(ExternalIdUtil.taxonomyProviderFor(externalId));
    }


    @Override
    public String getIdOrNull(Taxon key, TaxonomyProvider matchingTaxonomyProvider) {
        TaxonomyProvider taxonomyProvider = ExternalIdUtil.taxonomyProviderFor(key.getExternalId());
        return getTaxonomyProvider().equals(taxonomyProvider)
                ? ExternalIdUtil.stripPrefix(taxonomyProvider, key.getExternalId())
                : key.getExternalId();
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
            LOG.debug("[Open Tree of Life] taxonomy already indexed at [" + taxonomyDir.getAbsolutePath() + "], no need to import.");
            nodes = db.getTreeMap(NODES);
            childParent = db.getTreeMap(CHILD_PARENT);
            mergedNodes = db.getTreeMap(MERGED_NODES);
            name2nodeIds = db.getTreeMap(NAME_TO_NODE_IDS);
        } else {
            LOG.info("[" + getTaxonomyProvider().name() + "] taxonomy importing...");
            StopWatch watch = new StopWatch();
            watch.start();
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

                NameUsageListener nameUsageListener = new NameUsageListenerImpl(mergedNodes, nodes, childParent, name2nodeIds);
                parseNameUsage(resource, nameUsageListener);
            } catch (IOException e) {
                throw new PropertyEnricherException("failed to parse taxon", e);
            }

            TaxonCacheService.logCacheLoadStats(watch.getTime(), nodes.size(), LOG);

            watch.stop();
            LOG.info("[" + getTaxonomyProvider().name() + "] taxonomy imported.");

        }
    }



    private InputStream getNameUsageStream() throws PropertyEnricherException {
        InputStream resource;
        try {
            resource = getCtx().retrieve(getTaxonomy());
            if (resource == null) {
                throw new PropertyEnricherException("Open Tree of Life init failure: failed to find [" + getTaxonomy() + "]");
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
            throw new PropertyEnricherException("failed to parse [Open Tree of Life] taxon dump", e);
        }
    }

    private void parseLine(NameUsageListener nameUsageListener, String line) {
        String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "\t|\t");
        if (rowValues.length > 7) {
            String taxId = rowValues[0];
            String parentTaxId = rowValues[1];
            String completeName = rowValues[2];
            String rank = rowValues[3];
            String[] ids = StringUtils.split(rowValues[4],",");
            String idPrefix = getTaxonomyProvider().getIdPrefix();
            TaxonImpl taxon = new TaxonImpl(completeName, idPrefix + taxId);

            taxon.setRank(StringUtils.equals(StringUtils.trim(rank), "no rank") ? "" : rank);

            if (ids != null) {
                for (String id : ids) {
                    nameUsageListener.handle(NameType.SAME_AS, StringUtils.upperCase(id), taxId, taxon);
                }
            }
            nameUsageListener.handle(null, taxId, parentTaxId, taxon);

        }
    }

    interface NameUsageListener {
        void handle(NameType status, String childTaxId, String parentTaxId, Taxon taxon);
    }

    private void skipFirstLine(BufferedReader reader) throws IOException {
        reader.readLine();
    }

    private URI getTaxonomy() {
        String propertyValue = getCtx().getProperty("nomer.ott.taxonomy");
        return URI.create(propertyValue);
    }

    private class NameUsageListenerImpl implements NameUsageListener {
        private final Map<String, String> mergedNodes;
        private final Map<String, Map<String, String>> nodes;
        private final Map<String, String> childParent;
        private final Map<String, List<String>> name2ids;

        public NameUsageListenerImpl(Map<String, String> mergedNodes,
                                     Map<String, Map<String, String>> nodes,
                                     Map<String, String> childParent,
                                     Map<String, List<String>> name2ids) {
            this.mergedNodes = mergedNodes;
            this.nodes = nodes;
            this.childParent = childParent;
            this.name2ids = name2ids;
        }

        @Override
        public void handle(NameType status, String childTaxId, String parentTaxId, Taxon taxon) {
            registerIdForName(childTaxId, taxon, this.name2ids);
            if (NameType.SAME_AS.equals(status)) {
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
