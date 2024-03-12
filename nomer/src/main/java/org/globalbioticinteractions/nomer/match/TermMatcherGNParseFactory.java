package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public class TermMatcherGNParseFactory implements TermMatcherFactory {

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new ParserServiceGlobalNames();
    }

    @Override
    public String getPreferredName() {
        return "gn-parse";
    }

    @Override
    public String getDescription() {
        return "Attempts extract canonical taxonomic name from name string using https://github.com/GlobalNamesArchitecture/gnparser .";
    }
}
