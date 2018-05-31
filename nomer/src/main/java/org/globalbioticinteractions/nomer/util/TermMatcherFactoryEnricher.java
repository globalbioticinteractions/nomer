package org.globalbioticinteractions.nomer.util;

import org.eol.globi.service.PropertyEnricherFactory;
import org.eol.globi.taxon.GlobalNamesService;
import org.eol.globi.taxon.GlobalNamesSources;
import org.eol.globi.taxon.TermMatcher;

import java.util.Arrays;

public class TermMatcherFactoryEnricher implements TermMatcherFactory {

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return PropertyEnricherFactory.createTaxonMatcher(ctx);
    }

    @Override
    public String getName() {
        return "globi-enrich";
    }

    @Override
    public String getDescription() {
        return "Uses GloBI's taxon enricher to find first term match by id or name. Needs internet connection to work. Uses various web apis like Encyclopedia of Life, World Registry of Marine Species (WoRMS), Integrated Taxonomic Information System (ITIS), National Biodiversity Network (NBN) and more.";
    }
}
