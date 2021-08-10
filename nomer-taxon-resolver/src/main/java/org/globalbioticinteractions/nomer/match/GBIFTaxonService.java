package org.globalbioticinteractions.nomer.match;

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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.GBIFService;
import org.eol.globi.taxon.PropertyEnricherSimple;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GBIFTaxonService extends PropertyEnricherSimple implements TermMatcher {

    private static final Logger LOG = LoggerFactory.getLogger(GBIFTaxonService.class);
    private static final String DENORMALIZED_NODES = "denormalizedNodes";
    private static final String NAME_IDS = "nameIds";
    private static final String SYNONYM_IDS = "synonymIds";

    private final TermMatcherContext ctx;

    private BTreeMap<String, List<String>> nameIds = null;
    private BTreeMap<String, List<String>> synonymIds = null;
    private BTreeMap<String, Map<String, String>> gbifDenormalizedNodes = null;

    public GBIFTaxonService(TermMatcherContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void match(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException {
        lazyInitIfNeeded();

        for (Term term : terms) {
            List<Taxon> matchedTaxa = new ArrayList<>();
            List<Taxon> matchedSynonyms = new ArrayList<>();

            String gbifId = GBIFService.getGBIFId(term.getId());
            if (StringUtils.isNotBlank(gbifId)) {
                Map<String, String> taxonMap = lookupTaxonById(gbifId);
                if (taxonMap != null) {
                    matchedTaxa.add(TaxonUtil.mapToTaxon(taxonMap));
                }
            } else if (StringUtils.isNotBlank(term.getName())) {
                List<Map<String, String>> matched = lookupTaxaByName(term.getName());
                matched.forEach(taxonForName -> {
                    matchedTaxa.add(TaxonUtil.mapToTaxon(taxonForName));
                });

                List<Map<String, String>> synonyms = lookupTaxaBySynonym(term.getName());
                synonyms.forEach(taxonForName -> {
                    matchedSynonyms.add(TaxonUtil.mapToTaxon(taxonForName));
                });                
            }


            if (matchedTaxa.isEmpty() && matchedSynonyms.isEmpty()) {
                termMatchListener.foundTaxonForTerm(null, term, new TaxonImpl(term.getName(), term.getId()), NameType.NONE);
            } else {
                matchedTaxa.forEach(matchedTerm -> {
                    termMatchListener.foundTaxonForTerm(null, term, matchedTerm, NameType.SAME_AS);
                });
                matchedSynonyms.forEach(matchedTerm -> {
                    termMatchListener.foundTaxonForTerm(null, term, matchedTerm, NameType.SYNONYM_OF);
                });
            }
        }
    }

    private List<Map<String, String>> lookupTaxaBySynonym(String name) {
        List<Map<String, String>> matches = new ArrayList<>();
        List<String> ids = synonymIds.get(name);
        if (ids != null) {
            for (String id : ids) {
                Map<String, String> taxonMap = gbifDenormalizedNodes.get(id);
                if (taxonMap != null) {
                    matches.add(taxonMap);
                }
            }
        }
        return matches;
    }   

    private List<Map<String, String>> lookupTaxaByName(String name) {
        List<Map<String, String>> matches = new ArrayList<>();
        List<String> ids = nameIds.get(name);
        if (ids != null) {
            for (String id : ids) {
                Map<String, String> taxonMap = gbifDenormalizedNodes.get(id);
                if (taxonMap != null) {
                    matches.add(taxonMap);
                }
            }
        }
        return matches;
    }

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        String gbifTaxonId = GBIFService.getGBIFTaxonId(properties);
        Map<String, String> enriched = lookupTaxonById(gbifTaxonId);
        return enriched == null
                ? properties
                : enriched;
    }

    private Map<String, String> lookupTaxonById(String gbifTaxonId) throws PropertyEnricherException {
        Map<String, String> enrichedProperties = null;
        if (StringUtils.isNotBlank(gbifTaxonId)) {
            lazyInitIfNeeded();
            String externalId = TaxonomyProvider.ID_PREFIX_GBIF + gbifTaxonId;
            enrichedProperties = gbifDenormalizedNodes.get(externalId);
        }
        return enrichedProperties;
    }

    private void lazyInitIfNeeded() throws PropertyEnricherException {
        if (needsInit()) {
            lazyInit();
        }
    }

    private void lazyInit() throws PropertyEnricherException {
    	File cacheDir = getCacheDir();
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                throw new PropertyEnricherException("failed to create cache dir at [" + cacheDir.getAbsolutePath() + "]");
            }
        }
        
        File gbifTaxonomyDir = new File(getCacheDir(), "gbif");
        DB db = DBMaker
                .newFileDB(gbifTaxonomyDir)
                .mmapFileEnableIfSupported()
                .compressionEnable()
                .closeOnJvmShutdown()
                .transactionDisable()
                .make();

        if (db.exists(DENORMALIZED_NODES)) {
            LOG.info("GBIF taxonomy already indexed at [" + gbifTaxonomyDir.getAbsolutePath() + "], no need to import.");
            gbifDenormalizedNodes = db.getTreeMap(DENORMALIZED_NODES);
            nameIds = db.getTreeMap(NAME_IDS);
            synonymIds = db.getTreeMap(SYNONYM_IDS);
        } else {
            LOG.info("GBIF taxonomy importing...");
            StopWatch watch = new StopWatch();
            watch.start();

            BTreeMap<String, Map<String, String>> gbifNodes = db
                    .createTreeMap("nodes")
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();

            BTreeMap<String, String> childParent = db
                    .createTreeMap("childParent")
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();
            
            nameIds = db
                    .createTreeMap(NAME_IDS)
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();

            synonymIds = db
                    .createTreeMap(SYNONYM_IDS)
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();           
            
            try {
                String taxonUrl = ctx.getProperty("nomer.gbif.taxon");
                if (taxonUrl == null) {
                    throw new PropertyEnricherException("no url for taxon resource [nomer.gbif.taxon] found");
                }
                InputStream resource = ctx.getResource(taxonUrl);
                parseNodes(gbifNodes, childParent, resource, nameIds, synonymIds);
            } catch (IOException e) {
                throw new PropertyEnricherException("failed to parse GBIF nodes", e);
            }                     
                                

            gbifDenormalizedNodes = db
                    .createTreeMap(DENORMALIZED_NODES)
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();
            denormalizeTaxa(gbifNodes, gbifDenormalizedNodes, childParent);

            watch.stop();
            TaxonCacheService.logCacheLoadStats(watch.getTime(), gbifNodes.size(), LOG);
            LOG.info("GBIF taxonomy imported.");
        }
    }

    private boolean needsInit() {
    	return gbifDenormalizedNodes == null;
    }

    @Override
    public void shutdown() {

    } 
    
    private File getCacheDir() {
        File cacheDir = new File(ctx.getCacheDir(), "gbif");
        cacheDir.mkdirs();
        return cacheDir;
    }    


    static void parseNodes(Map<String, Map<String, String>> taxonMap, Map<String, String> childParent, InputStream resourceAsStream, BTreeMap<String,List<String>> names, BTreeMap<String,List<String>> synonyms) throws PropertyEnricherException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "\t");
                if (rowValues.length > 19) {
                	String taxId = rowValues[0];                
                	String parentTaxId = rowValues[1];
                	String rank = rowValues[5];
                	
                	boolean isSynomym = StringUtils.equals("t", rowValues[3]);
                	
                	String canonicalName = rowValues[19];                	
                	String externalId = TaxonomyProvider.ID_PREFIX_GBIF + taxId;
                	TaxonImpl taxon = new TaxonImpl(canonicalName, externalId);
                	taxon.setRank(StringUtils.equals(StringUtils.trim(rank), "UNRANKED") ? "" : rank);
                	taxon.setExternalId(externalId);
                	
                	taxonMap.put(externalId, TaxonUtil.taxonToMap(taxon));
                	childParent.put(TaxonomyProvider.ID_PREFIX_GBIF + taxId, TaxonomyProvider.ID_PREFIX_GBIF + parentTaxId);
                	
                	if (!isSynomym) {                        
                        addIdMapEntry(names, canonicalName, taxId);
                    } else {                        
                        addIdMapEntry(synonyms, canonicalName, taxId);
                    }
                }
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse GBIF taxon dump", e);
        }                    
    }

    static void denormalizeTaxa(Map<String, Map<String, String>> taxonMap, Map<String, Map<String, String>> taxonMapDenormalized, Map<String, String> childParent) {
        Set<Map.Entry<String, Map<String, String>>> taxa = taxonMap.entrySet();
        for (Map.Entry<String, Map<String, String>> taxon : taxa) {
            denormalizeTaxa(taxonMap, taxonMapDenormalized, childParent, taxon);
        }
    }

    private static void denormalizeTaxa(Map<String, Map<String, String>> taxonMap, Map<String, Map<String, String>> taxonEnrichMap, Map<String, String> childParent, Map.Entry<String, Map<String, String>> taxon) {
        Map<String, String> childTaxon = taxon.getValue();
        List<String> pathNames = new ArrayList<>();
        List<String> pathIds = new ArrayList<>();
        List<String> path = new ArrayList<>();

        Taxon origTaxon = TaxonUtil.mapToTaxon(childTaxon);                
        path.add(StringUtils.defaultIfBlank(origTaxon.getName(), ""));
        
        String externalId = origTaxon.getExternalId();
        pathIds.add(StringUtils.defaultIfBlank(externalId, ""));

        origTaxon.setRank(origTaxon.getRank());
        pathNames.add(StringUtils.defaultIfBlank(origTaxon.getRank(), ""));

        String parent = childParent.get(taxon.getKey());
        while (StringUtils.isNotBlank(parent) && !pathIds.contains(parent)) {
            Map<String, String> stringStringMap = taxonMap.get(parent);
            if (stringStringMap != null) {
                Taxon parentTaxon = TaxonUtil.mapToTaxon(stringStringMap);
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

        taxonEnrichMap.put(taxon.getKey(), TaxonUtil.taxonToMap(origTaxon));
    }

    static void parseNames(InputStream resourceAsStream, Map<String, String> nameMap,
                           Map<String, List<String>> nameIds,
                           Map<String, List<String>> commonNameIds,
                           Map<String, List<String>> synonymIds) throws PropertyEnricherException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "\t");
                if (rowValues.length > 3) {
                    String taxId = rowValues[0];
                    String taxonName = rowValues[18];
                    String rank = rowValues[5];
                    boolean isSynomym = StringUtils.equals("t", rowValues[3]);
                    String gbifTaxonId = TaxonomyProvider.ID_PREFIX_GBIF + taxId;
                    
                    if (!isSynomym) {                        
                        nameMap.put(gbifTaxonId, taxonName);
                        addIdMapEntry(nameIds, taxonName, gbifTaxonId);
                    } else {                        
                        addIdMapEntry(synonymIds, taxonName, gbifTaxonId);
                    }

                }
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse GBIF taxon dump", e);
        }
    }

    private static void addIdMapEntry(Map<String, List<String>> nameIds,
                                      String taxonName,
                                      String key) {
        List<String> ids = nameIds.get(taxonName);
        if (ids == null) {
            ids = new ArrayList<>();
        }
        if (!ids.contains(key)) {
            ids.add(key);
        }
        nameIds.put(taxonName, ids);
    }

    static void parseMerged(Map<String, String> mergedMap, InputStream resourceAsStream) throws PropertyEnricherException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "\t");
                if (rowValues.length > 1) {
                    String oldTaxId = rowValues[0];
                    String newTaxId = rowValues[1];
                    if (StringUtils.isNotBlank(oldTaxId) && StringUtils.isNotBlank(newTaxId)) {
                        mergedMap.put(TaxonomyProvider.ID_PREFIX_GBIF + oldTaxId, TaxonomyProvider.ID_PREFIX_GBIF + newTaxId);
                    }
                }
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse GBIF taxon dump", e);
        }
    }


}
