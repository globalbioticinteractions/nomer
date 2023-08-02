package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.TaxonNameSuggestor;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public class TermMatcherSuggestFactory implements TermMatcherFactory {

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new TaxonNameSuggestor(ctx);
    }

    @Override
    public String getPreferredName() {
        return "globi-suggest";
    }

    @Override
    public String getDescription() {
        return "Scrubs names using GloBI's (taxonomic) name scrubber. Scrubbing includes removing of stopwords (e.g., undefined), correcting common typos using a \"crappy\" names list, parse to canonical name using gnparser (see https://github.com/GlobalNamesArchitecture/gnparser), and more.";
    }
}
