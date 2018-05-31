package org.globalbioticinteractions.nomer.match;

import org.eol.globi.service.TermMatchEnsembleFactory;
import org.eol.globi.taxon.TaxonEnricherImpl;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public class TermMatcherFactoryEnsembleEnricher implements TermMatcherFactory {

    @Override
    public TermMatcher createTermMatcher(final TermMatcherContext ctx) {
        return new TaxonEnricherImpl() {{
            setServices(TermMatchEnsembleFactory.getEnrichers(ctx));
        }};
    }

    @Override
    public String getName() {
        return "globi-enrich";
    }

    @Override
    public String getDescription() {
        return "Uses GloBI's taxon enricher to find first term match by id or name. Uses various web apis like Encyclopedia of Life, World Registry of Marine Species (WoRMS), Integrated Taxonomic Information System (ITIS), National Biodiversity Network (NBN) and more.";
    }
}
