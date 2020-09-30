package org.globalbioticinteractions.nomer.match;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.PropertyEnricherSimple;
import org.eol.globi.taxon.TaxonCacheListener;
import org.eol.globi.taxon.TaxonCacheService;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@PropertyEnricherInfo(name = "plazi", description = "Lookup Plazi taxon treatment by name or id using offline-enabled database dump")
public class PlaziService extends PropertyEnricherSimple {

    private static final Log LOG = LogFactory.getLog(PlaziService.class);
    private static final String TREATMENTS = "treatments";


    private final TermMatcherContext ctx;

    private BTreeMap<String, Map<String, String>> treatments = null;

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
            Map<String, String> enrichedProperties = treatments.get(name);
            enriched = enrichedProperties == null ? enriched : new TreeMap<>(enrichedProperties);
        }
        return enriched;
    }


    static void parseNodes(Map<String, Map<String, String>> taxonMap,
                           Map<String, String> childParent,
                           Map<String, String> rankIdNameMap,
                           InputStream resourceAsStream) throws PropertyEnricherException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "|");
                if (rowValues.length > 24) {
                    String taxId = rowValues[0];
                    String parentTaxId = rowValues[17];
                    String rankKingdomId = rowValues[20];
                    String rankId = rowValues[21];
                    String rankKey = rankKingdomId + "-" + rankId;
                    String rank = rankIdNameMap.getOrDefault(rankKey, rankKey);

                    String completeName = rowValues[25];

                    String externalId = TaxonomyProvider.ID_PREFIX_ITIS + taxId;
                    TaxonImpl taxon = new TaxonImpl(completeName, externalId);
                    taxon.setRank(StringUtils.equals(StringUtils.trim(rank), "no rank") ? "" : rank);
                    taxonMap.put(externalId, TaxonUtil.taxonToMap(taxon));
                    childParent.put(
                            TaxonomyProvider.ID_PREFIX_ITIS + taxId,
                            TaxonomyProvider.ID_PREFIX_ITIS + parentTaxId
                    );
                }
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to parse ITIS taxon dump", e);
        }
    }


    private void lazyInit() throws PropertyEnricherException {
        File cacheDir = getCacheDir(this.ctx);
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                throw new PropertyEnricherException("failed to create cache dir at [" + cacheDir.getAbsolutePath() + "]");
            }
        }

        File taxonomyDir = new File(cacheDir, "plazi");
        DB db = DBMaker
                .newFileDB(taxonomyDir)
                .mmapFileEnableIfSupported()
                .compressionEnable()
                .closeOnJvmShutdown()
                .transactionDisable()
                .make();

        if (db.exists(TREATMENTS)) {
            LOG.info("Plazi taxonomy already indexed at [" + taxonomyDir.getAbsolutePath() + "], no need to import.");
            treatments = db.getTreeMap(TREATMENTS);
        } else {
            LOG.info("Plazi treatments importing...");
            StopWatch watch = new StopWatch();
            watch.start();

            treatments = db
                    .createTreeMap(TREATMENTS)
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();

            try {
                InputStream resource = this.ctx.getResource(getArchiveUrl());
                TaxonCacheListener listener = new TaxonCacheListener() {

                    @Override
                    public void start() {

                    }

                    @Override
                    public void addTaxon(Taxon taxon) {
                        treatments.put(taxon.getName(), TaxonUtil.taxonToMap(taxon));
                    }

                    @Override
                    public void finish() {

                    }
                };
                ArchiveInputStream archiveInputStream = new ArchiveStreamFactory()
                        .createArchiveInputStream(resource);

                ArchiveEntry nextEntry;
                while ((nextEntry = archiveInputStream.getNextEntry()) != null) {
                    if (!nextEntry.isDirectory() && StringUtils.endsWith(nextEntry.getName(), ".ttl")) {
                        PlaziTreatmentsLoader.importTreatment(archiveInputStream, listener);
                    }
                }


            } catch (IOException e) {
                throw new PropertyEnricherException("failed to load archive", e);
            } catch (ArchiveException e) {
                throw new PropertyEnricherException("failed to load archive", e);
            }


            watch.stop();
            TaxonCacheService.logCacheLoadStats(watch.getTime(), treatments.size(), LOG);
            LOG.info("Plazi treatments imported.");
        }
    }

    private boolean needsInit() {
        return treatments == null;
    }

    @Override
    public void shutdown() {

    }

    private File getCacheDir(TermMatcherContext ctx) {
        File cacheDir = new File(ctx.getCacheDir(), "plazi");
        cacheDir.mkdirs();
        return cacheDir;
    }

    private String getArchiveUrl() throws PropertyEnricherException {
        return ctx.getProperty("nomer.plazi.treatments.archive");
    }

}
