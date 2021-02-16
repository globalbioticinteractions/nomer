package org.eol.globi.service;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Table;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.eol.globi.taxon.TaxonLookupBuilder;
import org.eol.globi.taxon.TaxonLookupService;
import org.globalbioticinteractions.nomer.util.CacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.taxon.PropertyEnricherSimple;
import org.eol.globi.taxon.TaxonLookupServiceImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class UKSISuggestionService extends PropertyEnricherSimple implements NameSuggester, Initializing {
    private static final Logger LOG = LoggerFactory.getLogger(UKSISuggestionService.class);

    private TaxonLookupService service;

    @Override
    public String suggest(String name) {
        String suggestion = null;
        try {
            Taxon match = findMatch(name);
            suggestion = match == null ? name : match.getName();
        } catch (PropertyEnricherException e) {
            LOG.warn("failed to find suggestion for name [" + name + "]", e);
        }
        return suggestion;
    }

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enrichedProperties = new HashMap<String, String>(properties);
        Taxon match = findMatch(enrichedProperties.get(PropertyAndValueDictionary.NAME));
        if (match != null) {
            enrichedProperties.put(PropertyAndValueDictionary.NAME, match.getName());
            enrichedProperties.put(PropertyAndValueDictionary.EXTERNAL_ID, match.getExternalId());
        }
        return Collections.unmodifiableMap(enrichedProperties);
    }

    private Taxon findMatch(String taxonName) throws PropertyEnricherException {
        Taxon match = null;
        try {
            if (service == null) {
                init();
            }
            Taxon[] taxonTerms = service.lookupTermsByName(taxonName);
            if (taxonTerms.length > 0) {
                // pick the first one
                match = taxonTerms[0];
            }
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to lookup [" + taxonName + "]", e);
        }
        return match;
    }

    @Override
    public void shutdown() {
        if (null != service) {
            service = null;
        }
    }

    private void init() {
        try {
            init(getClass().getResourceAsStream("/org/eol/globi/data/uksi/NfWD.mdb.gz"));
        } catch (IOException e) {
            throw new IllegalArgumentException("failed to initialize [" + getClass().getSimpleName() + "]", e);
        }
    }

    public void init(InputStream resourceStream) throws IOException {
        LOG.info("[" + UKSISuggestionService.class.getSimpleName() + "] instantiating...");

        File tmpFile = null;

        File indexPath = CacheUtil.createTmpCacheDir();
        Directory directory = CacheUtil.luceneDirectoryFor(indexPath);

        try (TaxonLookupBuilder taxonLookupBuilder = new TaxonLookupBuilder(directory)) {
            taxonLookupBuilder.start();
            InputStream is = new GZIPInputStream(resourceStream);
            tmpFile = File.createTempFile("NfWD", "mdb");
            FileOutputStream fileOutputStream = new FileOutputStream(tmpFile);
            IOUtils.copy(is, fileOutputStream);

            Database db = DatabaseBuilder.open(tmpFile);

            Table names = db.getTable("NAMESERVER_FOR_WIDER_DELIVERY");
            for (Map<String, Object> study : names) {
                Object taxonName = study.get("TAXON_NAME");
                Object externalId = study.get("NBN_TAXON_VERSION_KEY");
                Object recommendedScientificName = study.get("RECOMMENDED_SCIENTIFIC_NAME");
                Taxon taxonTerm = new TaxonImpl();
                taxonTerm.setExternalId("UKSI:" + externalId);
                taxonTerm.setName(recommendedScientificName.toString());
                taxonLookupBuilder.addTerm(taxonName.toString(), taxonTerm);
            }
            taxonLookupBuilder.finish();
            service = new TaxonLookupServiceImpl(CacheUtil.luceneDirectoryFor(indexPath));
        } finally {
            FileUtils.deleteQuietly(tmpFile);
        }

        LOG.info("[" + UKSISuggestionService.class.getSimpleName() + "] instantiated.");
    }

}
