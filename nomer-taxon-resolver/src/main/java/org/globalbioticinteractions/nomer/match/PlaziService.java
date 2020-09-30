package org.globalbioticinteractions.nomer.match;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.PropertyEnricherSimple;
import org.eol.globi.taxon.TaxonCacheListener;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.taxon.TaxonLookupServiceImpl;
import org.globalbioticinteractions.nomer.util.PropertyEnricherInfo;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

@PropertyEnricherInfo(name = "plazi", description = "Lookup Plazi taxon treatment by name or id using offline-enabled database dump")
public class PlaziService extends PropertyEnricherSimple {

    private static final Log LOG = LogFactory.getLog(PlaziService.class);

    private final TermMatcherContext ctx;

    private TaxonLookupServiceImpl taxonLookupService = null;

    public PlaziService(TermMatcherContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enriched = new TreeMap<>(properties);
        String name = properties.get(PropertyAndValueDictionary.NAME);
        if (StringUtils.isNotBlank(name)) {
            if (needsInit()) {
                if (ctx == null) {
                    throw new PropertyEnricherException("context needed to initialize");
                }
                lazyInit();
            }
            Map<String, String> enrichedProperties;
            try {
                Taxon[] taxons = taxonLookupService.lookupTermsByName(name);
                enrichedProperties = taxons != null && taxons.length > 0
                        ? TaxonUtil.taxonToMap(taxons[0])
                        : null;
            } catch (IOException e) {
                throw new PropertyEnricherException("failed to lookup [" + name + "]", e);
            }
            enriched = enrichedProperties == null ? enriched : new TreeMap<>(enrichedProperties);
        }
        return enriched;
    }

    private void lazyInit() throws PropertyEnricherException {
        File cacheDir = getCacheDir(this.ctx);
        boolean preExistingCacheDir = cacheDir.exists();
        if (!preExistingCacheDir) {
            if (!cacheDir.mkdirs()) {
                throw new PropertyEnricherException("failed to create cache dir at [" + cacheDir.getAbsolutePath() + "]");
            }
        }

        try {
            taxonLookupService = new TaxonLookupServiceImpl(new SimpleFSDirectory(cacheDir));
            taxonLookupService.start();
            if (preExistingCacheDir) {
                LOG.info("Plazi taxonomy already indexed at [" + cacheDir.getAbsolutePath() + "], no need to import.");
            } else {
                LOG.info("Plazi treatments importing...");
                StopWatch watch = new StopWatch();
                watch.start();
                AtomicLong counter = new AtomicLong();

                try {
                    InputStream resource = this.ctx.getResource(getArchiveUrl());
                    TaxonCacheListener listener = new TaxonCacheListener() {

                        @Override
                        public void start() {

                        }

                        @Override
                        public void addTaxon(Taxon taxon) {
                            counter.incrementAndGet();
                            taxonLookupService.addTerm(taxon);
                        }

                        @Override
                        public void finish() {

                        }
                    };
                    ArchiveInputStream archiveInputStream = new ZipArchiveInputStream(resource);

                    ArchiveEntry nextEntry;
                    while ((nextEntry = archiveInputStream.getNextEntry()) != null) {
                        if (!nextEntry.isDirectory() && StringUtils.endsWith(nextEntry.getName(), ".ttl")) {
                            CloseShieldInputStream closeShieldInputStream = new CloseShieldInputStream(archiveInputStream);
                            PlaziTreatmentsLoader.importTreatment(closeShieldInputStream, listener);
                        }
                    }


                } catch (IOException e) {
                    throw new PropertyEnricherException("failed to load archive", e);
                }


                watch.stop();
                TaxonCacheService.logCacheLoadStats(watch.getTime(), counter.intValue(), LOG);
                LOG.info("Plazi treatments imported.");
            }
            taxonLookupService.finish();

        } catch (IOException e) {
            throw new PropertyEnricherException("failed to init enricher", e);
        }

    }

    private boolean needsInit() {
        return taxonLookupService == null;
    }

    @Override
    public void shutdown() {

    }

    private File getCacheDir(TermMatcherContext ctx) {
        return new File(ctx.getCacheDir(), "plazi");
    }

    private String getArchiveUrl() throws PropertyEnricherException {
        return ctx.getProperty("nomer.plazi.treatments.archive");
    }

}
