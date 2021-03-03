package org.globalbioticinteractions.nomer.match;

import org.globalbioticinteractions.nomer.util.PropertyEnricherInfo;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

@PropertyEnricherInfo(name = "ncbi-taxon-id", description = "Lookup NCBI taxon by id with NCBI:* prefix.")
public class NCBITaxonIdService extends NCBITaxonService {

    public NCBITaxonIdService(TermMatcherContext ctx) {
        super(ctx);
    }
}
