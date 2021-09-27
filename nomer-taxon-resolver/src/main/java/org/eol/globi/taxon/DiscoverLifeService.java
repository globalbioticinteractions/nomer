package org.eol.globi.taxon;

import org.apache.http.client.cache.Resource;
import org.globalbioticinteractions.nomer.util.PropertyEnricherInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@PropertyEnricherInfo(name = "discoverlife-taxon", description = "Look up taxa of https://discoverlife.org by name or id with DL:* prefix.")
public class DiscoverLifeService extends OfflineService {

    @Override
    protected TaxonomyImporter createTaxonomyImporter() {

        return new TaxonomyImporter(
                new TaxonParser() {
                    @Override
                    public void parse(InputStream is, TaxonImportListener listener) throws IOException {

                    }
                },
                new TaxonReaderFactory() {
                    @Override
                    public Map<String, Resource> getResources() throws IOException {
                        return null;
                    }
                }
        );
    }

}
