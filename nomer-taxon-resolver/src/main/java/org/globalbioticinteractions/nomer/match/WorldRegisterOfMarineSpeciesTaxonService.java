package org.globalbioticinteractions.nomer.match;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.domain.TermImpl;
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
import java.util.Objects;

public class WorldRegisterOfMarineSpeciesTaxonService extends CommonTaxonService<Long> {
    private static final Logger LOG = LoggerFactory.getLogger(WorldRegisterOfMarineSpeciesTaxonService.class);

    private static final List<String> uncheckedStatus = Arrays.asList(
            "unassessed",
            "nomen dubium",
            "uncertain",
            "taxon inquirendum",
            "temporary name",
            "nomen nudum",
            "interim unpublished",
            "nomen oblitum"
    );

    private static final List<String> synonymStatus = Arrays.asList(
            "junior objective synonym",
            "junior subjective synonym",
            "unaccepted"
    );

    public static final String TAXON_ID = "http://rs.tdwg.org/dwc/terms/taxonID";
    public static final String PARENT_NAME_USAGE_ID = "http://rs.tdwg.org/dwc/terms/parentNameUsageID";
    public static final String ACCEPTED_NAME_USAGE_ID = "http://rs.tdwg.org/dwc/terms/acceptedNameUsageID";
    public static final String TAXONOMIC_STATUS = "http://rs.tdwg.org/dwc/terms/taxonomicStatus";
    public static final String SCIENTIFIC_NAME = "http://rs.tdwg.org/dwc/terms/scientificName";
    public static final String SCIENTIFIC_NAME_AUTHORSHIP = "http://rs.tdwg.org/dwc/terms/scientificNameAuthorship";
    public static final String TAXON_RANK = "http://rs.tdwg.org/dwc/terms/taxonRank";

    public static final List<String> requiredFields =
            Arrays.asList(
                    TAXON_ID,
                    PARENT_NAME_USAGE_ID,
                    ACCEPTED_NAME_USAGE_ID,
                    TAXONOMIC_STATUS,
                    SCIENTIFIC_NAME,
                    SCIENTIFIC_NAME_AUTHORSHIP,
                    TAXON_RANK
            );


    public WorldRegisterOfMarineSpeciesTaxonService(TermMatcherContext ctx) {
        super(ctx);
    }

    @Override
    public TaxonomyProvider getTaxonomyProvider() {
        return TaxonomyProvider.WORMS;
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
        File taxonomyDir = new File(getCacheDir(), StringUtils.lowerCase(getTaxonomyProvider().name()));
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
            LOG.debug("[" + getTaxonomyProvider().name() + "] taxonomy already indexed at [" + taxonomyDir.getAbsolutePath() + "], no need to import.");
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


    @Override
    protected NameType getNameTypeFor(Taxon taxon) {
        return taxon.getStatus() == null
                ? defaultRelation()
                : getNameType(taxon.getStatus().getName()
        );
    }

    private NameType defaultRelation() {
        return NameType.HAS_ACCEPTED_NAME;
    }


    private InputStream getClassificationStream() throws PropertyEnricherException {
        InputStream resource;
        try {
            resource = getCtx().retrieve(getClassificationResource());
            if (resource == null) {
                throw new PropertyEnricherException("World Register of Marine Species init failure: failed to find [" + getClassificationResource() + "]");
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
            while ((line = reader.readLine()) != null) {
                parseLine(nameUsageListener, line);
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse [Catalogue of Life] taxon dump", e);
        }
    }

    private void parseLine(NameUsageListener<Long> nameUsageListener, String line) throws IOException {

        JsonNode jsonNode = new ObjectMapper().readTree(line);

        long numberOfMissingFields = requiredFields
                .stream()
                .filter(fieldName -> !jsonNode.has(fieldName))
                .count();

        if (numberOfMissingFields == 0) {
            parseLine(nameUsageListener, jsonNode);
        }


    }

    private void parseLine(NameUsageListener<Long> nameUsageListener, JsonNode jsonNode) {
        String taxIdString = pruneTaxonId(jsonNode, TAXON_ID);
        Long taxId = getIdAsLong(taxIdString);
        String parentTaxIdString = pruneTaxonId(jsonNode, PARENT_NAME_USAGE_ID);
        Long parentTaxId = getIdAsLong(parentTaxIdString);
        String acceptedNameUsageIdString = pruneTaxonId(jsonNode, ACCEPTED_NAME_USAGE_ID);
        Long acceptedNameUsageId = getIdAsLong(acceptedNameUsageIdString);
        String completeName = removeQuotes(jsonNode.get(SCIENTIFIC_NAME).asText(""));
        String authorship = removeQuotes(jsonNode.get(SCIENTIFIC_NAME_AUTHORSHIP).asText(""));
        String rank = StringUtils.lowerCase(jsonNode.get(TAXON_RANK).asText(""));

        String idPrefix = getTaxonomyProvider().getIdPrefix();
        TaxonImpl taxon = new TaxonImpl(completeName, idPrefix + taxIdString);
        taxon.setAuthorship(StringUtils.trim(authorship));

        String status = jsonNode.get(TAXONOMIC_STATUS).asText("");
        taxon.setStatus(new TermImpl(null, getNameType(status).name()));

        taxon.setRank(StringUtils.equals(StringUtils.trim(rank), "no rank") ? "" : rank);

        nameUsageListener.handle(
                status,
                taxId,
                parentTaxId,
                acceptedNameUsageId,
                taxon
        );
    }

    private NameType getNameType(String statusValue) {
        NameType nameType = defaultRelation();
        if (synonymStatus.contains(statusValue)) {
            nameType = NameType.SYNONYM_OF;
        } else if (uncheckedStatus.contains(statusValue)) {
            nameType = NameType.HAS_UNCHECKED_NAME;
        }
        return nameType;
    }


    private String pruneTaxonId(JsonNode jsonNode, String taxonIdName) {
        JsonNode jsonNode1 = jsonNode.get(taxonIdName);
        return jsonNode1 == null
                ? null
                : StringUtils.remove(jsonNode1.asText(""), "urn:lsid:marinespecies.org:taxname:");
    }

    private String removeQuotes(String rowValue) {
        return RegExUtils.removeAll(rowValue, "(^\"|\"$)");
    }

    private static Long getIdAsLong(String stripped) {
        return NumberUtils.isDigits(stripped) ? Long.parseLong(stripped) : null;
    }

    interface NameUsageListener<T> {
        void handle(String status, T childTaxId, T parentTaxId, T acceptedNameUsage, Taxon taxon);
    }

    private URI getClassificationResource() {
        String propertyValue = getCtx().getProperty("nomer.worms.url");
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
        public void handle(String status, Long childTaxId, Long parentTaxId, Long acceptedNameUsageId, Taxon taxon) {
            registerIdForName(childTaxId, taxon, WorldRegisterOfMarineSpeciesTaxonService.this.name2nodeIds);

            if (childTaxId != null
                    && acceptedNameUsageId != null
                    && !childTaxId.equals(acceptedNameUsageId)) {
                mergedNodes.put(childTaxId, acceptedNameUsageId);
            }

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
