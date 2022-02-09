package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.util.ExternalIdUtil;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Map;

public class CatalogueOfLifeTaxonService extends CommonTaxonService<String> {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogueOfLifeTaxonService.class);


    public CatalogueOfLifeTaxonService(TermMatcherContext ctx) {
        super(ctx);
    }

    @Override
    public TaxonomyProvider getTaxonomyProvider() {
        return TaxonomyProvider.CATALOGUE_OF_LIFE;
    }

    @Override
    public String getIdOrNull(String key, TaxonomyProvider matchingTaxonomyProvider) {
        TaxonomyProvider taxonomyProvider = ExternalIdUtil.taxonomyProviderFor(key);
        String idString = ExternalIdUtil.stripPrefix(matchingTaxonomyProvider, key);
        return (matchingTaxonomyProvider.equals(taxonomyProvider)
                && StringUtils.isNoneBlank(idString))
                ? idString
                : null;
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
                .compressionEnable()
                .closeOnJvmShutdown()
                .transactionDisable()
                .make();

        if (db.exists(DENORMALIZED_NODES)
                && db.exists(DENORMALIZED_NODE_IDS)) {
            LOG.info("[Catalogue of Life] taxonomy already indexed at [" + taxonomyDir.getAbsolutePath() + "], no need to import.");
            denormalizedNodes = db.getTreeMap(DENORMALIZED_NODES);
            denormalizedNodeIds = db.getTreeMap(DENORMALIZED_NODE_IDS);
        } else {
            LOG.info("[" + getTaxonomyProvider().name() + "] taxonomy importing...");
            StopWatch watch = new StopWatch();
            watch.start();

            InputStream resource;
            try {
                resource = getCtx().retrieve(getNameUsageResource());
                if (resource == null) {
                    throw new PropertyEnricherException("Catalogue of Life init failure: failed to find [" + getNameUsageResource() + "]");
                }
            } catch (IOException e) {
                throw new PropertyEnricherException("failed to parse [" + getTaxonomyProvider().name() + "]", e);
            }

            BTreeMap<String, Map<String, String>> nodes = db
                    .createTreeMap("nodes")
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();

            BTreeMap<String, String> childParent = db
                    .createTreeMap("childParent")
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();

            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resource));

                String line;
                skipFirstLine(reader);
                while ((line = reader.readLine()) != null) {
                    String[] rowValues = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "\t");
                    if (rowValues.length > 8) {
                        String taxId = rowValues[0];
                        String parentTaxId = rowValues[2];
                        String rank = rowValues[7];

                        String completeName = rowValues[5];

                        TaxonImpl taxon = new TaxonImpl(completeName, getTaxonomyProvider().getIdPrefix() + taxId);
                        taxon.setRank(StringUtils.equals(StringUtils.trim(rank), "no rank") ? "" : rank);
                        if (StringUtils.isNoneBlank(taxId)) {
                            ((Map<String, Map<String, String>>) nodes).put(taxId, TaxonUtil.taxonToMap(taxon));
                            if (StringUtils.isNoneBlank(parentTaxId)) {
                                ((Map<String, String>) childParent).put(
                                        taxId,
                                        parentTaxId
                                );
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new PropertyEnricherException("failed to parse [Catalogue of Life] taxon dump", e);
            }

            denormalizedNodes = db
                    .createTreeMap(DENORMALIZED_NODES)
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();

            denormalizedNodeIds = db
                    .createTreeMap(DENORMALIZED_NODE_IDS)
                    .keySerializer(BTreeKeySerializer.STRING)
                    .make();


            denormalizeTaxa(
                    nodes,
                    denormalizedNodes,
                    denormalizedNodeIds,
                    childParent);

            watch.stop();
            TaxonCacheService.logCacheLoadStats(watch.getTime(), nodes.size(), LOG);
            LOG.info("[" + getTaxonomyProvider().name() + "] taxonomy imported.");

        }
    }

    private void skipFirstLine(BufferedReader reader) throws IOException {
        reader.readLine();
    }

    private URI getNameUsageResource() {
        String propertyValue = getCtx().getProperty("nomer.col.name_usage");
        return URI.create(propertyValue);
    }
}
