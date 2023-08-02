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

public abstract class TaxonNameSuggestorBase implements TermMatcher {

    private static final Logger LOG = LoggerFactory.getLogger(TaxonNameSuggestorBase.class);
    private final TermMatcherContext ctx;

    private List<NameSuggester> suggestors = null;

    public TaxonNameSuggestorBase() {
        this(null);
    }

    public TaxonNameSuggestorBase(TermMatcherContext ctx) {
        this.ctx = ctx;
    }


    public List<NameSuggester> getSuggestors() {
        return suggestors;
    }

    public void setSuggestors(List<NameSuggester> suggestors) {
        this.suggestors = suggestors;
    }

    public void lazyInit() {
        if (getSuggestors() == null) {
            setSuggestors(SuggesterFactory.createSuggesterEnsemble(ctx));
        }
    }
}
