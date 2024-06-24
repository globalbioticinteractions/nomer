package org.eol.globi.taxon;

import org.globalbioticinteractions.nomer.util.PropertyEnricherInfo;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

@PropertyEnricherInfo(name = "gulfbase-taxon", description = "Look up taxa of https://gulfbase.org by name or id with BioGoMx:* prefix.")
public class GulfBaseService extends OfflineService {

    private final TermMatcherContext ctx;

    public GulfBaseService(TermMatcherContext ctx) {
        this.ctx = ctx;
    }


    @Override
    protected TaxonomyImporter createTaxonomyImporter() {
        return new TaxonomyImporter(
                ctx,
                new GulfBaseTaxonParser(),
                new GulfBaseTaxonReaderFactory()
        );
    }

    @Override
    public void shutdown() {

    }
}
