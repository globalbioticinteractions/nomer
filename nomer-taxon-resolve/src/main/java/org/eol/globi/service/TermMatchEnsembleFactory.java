package org.eol.globi.service;

import org.eol.globi.taxon.AtlasOfLivingAustraliaService;
import org.eol.globi.taxon.EOLService;
import org.eol.globi.taxon.EOLService2;
import org.eol.globi.taxon.EnvoService;
import org.eol.globi.taxon.FunctionalGroupService;
import org.eol.globi.taxon.GBIFService;
import org.eol.globi.taxon.GulfBaseService;
import org.eol.globi.taxon.INaturalistTaxonService;
import org.eol.globi.taxon.ITISService;
import org.eol.globi.taxon.NBNService;
import org.eol.globi.taxon.NCBIService;
import org.eol.globi.taxon.NODCTaxonService;
import org.eol.globi.taxon.TaxonEnricherImpl;
import org.eol.globi.taxon.TermMatcher;
import org.eol.globi.taxon.WoRMSService;
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
                add(new NCBIService());
                add(new GBIFService());
                add(new INaturalistTaxonService());
                add(new EOLService2());
                add(new WoRMSService());
                add(new GulfBaseService());
                add(new AtlasOfLivingAustraliaService());
            }
        };
    }

}
