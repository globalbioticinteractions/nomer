package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public class TermMatcherGBIFParseFactory implements TermMatcherFactory {

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        String ignoreAbbreviationValue = StringUtils.defaultIfBlank(ctx.getProperty("nomer.parser.gbif.ignoreAbbreviations"), "true");
        return new ParserServiceGBIF(StringUtils.equalsIgnoreCase(ignoreAbbreviationValue, "true"));
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
