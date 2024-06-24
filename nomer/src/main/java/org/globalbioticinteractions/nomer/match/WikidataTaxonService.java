package org.globalbioticinteractions.nomer.match;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.LineReader;
import org.apache.commons.lang3.RegExUtils;
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
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.util.ExternalIdUtil;
import org.globalbioticinteractions.nomer.util.CacheUtil;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.globalbioticinteractions.wikidata.WikidataUtil;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class WikidataTaxonService extends CommonStringTaxonService {
    private static final Logger LOG = LoggerFactory.getLogger(WikidataTaxonService.class);

    private static final String NAME_TO_EXTERNAL_IDS = "node2externalId";


    private BTreeMap<String, String> externalIdNodes = null;

    public WikidataTaxonService(TermMatcherContext ctx) {
        super(ctx);
    }


    @Override
    public String getIdOrNull(Taxon key, TaxonomyProvider matchingTaxonomyProvider) {
        return key.getExternalId();
    }


    @Override
    public TaxonomyProvider getTaxonomyProvider() {
        return TaxonomyProvider.WIKIDATA;
    }

    void parseNodes(Map<String, Map<String, String>> taxonMap,
                    Map<String, String> childParent,
                    Map<String, List<String>> name2nodeIds,
                    BTreeMap<String, String> mergedNodes,
                    InputStream is) throws PropertyEnricherException {
        try {
            InputStreamReader readerInputStream
                    = new InputStreamReader(is, StandardCharsets.UTF_8);

            LineReader reader = new LineReader(readerInputStream);


            String line;
            while ((line = reader.readLine()) != null) {
                String trimmedLine = RegExUtils.replaceAll(line, "[ ]*,[ ]*$", "");
                trimmedLine = RegExUtils.replaceAll(trimmedLine, "^[ ]*[\\[\\]][ ]*", "");
                if (StringUtils.isNotBlank(trimmedLine)) {
                    JsonNode jsonNode = new ObjectMapper().readTree(trimmedLine);
                    Taxon taxon = parseTaxon(jsonNode);
                    String wikidataItemId = ExternalIdUtil.stripPrefix(TaxonomyProvider.WIKIDATA, taxon.getExternalId());
                    if (StringUtils.isNotBlank(wikidataItemId)) {
                        Map<String, String> taxonPropertyMap = TaxonUtil.taxonToMap(taxon);
                        nodes.put(taxon.getExternalId(), taxonPropertyMap);
                        List<String> relatedIds = parseRelatedIds(jsonNode);
                        for (String relatedId : relatedIds) {
                            mergedNodes.put(relatedId, taxon.getExternalId());
                        }

                        if (StringUtils.isNotBlank(taxon.getName())) {
                            List<String> nodeIds = name2nodeIds.getOrDefault(taxon.getExternalId(), new ArrayList<>());
                            nodeIds.add(taxon.getExternalId());
                            name2nodeIds.put(taxon.getName(), nodeIds);
                        }
                        JsonNode parentId = getParentId(jsonNode);
                        if (!parentId.isMissingNode()) {
                            String parentIdString = parentId.asText();
                            if (StringUtils.isNotBlank(parentIdString)) {
                                childParent.putIfAbsent(wikidataItemId, parentIdString);
                            }
                        }
                    }
                }
            }

        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse Wikidata taxon dump", e);
        }
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
                && db.exists(NAME_TO_EXTERNAL_IDS)
                && db.exists(MERGED_NODES)
                && db.exists(NAME_TO_NODE_IDS)
        ) {
            LOG.debug("Wikidata taxonomy already indexed at [" + taxonomyDir.getAbsolutePath() + "], no need to import.");
            nodes = db.getTreeMap(NODES);
            childParent = db.getTreeMap(CHILD_PARENT);
            name2nodeIds = db.getTreeMap(NAME_TO_NODE_IDS);
            mergedNodes = db.getTreeMap(MERGED_NODES);
            externalIdNodes = db.getTreeMap(NAME_TO_EXTERNAL_IDS);
        } else {
            index(db);
        }
    }

    private void index(DB db) throws PropertyEnricherException {
        LOG.info("Wikidata taxonomy importing...");
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

        name2nodeIds = db
                .createTreeMap(NAME_TO_NODE_IDS)
                .keySerializer(BTreeKeySerializer.STRING)
                .valueSerializer(Serializer.JAVA)
                .make();

        mergedNodes = db
                .createTreeMap(NAME_TO_EXTERNAL_IDS)
                .keySerializer(BTreeKeySerializer.STRING)
                .valueSerializer(Serializer.STRING)
                .make();

        try {
            parseNodes(
                    nodes,
                    childParent,
                    name2nodeIds,
                    mergedNodes,
                    getCtx().retrieve(getNodesUrl())
            );
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse Wikidata nodes", e);
        }

        watch.stop();
        TaxonCacheService.logCacheLoadStats(watch.getTime(), nodes.size(), LOG);
        LOG.info("[" + getTaxonomyProvider().name() + "] taxonomy imported.");
    }

    @Override
    public void shutdown() {

    }

    @Override
    boolean isIdSchemeSupported(String externalId) {
        return ExternalIdUtil.taxonomyProviderFor(externalId) != null;
    }


    private URI getNodesUrl() throws PropertyEnricherException {
        return CacheUtil.getValueURI(getCtx(), "nomer.wikidata.items");
    }

    public static List<String> parseRelatedIds(JsonNode jsonNode) {
        List<String> relatedIds = new ArrayList<>();

        JsonNode externalIds = jsonNode.at("/claims");
        for (JsonNode externalId : externalIds) {
            JsonNode dataType = externalId.at("/0/mainsnak/datatype");
            if (!dataType.isMissingNode()) {
                if (StringUtils.equals("external-id", dataType.asText())) {
                    JsonNode externalIdScheme = externalId.at("/0/mainsnak/property");
                    if (!externalIdScheme.isMissingNode()) {
                        String externalIdSchemeValue = externalIdScheme.asText();
                        TaxonomyProvider taxonomyProvider
                                = WikidataUtil.WIKIDATA_TO_PROVIDER.get(externalIdSchemeValue);
                        if (taxonomyProvider != null) {
                            JsonNode identifier = externalId.at("/0/mainsnak/datavalue/value");
                            if (!identifier.isMissingNode()) {
                                relatedIds.add(taxonomyProvider.getIdPrefix() + identifier.asText());
                            }
                        }
                    }
                }
            }
        }
        return relatedIds;
    }

    public static Taxon parseTaxon(JsonNode jsonNode) {
        Taxon taxon = new TaxonImpl();

        JsonNode at = getId(jsonNode);
        if (!at.isMissingNode()) {
            taxon.setExternalId("WD:" + at.asText());
        }

        JsonNode labels = jsonNode.at("/claims/P1843");
        List<String> commonNames = new ArrayList<>();
        for (JsonNode label : labels) {
            JsonNode value = label.at("/mainsnak/datavalue/value");
            commonNames.add(value.get("text").asText() + " @" + value.get("language").asText());
        }

        taxon.setCommonNames(StringUtils.join(commonNames, CharsetConstant.SEPARATOR));

        JsonNode name = jsonNode.at("/claims/P225/0/mainsnak/datavalue/value");
        if (!name.isMissingNode()) {
            taxon.setName(name.asText());
        }

        JsonNode parentId = getParentId(jsonNode);
        if (!parentId.isMissingNode()) {
            taxon.setPathIds("WD:" + parentId.asText() + CharsetConstant.SEPARATOR + taxon.getId());
        }

        JsonNode thumbnailUrl = jsonNode.at("/claims/P18/0/mainsnak/datavalue/value");
        if (!thumbnailUrl.isMissingNode()) {
            String s = StringUtils.replace(thumbnailUrl.asText(), " ", "_");
            taxon.setThumbnailUrl("https://commons.wikimedia.org/wiki/File:" + s);
        }
        return taxon;
    }

    private static JsonNode getId(JsonNode jsonNode) {
        return jsonNode.at("/id");
    }

    private static JsonNode getParentId(JsonNode jsonNode) {
        return jsonNode.at("/claims/P171/0/mainsnak/datavalue/value/id");
    }

}
