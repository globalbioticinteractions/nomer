package org.eol.globi.taxon;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.CacheService;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;

public class NODCTaxonService implements PropertyEnricher {
    private static final Log LOG = LogFactory.getLog(NODCTaxonService.class);
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
                enriched = itisService.enrich(enriched);
            }
        }
        return enriched;
    }

    public boolean needsInit() {
        return nodc2itis == null;
    }

    private String getNodcResourceUrl() throws PropertyEnricherException {
        return ctx.getProperty("nomer.nodc.url");
    }

    private void lazyInit() throws PropertyEnricherException {
        String nodcFilename = getNodcResourceUrl();
        if (StringUtils.isBlank(getNodcResourceUrl())) {
            throw new PropertyEnricherException("cannot initialize NODC enricher: failed to find NODC taxon file. Did you install the NODC taxonomy and set -DnodcFile=...?");
        }
        try {
            NODCTaxonParser parser = new NODCTaxonParser(new BufferedReader(new InputStreamReader(ctx.getResource(nodcFilename))));
            init(parser);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to read from NODC resource [" + nodcFilename + "]", e);
        }
    }

    protected void init(NODCTaxonParser parser) throws PropertyEnricherException {
        CacheService.createCacheDir(getCacheDir());

        LOG.info("NODC taxonomy importing...");
        StopWatch watch = new StopWatch();
        watch.start();

        DB db = DBMaker
                .newFileDB(new File(getCacheDir(), "nodcLookup"))
                .mmapFileEnableIfSupported()
                .closeOnJvmShutdown()
                .transactionDisable()
                .make();

        nodc2itis = db
                .createTreeMap("nodc2itis")
                .pumpSource(parser)
                .pumpPresort(100000)
                .pumpIgnoreDuplicates()
                .keySerializer(BTreeKeySerializer.STRING)
                .make();

        watch.stop();
        TaxonCacheService.logCacheLoadStats(watch.getTime(), nodc2itis.size(), LOG);
        LOG.info("NODC taxonomy imported.");
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
