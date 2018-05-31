package org.eol.globi.taxon;

import org.globalbioticinteractions.nomer.util.PropertyEnricherInfo;

@PropertyEnricherInfo(name ="gulfbase-taxon", description = "look up taxa of https://gulfbase.org by name or id with prefix BioGoMx:")
public class GulfBaseService extends OfflineService {

    @Override
    protected TaxonomyImporter createTaxonomyImporter() {
        return new TaxonomyImporter(new GulfBaseTaxonParser(), new GulfBaseTaxonReaderFactory());
    }

}
