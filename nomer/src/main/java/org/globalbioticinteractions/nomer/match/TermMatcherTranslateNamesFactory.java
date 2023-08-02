package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.SuggesterFactory;
import org.eol.globi.taxon.TaxonNameSuggestor;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.util.Collections;

public class TermMatcherTranslateNamesFactory implements TermMatcherFactory {

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new TaxonNameSuggestor(ctx) {{
            setSuggestors(Collections.singletonList(SuggesterFactory.createManualSuggester(ctx)));
        }};
    }

    @Override
    public String getPreferredName() {
        return "translate-names";
    }

    @Override
    public String getDescription() {
        return "Translates incoming names using a two column csv file specified by property [" + SuggesterFactory.NOMER_TAXON_NAME_CORRECTION_URL + "] .";
    }
}
