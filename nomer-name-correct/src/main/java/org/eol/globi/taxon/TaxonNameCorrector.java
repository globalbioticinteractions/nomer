package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.NameSuggester;
import org.eol.globi.service.PropertyEnricherException;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class TaxonNameCorrector implements CorrectionService, TermMatcher {

    private static final Logger LOG = LoggerFactory.getLogger(TaxonNameCorrector.class);
    private final TermMatcherContext ctx;

    private List<NameSuggester> suggestors = null;

    public TaxonNameCorrector() {
        this(null);
    }

    public TaxonNameCorrector(TermMatcherContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String correct(String taxonName) {
        String suggestion = "";
        if (StringUtils.isNotBlank(taxonName)) {
            suggestion = suggestCorrection(taxonName);
        }
        return suggestion;
    }

    private String suggestCorrection(String taxonName) {
        String suggestion;
        if (suggestors == null) {
            setSuggestors(SuggesterFactory.createSuggesterEnsemble(ctx));
        }
        List<String> suggestions = new ArrayList<>();
        suggestion = taxonName;
        suggestions.add(suggestion);
        boolean isCircular = false;
        while (!isCircular) {
            String newSuggestion = suggest(suggestion);
            if (StringUtils.equals(newSuggestion, suggestion)) {
                break;
            } else if (suggestions.contains(newSuggestion)) {
                isCircular = true;
                suggestions.add(newSuggestion);
                LOG.warn("found circular suggestion path " + suggestions + ": choosing original [" + taxonName + "] instead");
            } else {
                suggestions.add(newSuggestion);
                suggestion = newSuggestion;

            }
        }
        suggestion = isCircular ? suggestions.get(0) : suggestions.get(suggestions.size() - 1);
        return suggestion;
    }

    private String suggest(String nameSuggestion) {
        for (NameSuggester suggestor : suggestors) {
            nameSuggestion = StringUtils.trim(suggestor.suggest(nameSuggestion));
            if (StringUtils.length(nameSuggestion) < 2) {
                nameSuggestion = "";
                break;
            }
        }
        return nameSuggestion;
    }


    @Override
    public void match(List<Term> terms, TermMatchListener listener) throws PropertyEnricherException {
        Stream<TermImpl> correctedTerms = terms.stream()
                .map(term -> new TermImpl(term.getId(), correct(term.getName())));
        correctedTerms.forEach(term -> {
            listener.foundTaxonForTerm(null,
                    term,
                    NameType.SAME_AS,
                    new TaxonImpl(term.getName(), term.getId())
            );
        });

    }


    public void setSuggestors(List<NameSuggester> suggestors) {
        this.suggestors = suggestors;
    }
}
