package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.TaxonNameCorrector;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public class TermMatcherCorrectFactory implements TermMatcherFactory {

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new TaxonNameCorrector(ctx);
    }

    @Override
    public String getName() {
        return "globi-correct";
    }

    @Override
    public String getDescription() {
        return "Scrubs names using GloBI's (taxonomic) name scrubber. Scrubbing includes removing of stopwords (e.g., undefined), correcting common typos using a \"crappy\" names list, parse to canonical name using gnparser (see https://github.com/GlobalNamesArchitecture/gnparser), and more.";
    }
}
