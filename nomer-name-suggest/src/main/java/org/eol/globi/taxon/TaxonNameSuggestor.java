package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.NameSuggester;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TaxonNameSuggestor extends TaxonNameSuggestorBase implements SuggestionService {

    private static final Logger LOG = LoggerFactory.getLogger(TaxonNameSuggestor.class);

    public TaxonNameSuggestor() {
        super(null);
    }

    public TaxonNameSuggestor(TermMatcherContext ctx) {
        super(ctx);
    }

    @Override
    public String suggest(String taxonName) {
        String suggestion = "";
        if (StringUtils.isNotBlank(taxonName)) {
            suggestion = suggestCorrection(taxonName);
        }
        return suggestion;
    }

    private String suggestCorrection(String taxonName) {
        lazyInit();
        String suggestion;
        List<String> suggestions = new ArrayList<>();
        suggestion = taxonName;
        suggestions.add(suggestion);
        boolean isCircular = false;
        while (!isCircular) {
            String newSuggestion = suggest2(suggestion);
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

    private String suggest2(String nameSuggestion) {
        for (NameSuggester suggestor : getSuggestors()) {
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
        terms.stream()
                .map(term -> {
                    Taxon taxon = term instanceof Taxon
                            ? TaxonUtil.copy((Taxon) term)
                            : new TaxonImpl(term.getName(), term.getId());
                    taxon.setName(suggest(term.getName()));
                    return taxon;
                })
                .forEach(taxon -> listener.foundTaxonForTerm(null,
                        taxon,
                        NameType.SAME_AS,
                        taxon
                ));

    }

}
