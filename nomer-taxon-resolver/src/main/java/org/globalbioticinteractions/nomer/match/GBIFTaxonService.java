package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.GBIFUtil;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.util.CSVTSVUtil;
import org.globalbioticinteractions.nomer.util.CacheUtil;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.eol.globi.taxon.GBIFUtil.compileAuthorshipString;

public class GBIFTaxonService extends CommonLongTaxonService {

    private static final Logger LOG = LoggerFactory.getLogger(GBIFTaxonService.class);

    public GBIFTaxonService(TermMatcherContext ctx) {
        super(ctx);
    }

    @Override
    public TaxonomyProvider getTaxonomyProvider() {
        return TaxonomyProvider.GBIF;
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


            URI gbifNameResource = CacheUtil.getValueURI(getCtx(), "nomer.gbif.ids");

            watch.start();
            nodes = db
                    .createTreeMap(NODES)
                    .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                    .valueSerializer(Serializer.JAVA)
                    .make();


            InputStream retrieve = null;
            try {
                retrieve = getCtx().retrieve(gbifNameResource);
                if (retrieve == null) {
                    throw new PropertyEnricherException("failed to retrieve [" + gbifNameResource + "]");
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                retrieve,
                                StandardCharsets.UTF_8
                        )
                );

                String line;
                while ((line = reader.readLine()) != null) {
                    handleLine(line);
                }
            } catch (IOException e) {
                throw new PropertyEnricherException("failed to retrieve [" + gbifNameResource + "]");
            }
            watch.stop();
            TaxonCacheService.logCacheLoadStats(watch.getTime(), nodes.size(), LOG);
            LOG.info("[" + getTaxonomyProvider().name() + "] taxonomy imported.");
        }

    }

    private void handleLine(String line) {
        String[] rowValues = CSVTSVUtil.splitTSV(line);
        Taxon taxon;

        if (rowValues.length > 19) {
            String taxIdString = rowValues[0];
            String relatedTaxIdString = rowValues[1];
            String rank = StringUtils.trim(rowValues[5]);
            String canonicalName = rowValues[19];

            taxon = new TaxonImpl(canonicalName, TaxonomyProvider.ID_PREFIX_GBIF + taxIdString);
            taxon.setRank(StringUtils.lowerCase(rank));
            if (rowValues.length > 27) {
                String authorshipString = compileAuthorshipString(rowValues[26], rowValues[27]);
                if (StringUtils.isBlank(authorshipString)) {
                    authorshipString = compileAuthorshipString(rowValues[24], rowValues[25]);
                } else {
                    authorshipString = "(" + authorshipString + ")";
                }
                taxon.setAuthorship(authorshipString);
            }


            long taxId = Long.parseLong(taxIdString);
            nodes.put(taxId, TaxonUtil.taxonToMap(taxon));

            if (StringUtils.isNumeric(relatedTaxIdString)) {
                long relatedTaxId = Long.parseLong(relatedTaxIdString);
                if (GBIFUtil.isSynonym(rowValues)) {
                    mergedNodes.put(taxId, relatedTaxId);
                } else {
                    childParent.put(taxId, relatedTaxId);
                }
            }

            List<Long> ids = name2nodeIds.get(taxon.getName());
            List<Long> idsNew = ids == null
                    ? new ArrayList<>()
                    : new ArrayList<>(ids);
            idsNew.add(taxId);
            name2nodeIds.put(taxon.getName(), idsNew);
        }
    }


}
