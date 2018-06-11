package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.GlobalNamesService2;
import org.eol.globi.taxon.GlobalNamesSources2;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.util.Arrays;

public class TermMatcherFactoryGlobalNames implements TermMatcherFactory {

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new GlobalNamesService2(Arrays.asList(GlobalNamesSources2.values()));
    }

    @Override
    public String getName() {
        return "globi-globalnames";
    }

    @Override
    public String getDescription() {
        return "Uses https://resolver.globalnames.org to match taxon names. Searches by name only (not id).";
    }
}
