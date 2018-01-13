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
}
