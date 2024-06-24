package org.globalbioticinteractions.nomer.match;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
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
import org.eol.globi.util.ExternalIdUtil;
import org.globalbioticinteractions.nomer.util.CacheUtil;
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
import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HesperomysTaxonService extends CommonLongTaxonService {
    private static final Logger LOG = LoggerFactory.getLogger(HesperomysTaxonService.class);

    private static final List<String> RANKS = Arrays.asList(StringUtils.split("class_\torder\tfamily\tgenus\tspecies\tsubspecies", "\t"));

    public HesperomysTaxonService(TermMatcherContext ctx) {
        super(ctx);
    }

    @Override
    public TaxonomyProvider getTaxonomyProvider() {
        return TaxonomyProvider.HESPEROMYS;
    }

    void parseNodes(Map<Long, Map<String, String>> taxonMap,
                    Map<String, List<Long>> name2nodeIds,
                    BTreeMap<Long, Long> mergedNodes,
                    InputStream is) throws PropertyEnricherException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        CSVParser parser;
        try {
            parser = CSVFormat
                    .EXCEL
                    .withFirstRecordAsHeader()
                    .withCommentMarker('#')
                    .parse(reader);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to access taxonomic resource", e);
        }

        for (CSVRecord record : parser) {
                parseRecord(taxonMap, name2nodeIds, record);
        }
    }

    private void parseRecord(
            Map<Long, Map<String, String>> taxonMap,
            Map<String, List<Long>> name2nodeIds,
            CSVRecord record) {
        String taxonId = record.get("id");

        String authorityAuthor = record.get("authors");
        String authorityYear = record.get("year");

        String originalName = replaceUnderscoresWithSpaces(record, "original_name");

        String authorship = authorityAuthor + ", " + authorityYear;

        Taxon taxon = new TaxonImpl();

        Stream<String> pathStream = RANKS
                .stream()
                .map(rank -> {
                    String rankValue = record.get(rank);
                    String value = "";
                    if (StringUtils.isNotBlank(rankValue)) {
                        String[] words = StringUtils.split(rankValue, ' ');
                        value = words[words.length-1];
                    }
                    return value;
                });

        Stream<String> pathAuthorshipStream = RANKS
                .stream()
                .map(rank -> "");

        taxon.setPath(pathStream.collect(Collectors.joining(CharsetConstant.SEPARATOR)));
        taxon.setPathAuthorships(pathAuthorshipStream.collect(Collectors.joining(CharsetConstant.SEPARATOR)));
        String rankNames = RANKS
                .stream()
                .map(rank -> StringUtils.remove(rank, '_'))
                .map(rank -> StringUtils.replace(rank, "species", "specificEpithet"))
                .map(rank -> StringUtils.replace(rank, "subspecies", "subspecificEpithet"))
                .collect(Collectors.joining(CharsetConstant.SEPARATOR));
        taxon.setPathNames(rankNames);


        String externalId = TaxonomyProvider.HESPEROMYS.getIdPrefix() + taxonId;
        taxon.setExternalId(externalId);

        Map<String, String> taxon2 = TaxonUtil.toPathNameMap(taxon, taxon.getPath());
        Taxon taxonGen = (TaxonUtil.generateSpecies(taxon2, "genus", "specificepithet", "subspecificepithet", ""));

        taxon.setName(taxonGen.getName());
        taxon.setRank(taxonGen.getRank());

        taxon.setAuthorship(authorship);

        if (NumberUtils.isCreatable(taxonId)) {
            Long id = Long.parseLong(taxonId);
            registerTaxon(taxonMap, name2nodeIds, id, taxon);
            if (StringUtils.isNotBlank(originalName)
                    && !StringUtils.equals(originalName, taxon.getName())) {
                registerIdForName(id, new TaxonImpl(originalName), name2nodeIds);
            }

            // register synonym
            if (!StringUtils.equalsIgnoreCase(taxon.getName(), originalName)) {
                registerIdForName(id, originalName, name2nodeIds);
            }
        }
    }

    private void registerTaxon(Map<Long, Map<String, String>> taxonMap,
                               Map<String, List<Long>> name2nodeIds,
                               Long taxonId,
                               Taxon taxon) {
        registerIdForName(taxonId, taxon, name2nodeIds);
        taxonMap.put(taxonId, TaxonUtil.taxonToMap(taxon));
    }

    private String replaceUnderscoresWithSpaces(CSVRecord labeledCSVParser, String sciName1) {
        String sciName = labeledCSVParser.get(sciName1);
        return replaceUnderscoresWithSpaces(sciName);
    }

    private String replaceUnderscoresWithSpaces(String sciName) {
        return StringUtils.replace(sciName, "_", " ");
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
                && db.exists(NAME_TO_NODE_IDS)) {
            LOG.debug("ITIS taxonomy already indexed at [" + taxonomyDir.getAbsolutePath() + "], no need to import.");
            nodes = db.getTreeMap(NODES);
            childParent = db.getTreeMap(CHILD_PARENT);
            mergedNodes = db.getTreeMap(MERGED_NODES);
            name2nodeIds = db.getTreeMap(NAME_TO_NODE_IDS);
        } else {
            index(db);
        }
    }

    private void index(DB db) throws PropertyEnricherException {
        LOG.info("MDD taxonomy importing...");
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

        mergedNodes = db
                .createTreeMap(MERGED_NODES)
                .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                .valueSerializer(Serializer.LONG)
                .make();


        try {
            parseNodes(
                    nodes,
                    name2nodeIds,
                    mergedNodes,
                    getCtx().retrieve(getNodesUrl())
            );
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse ITIS nodes", e);
        }

    }

    @Override
    protected boolean shouldResolveHierarchy(Map<Long, Long> childParent, Taxon resolvedTaxon) {
        return false;
    }

    private URI getNodesUrl() throws PropertyEnricherException {
        return CacheUtil.getValueURI(getCtx(), "nomer.hesperomys.url");
    }
}
