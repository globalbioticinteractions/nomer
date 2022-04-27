package org.eol.globi.service;

import org.eol.globi.taxon.AtlasOfLivingAustraliaService;
import org.eol.globi.taxon.BOLDService;
import org.eol.globi.taxon.EnvoService;
import org.eol.globi.taxon.FunctionalGroupService;
import org.eol.globi.taxon.GBIFService;
import org.eol.globi.taxon.GulfBaseService;
import org.eol.globi.taxon.INaturalistTaxonService;
import org.eol.globi.taxon.ITISService;
import org.eol.globi.taxon.NBNService;
import org.eol.globi.taxon.NCBIService;
import org.eol.globi.taxon.NODCTaxonService;
import org.eol.globi.taxon.ORCIDService;
import org.eol.globi.taxon.TaxonEnricherImpl;
import org.eol.globi.taxon.TermMatcher;
import org.eol.globi.taxon.WoRMSService;
import org.globalbioticinteractions.nomer.match.CatalogueOfLifeTaxonService;
import org.globalbioticinteractions.nomer.match.EOLTaxonService;
import org.globalbioticinteractions.nomer.match.GBIFTaxonService;
import org.globalbioticinteractions.nomer.match.ITISTaxonService;
import org.globalbioticinteractions.nomer.match.IndexFungorumTaxonService;
import org.globalbioticinteractions.nomer.match.NCBITaxonIdService;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.util.ArrayList;
import java.util.Collections;

public class TermMatchEnsembleFactory {

    public static TermMatcher createTermMatcher(final PropertyEnricher enricher) {
        return new TaxonEnricherImpl() {{
            setServices(Collections.singletonList(enricher));
        }};
    }

    public static ArrayList<PropertyEnricher> getEnrichers(TermMatcherContext ctx) {
        return new ArrayList<PropertyEnricher>() {
            {
                add(new EnvoService());
                add(new FunctionalGroupService());
                add(new NBNService());
                add(new NODCTaxonService(ctx));
                add(new ITISService());
                add(new NCBITaxonIdService(ctx));
                add(new ITISTaxonService(ctx));
                add(new IndexFungorumTaxonService(ctx));
                add(new GBIFTaxonService(ctx));
                add(new NCBIService());
                add(new BOLDService());
                add(new EOLTaxonService(ctx));
                add(new GBIFService());
                add(new INaturalistTaxonService());
//                add(new EOLService2());
                add(new WoRMSService());
                add(new GulfBaseService(ctx));
                add(new AtlasOfLivingAustraliaService());
                add(new ITISService());
                add(new CatalogueOfLifeTaxonService(ctx));
                add(new ORCIDService());
            }
        };
    }

}
