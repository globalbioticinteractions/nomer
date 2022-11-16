package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.globalbioticinteractions.wikidata.WikidataUtil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class TermMatcherWikidataFactory implements TermMatcherFactory {

    @Override
    public String getPreferredName() {
        return "wikidata-taxon-id-web";
    }

    @Override
    public String getDescription() {
        return "uses wikidata to cross-walk taxon id across taxonomies";
    }

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {

        return new TermMatcher() {
            @Override
            public void match(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException {
                for (Term term : terms) {
                    if (StringUtils.isNotBlank(term.getId())) {
                        try {
                            final List<Taxon> linkedTaxa = WikidataUtil.findRelatedTaxonIds(term.getId());
                            for (Taxon taxon : linkedTaxa) {
                                termMatchListener.foundTaxonForTerm(
                                        null,
                                        term,
                                        NameType.SAME_AS,
                                        taxon
                                );
                            }
                            if (linkedTaxa.size() == 0) {
                                termMatchListener.foundTaxonForTerm(
                                        null,
                                        term,
                                        NameType.NONE,
                                        new TaxonImpl(term.getName(), term.getId())
                                );
                            }
                        } catch (URISyntaxException | IOException e) {
                            throw new PropertyEnricherException("failed to query wikidata with [" + term.getId() + "]", e);
                        }
                    }
                }
            }
        };
    }
}
