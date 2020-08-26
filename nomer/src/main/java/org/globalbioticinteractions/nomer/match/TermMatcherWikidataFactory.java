package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.DOIResolver;
import org.eol.globi.service.DOIResolverCache;
import org.eol.globi.service.DOIResolverImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.WikidataUtil;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TermMatcherWikidataFactory implements TermMatcherFactory {

    @Override
    public String getName() {
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
                                termMatchListener.foundTaxonForTerm(null, term, taxon, NameType.SAME_AS);
                            }
                            if (linkedTaxa.size() == 0) {
                                termMatchListener.foundTaxonForTerm(null, term, new TaxonImpl(term.getName(), term.getId()), NameType.NONE);
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
