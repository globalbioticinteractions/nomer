package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.GBIFParserCanon;
import org.eol.globi.taxon.GlobalNamesCanon;
import org.eol.globi.taxon.TaxonNameCorrector;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.util.Collections;

public class TermMatcherGBIFParseFactory implements TermMatcherFactory {

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new TaxonNameCorrector(ctx) {{
            setSuggestors(Collections.singletonList(new GBIFParserCanon()));
        }};
    }

    @Override
    public String getPreferredName() {
        return "gbif-parse";
    }

    @Override
    public String getDescription() {
        return "Attempts extract canonical taxonomic name from name string using https://github.com/gbif/name-parser .";
    }
}
