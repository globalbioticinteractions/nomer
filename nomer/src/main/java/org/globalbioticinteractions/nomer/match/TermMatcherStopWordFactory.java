package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.SuggesterFactory;
import org.eol.globi.taxon.TaxonNameCorrector;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.util.Arrays;
import java.util.Collections;

public class TermMatcherStopWordFactory implements TermMatcherFactory {

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new TaxonNameCorrector(ctx) {{
            setSuggestors(Collections.singletonList(SuggesterFactory.createStopwordRemover(ctx)));
        }};
    }

    @Override
    public String getName() {
        return "remove-stop-words";
    }

    @Override
    public String getDescription() {
        return "Removes stop words (e.g., undefined) using a stop word list specified by property [" + SuggesterFactory.NOMER_TAXON_NAME_STOPWORD_URL + "] .";
    }
}
