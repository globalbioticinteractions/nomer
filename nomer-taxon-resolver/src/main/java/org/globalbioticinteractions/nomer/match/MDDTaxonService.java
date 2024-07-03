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
import org.eol.globi.taxon.Capitalizer;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MDDTaxonService extends CommonStringTaxonService {
    private static final Logger LOG = LoggerFactory.getLogger(MDDTaxonService.class);

    private static final List<String> RANKS = Arrays.asList(StringUtils.split("subclass\tinfraclass\tmagnorder\tsuperorder\torder\tsuborder\tinfraorder\tparvorder\tsuperfamily\tfamily\tsubfamily\ttribe\tgenus\tsubgenus\tspecificEpithet", "\t"));

    public MDDTaxonService(TermMatcherContext ctx) {
        super(ctx);
    }

    @Override
    public TaxonomyProvider getTaxonomyProvider() {
        return TaxonomyProvider.MAMMAL_DIVERSITY_DATABASE;
    }

    void parseNodes(Map<String, Map<String, String>> taxonMap,
                    Map<String, List<String>> name2nodeIds,
                    BTreeMap<String, String> mergedNodes,
                    InputStream is) throws PropertyEnricherException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        CSVParser parser;
        try {
            parser = CSVFormat.EXCEL.withFirstRecordAsHeader().parse(reader);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to access taxonomic resource", e);
        }

        for (CSVRecord record : parser) {

            String taxonId = record.get("id");

            String authorityAuthor = record.get("authoritySpeciesAuthor");
            String authorityYear = record.get("authoritySpeciesYear");
            String parenthesis = record.get("authorityParentheses");

            String originalName = replaceUnderscoresWithSpaces(record, "originalNameCombination");

            String authorship = authorityAuthor + ", " + authorityYear;

            String completeName = replaceUnderscoresWithSpaces(record, "sciName");

            TaxonImpl taxon = new TaxonImpl(completeName);

            String genus = record.get("genus");
            String specificEpithet = record.get("specificEpithet");

            Stream<String> pathStream = RANKS
                    .stream()
                    .map(rank -> {
                        String value = record.get(rank);
                        if (StringUtils.equalsIgnoreCase("specificEpithet", rank)) {
                            value = StringUtils.lowerCase(value);
                        } else {
                            value = Capitalizer.capitalize(StringUtils.lowerCase(value));
                        }

                        return StringUtils.equalsIgnoreCase(value, "NA")
                                ? ""
                                : StringUtils.defaultIfBlank(value, "");
                    });

            Stream<String> pathAuthorshipStream = RANKS
                    .stream()
                    .map(rank -> {
                        return "";
                    });

            taxon.setPath(pathStream.collect(Collectors.joining(CharsetConstant.SEPARATOR)));
            taxon.setPathNames(String.join(CharsetConstant.SEPARATOR, RANKS));
            taxon.setPathAuthorships(pathAuthorshipStream.collect(Collectors.joining(CharsetConstant.SEPARATOR)));


            String id = "https://www.mammaldiversity.org/explore.html#genus=" + genus + "&species=" + specificEpithet + "&id=" + taxonId;
            taxon.setExternalUrl(id);
            taxon.setExternalId(id);

            if (StringUtils.isNotBlank(authorship)) {
                if (!StringUtils.equals(parenthesis, "0")) {
                    taxon.setAuthorship("(" + authorship + ")");
                } else {
                    taxon.setAuthorship(authorship);
                }
            }

            if (NumberUtils.isCreatable(taxonId)) {
                registerTaxon(taxonMap, name2nodeIds, id, taxon);
                if (StringUtils.isNotBlank(originalName)
                        && !StringUtils.equals(originalName, completeName)) {
                    registerIdForName(id, new TaxonImpl(originalName), name2nodeIds);
                }

                // register subspecies
                String nominalNames = record.get("nominalNames");
                List<String> subspecies = Arrays.asList(StringUtils.split(nominalNames, CharsetConstant.SEPARATOR_CHAR));
                Stream<Taxon> subspeciesTaxa = subspecies
                        .stream()
                        .map(subspecificEpithetAndAuthor -> {
                            String[] s = subspecificEpithetAndAuthor.split(" ");
                            String subspecificEpithet = s[0];
                            String subspecificAuthorship = StringUtils.trim(RegExUtils.replaceFirst(subspecificEpithetAndAuthor, subspecificEpithet, ""));
                            Taxon subspecificTaxon = TaxonUtil.copy(taxon);
                            subspecificTaxon.setName(taxon.getName() + " " + subspecificEpithet);
                            subspecificTaxon.setPath(taxon.getPath() + CharsetConstant.SEPARATOR + subspecificEpithet);
                            subspecificTaxon.setPathNames(taxon.getPathNames() + CharsetConstant.SEPARATOR + "subspecificEpithet");
                            subspecificTaxon.setAuthorship(subspecificAuthorship);
                            String suspecificId = taxon.getExternalId() + "&subspecies=" + subspecificEpithet;
                            subspecificTaxon.setExternalId(suspecificId);
                            subspecificTaxon.setExternalUrl(suspecificId);
                            return subspecificTaxon;
                        });

                subspeciesTaxa.forEach(t -> {
                    registerTaxon(taxonMap, name2nodeIds, t.getExternalId(), t);
                });

            }


        }
    }

    private void registerTaxon(Map<String, Map<String, String>> taxonMap,
                               Map<String, List<String>> name2nodeIds,
                               String taxonId,
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
                .keySerializer(BTreeKeySerializer.STRING)
                .valueSerializer(Serializer.JAVA)
                .make();

        childParent = db
                .createTreeMap(CHILD_PARENT)
                .keySerializer(BTreeKeySerializer.STRING)
                .valueSerializer(Serializer.LONG)
                .make();

        name2nodeIds = db
                .createTreeMap(NAME_TO_NODE_IDS)
                .keySerializer(BTreeKeySerializer.STRING)
                .valueSerializer(Serializer.JAVA)
                .make();

        mergedNodes = db
                .createTreeMap(MERGED_NODES)
                .keySerializer(BTreeKeySerializer.STRING)
                .valueSerializer(Serializer.STRING)
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
    protected boolean shouldResolveHierarchy(Map<String, String> childParent, Taxon resolvedTaxon) {
        return false;
    }

    private URI getNodesUrl() throws PropertyEnricherException {
        return CacheUtil.getValueURI(getCtx(), "nomer.mdd.url");
    }
}
