package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.Initializing;
import org.eol.globi.service.NameSuggester;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.UKSISuggestionService;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TaxonNameCorrector implements CorrectionService, TermMatcher {

    private static final Log LOG = LogFactory.getLog(TaxonNameCorrector.class);
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
        String suggestion;
        if (StringUtils.isBlank(taxonName)) {
            suggestion = PropertyAndValueDictionary.NO_NAME;
        } else if (StringUtils.equals(taxonName, PropertyAndValueDictionary.NO_MATCH)) {
            suggestion = PropertyAndValueDictionary.NO_MATCH;
        } else {
            suggestion = suggestCorrection(taxonName);
        }
        return suggestion;
    }

    private void initWith(Initializing service, String propertyName) {
        String property = null == ctx ? null : ctx.getProperty(propertyName);
        try {
            if (StringUtils.isNotBlank(property)) {
                service.init(ctx.getResource(property));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("failed to instantiate name service [" + service.getClass().getSimpleName() + "]", e);
        }
    }

    private String suggestCorrection(String taxonName) {
        String suggestion;
        if (suggestors == null) {
            final ManualSuggester manualSuggestor = new ManualSuggester() {{
                initWith(this,"nomer.taxon.name.correction.url");
            }};

            final RemoveStopWordService stopwordRemover = new RemoveStopWordService() {{
                initWith(this, "nomer.taxon.name.stopword.url");
            }};


            suggestors = new ArrayList<NameSuggester>() {
                {
                    add(new UKSISuggestionService() {
                        {
                            initWith(this,"nomer.taxon.name.uksi.url");
                        }
                    });
                    add(manualSuggestor);
                    add(new NameScrubber());
                    add(stopwordRemover);
                    add(new GlobalNamesCanon());
                    add(manualSuggestor);
                }
            };
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
                nameSuggestion = PropertyAndValueDictionary.NO_NAME;
                break;
            }
        }
        return nameSuggestion;
    }


    @Override
    public void findTermsForNames(List<String> names, TermMatchListener termMatchListener) throws
            PropertyEnricherException {
        List<Term> terms = names.stream().map(name -> new TermImpl(null, name)).collect(Collectors.toList());
        this.findTerms(terms, termMatchListener);
    }

    @Override
    public void findTerms(List<Term> terms, TermMatchListener listener) throws PropertyEnricherException {
        Stream<TermImpl> correctedTerms = terms.stream()
                .map(term -> new TermImpl(term.getId(), correct(term.getName())));
        correctedTerms.forEach(term -> {
            listener.foundTaxonForName(null, term.getName(), new TaxonImpl(term.getName(), term.getId()), NameType.SAME_AS);
        });

    }


}
