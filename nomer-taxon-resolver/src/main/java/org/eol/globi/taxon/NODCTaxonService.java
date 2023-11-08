package org.eol.globi.taxon;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.globalbioticinteractions.nomer.util.CacheUtil;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.CacheServiceUtil;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.globalbioticinteractions.nomer.util.PropertyEnricherInfo;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

@PropertyEnricherInfo(name = "nodc-taxon-id", description = "Lookup taxon in the Taxonomic Code of the National Oceanographic Data Center (NODC) by id with prefix NODC: . Maps to ITIS terms if possible.")
public class NODCTaxonService extends PropertyEnricherSimple {
    private static final Logger LOG = LoggerFactory.getLogger(NODCTaxonService.class);
    public static final String NODC_2_ITIS = "nodc2itis";
    private final TermMatcherContext ctx;

    private BTreeMap<String, String> nodc2itis = null;
    private PropertyEnricher itisService = new ITISService();

    public NODCTaxonService(TermMatcherContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        String externalId = properties.get(PropertyAndValueDictionary.EXTERNAL_ID);
        Map<String, String> enriched = new TreeMap<String, String>(properties);
        String nodcPrefix = TaxonomyProvider.NATIONAL_OCEANOGRAPHIC_DATA_CENTER.getIdPrefix();
        if (StringUtils.startsWith(externalId, nodcPrefix)) {
            if (needsInit()) {
                lazyInit();
            }
            String tsn = nodc2itis.get(externalId);
            tsn = StringUtils.startsWith(tsn, nodcPrefix) ? nodc2itis.get(tsn) : tsn;

            if (StringUtils.isNotBlank(tsn)) {
                enriched.put(PropertyAndValueDictionary.EXTERNAL_ID, tsn);
                enriched = itisService.enrichFirstMatch(enriched);
            }
        }
        return enriched;
    }

    private boolean needsInit() {
        return nodc2itis == null;
    }

    private URI getNodcResourceUrl() throws PropertyEnricherException {
        return CacheUtil.getValueURI(ctx, "nomer.nodc.url");
    }

    private void lazyInit() throws PropertyEnricherException {
        try {
            InputStream retrieve = ctx.retrieve(getNodcResourceUrl());
            NODCTaxonParser parser = new NODCTaxonParser(new BufferedReader(new InputStreamReader(retrieve)));
            init(parser);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to read from NODC resource [" + getNodcResourceUrl() + "]", e);
        }
    }

    protected void init(NODCTaxonParser parser) throws PropertyEnricherException {
        try {
            CacheServiceUtil.createCacheDir(getCacheDir());
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to initialize", e);
        }

        DB db = DBMaker
                .newFileDB(new File(getCacheDir(), "nodcLookup"))
                .mmapFileEnableIfSupported()
                .mmapFileCleanerHackDisable()
                .closeOnJvmShutdown()
                .transactionDisable()
                .make();

        nodc2itis = db.exists(NODC_2_ITIS)
                ? db.getTreeMap(NODC_2_ITIS)
                : createTreeMap(parser, db);
    }

    private static BTreeMap<String, String> createTreeMap(NODCTaxonParser parser, DB db) {
        LOG.info("NODC taxonomy importing...");
        StopWatch watch = new StopWatch();
        watch.start();
        BTreeMap<String, String> aMap = db
                .createTreeMap(NODC_2_ITIS)
                .pumpSource(parser)
                .pumpPresort(100000)
                .pumpIgnoreDuplicates()
                .keySerializer(BTreeKeySerializer.STRING)
                .valueSerializer(Serializer.STRING)
                .make();
        watch.stop();
        TaxonCacheService.logCacheLoadStats(watch.getTime(), aMap.size(), LOG);
        LOG.info("NODC taxonomy imported.");
        return aMap;
    }

    @Override
    public void shutdown() {
        if (nodc2itis != null) {
            TaxonCacheService.close(nodc2itis.getEngine());
            nodc2itis = null;
        }
        if (ctx != null && StringUtils.isNotEmpty(ctx.getCacheDir())) {
            FileUtils.deleteQuietly(getCacheDir());
        }
    }

    private File getCacheDir() {
        File cacheDir = new File(ctx.getCacheDir(), "nodc");
        cacheDir.mkdirs();
        return cacheDir;
    }
}
