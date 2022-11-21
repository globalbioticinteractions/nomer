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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class OpenTreeTaxonService extends CommonTaxonService<Long> {
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
    public Long getIdOrNull(Taxon key, TaxonomyProvider matchingTaxonomyProvider) {
        TaxonomyProvider taxonomyProvider = ExternalIdUtil.taxonomyProviderFor(key.getExternalId());
        Long id;
        if (getTaxonomyProvider().equals(taxonomyProvider)) {
            id = Long.parseLong(ExternalIdUtil.stripPrefix(taxonomyProvider, key.getExternalId()));
        } else {
            List<Long> ids = name2nodeIds.get(key.getExternalId());
            id = ids != null && ids.size() > 0 ? ids.get(0) : null;
        }
        return id;
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

            initDb(db);
            indexTaxonomy(getResource("taxonomy"));
            indexSynonyms(getResource("synonyms"));

            TaxonCacheService.logCacheLoadStats(watch.getTime(), nodes.size(), LOG);

            watch.stop();
            LOG.info("[" + getTaxonomyProvider().name() + "] taxonomy imported.");

        }
    }

    private void indexSynonyms(URI synonyms) throws PropertyEnricherException {
        try (InputStream resource = getResourceStream(synonyms)) {
            NameUsageListener nameUsageListener = new NameUsageListener() {

                @Override
                public void handle(NameType type, Long childTaxId, Long parentTaxId, Taxon taxon) {
                    if (NameType.SYNONYM_OF.equals(type)) {
                        appendIdMap(parentTaxId, taxon.getName());
                        appendIdMap(parentTaxId, taxon.getExternalId());
                        mergedNodes.put(parentTaxId, parentTaxId);
                    }
                }

            };
            parseSynonyms(resource, nameUsageListener);
        } catch (IOException | PropertyEnricherException e) {
            throw new PropertyEnricherException("failed to parse taxon", e);
        }
    }

    private void appendIdMap(Long parentTaxId, String name) {
        if (StringUtils.isNotBlank(name)) {
            List<Long> ids = name2nodeIds.getOrDefault(name, Collections.emptyList());
            Set<Long> idsUpdated = new TreeSet<>(ids);
            idsUpdated.add(parentTaxId);
            name2nodeIds.put(name, new ArrayList<>(idsUpdated));
        }
    }


    private void parseSynonyms(InputStream resource, NameUsageListener nameUsageListener) throws PropertyEnricherException {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource));

            String line;
            skipFirstLine(reader);
            while ((line = reader.readLine()) != null) {
                parseSynonym(nameUsageListener, line);
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse [Open Tree of Life] taxon dump", e);
        }
    }

    private void indexTaxonomy(URI taxonomy) throws PropertyEnricherException {
        try (InputStream resource = getResourceStream(taxonomy)) {
            NameUsageListener nameUsageListener = new NameUsageListenerImpl(nodes, childParent, name2nodeIds);
            parseNameUsage(resource, nameUsageListener);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse taxon", e);
        }
    }

    private void initDb(DB db) {
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

        mergedNodes = db
                .createTreeMap(MERGED_NODES)
                .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                .valueSerializer(Serializer.LONG)
                .make();

        name2nodeIds = db
                .createTreeMap(NAME_TO_NODE_IDS)
                .keySerializer(BTreeKeySerializer.STRING)
                .valueSerializer(Serializer.JAVA)
                .make();
    }


    private InputStream getResourceStream(URI resource) throws PropertyEnricherException {
        InputStream is;
        try {
            is = getCtx().retrieve(resource);
            if (resource == null) {
                throw new PropertyEnricherException("Open Tree of Life init failure: failed to find [" + getResource("taxonomy") + "]");
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse [" + getTaxonomyProvider().name() + "]", e);
        }
        return is;
    }

    private void parseNameUsage(InputStream resource, NameUsageListener nameUsageListener) throws PropertyEnricherException {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource));

            String line;
            skipFirstLine(reader);
            while ((line = reader.readLine()) != null) {
                if (!StringUtils.contains(line, "incertae_sedis")) {
                    parseLine(nameUsageListener, line);
                }
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse [Open Tree of Life] taxon dump", e);
        }
    }

    private void parseSynonym(NameUsageListener nameUsageListener, String line) {
        String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "\t|\t");
        if (rowValues.length > 2) {
            String completeName = rowValues[0];
            String acceptedTaxonIdString = rowValues[1];
            Long acceptedTaxonId = StringUtils.isNumeric(acceptedTaxonIdString) ? Long.parseLong(acceptedTaxonIdString) : null;

            String sourceIds = rowValues[4];
            if (StringUtils.isNotBlank(sourceIds)) {
                String[] ids = StringUtils.split(sourceIds, ',');
                for (String id : ids) {
                    String externalId = StringUtils.upperCase(id);
                    if (isIdSchemeSupported(externalId)) {
                        handleSynonym(nameUsageListener, new TaxonImpl(null, externalId), acceptedTaxonId);
                    }
                }
            }

            handleSynonym(nameUsageListener, new TaxonImpl(completeName, null), acceptedTaxonId);

        }
    }

    private void handleSynonym(NameUsageListener nameUsageListener, TaxonImpl taxon, Long parentTaxId) {
        nameUsageListener.handle(
                NameType.SYNONYM_OF,
                null,
                parentTaxId,
                taxon);
    }


    private void parseLine(NameUsageListener nameUsageListener, String line) {
        String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "\t|\t");
        if (rowValues.length > 3) {
            String taxonIdString = rowValues[0];
            String parentTaxonIdString = rowValues[1];

            Long childTaxonId = StringUtils.isNumeric(taxonIdString) ? Long.parseLong(taxonIdString) : null;
            Long parentTaxonId = StringUtils.isNumeric(parentTaxonIdString) ? Long.parseLong(parentTaxonIdString) : null;


            String completeName = rowValues[2];
            String rank = rowValues[3];

            registerSourceIds(rowValues[4], childTaxonId);

            String idPrefix = getTaxonomyProvider().getIdPrefix();

            TaxonImpl taxon = new TaxonImpl(completeName, idPrefix + taxonIdString);
            taxon.setRank(StringUtils.equals(StringUtils.trim(rank), "no rank") ? "" : rank);

            nameUsageListener.handle(null,
                    childTaxonId,
                    parentTaxonId,
                    taxon);

        }
    }

    private void registerSourceIds(String sourceIds, Long childTaxonId) {
        if (StringUtils.isNotBlank(sourceIds)) {
            String[] ids = StringUtils.split(sourceIds, ',');
            for (String id : ids) {
                String externalId = StringUtils.upperCase(id);
                if (isIdSchemeSupported(externalId)) {
                    appendIdMap(childTaxonId, externalId);
                }
            }
        }
    }

    interface NameUsageListener {
        void handle(NameType status, Long childTaxId, Long parentTaxId, Taxon taxon);
    }

    private void skipFirstLine(BufferedReader reader) throws IOException {
        reader.readLine();
    }

    private URI getResource(String resourceName) {
        String propertyValue = getCtx().getProperty("nomer.ott." + resourceName);
        return URI.create(propertyValue);
    }

    private class NameUsageListenerImpl implements NameUsageListener {
        private final Map<Long, Map<String, String>> nodes;
        private final Map<Long, Long> childParent;
        private final Map<String, List<Long>> name2ids;

        public NameUsageListenerImpl(Map<Long, Map<String, String>> nodes,
                                     Map<Long, Long> childParent,
                                     Map<String, List<Long>> name2ids) {
            this.nodes = nodes;
            this.childParent = childParent;
            this.name2ids = name2ids;
        }

        @Override
        public void handle(NameType status, Long childTaxId, Long parentTaxId, Taxon taxon) {
            registerIdForName(childTaxId, taxon, this.name2ids);
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
