package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.WikidataUtil;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.eol.globi.util.ResourceUtil;
import org.globalbioticinteractions.nomer.util.OpenBiodivUtil;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.globalbioticinteractions.util.SparqlClient;
import org.globalbioticinteractions.util.SparqlClientImpl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class TermMatcherOpenBiodivFactory implements TermMatcherFactory {

    @Override
    public String getName() {
        return "openbiodiv";
    }

    @Override
    public String getDescription() {
        return "uses openbiodiv sparql endpoint to resolve openbiodiv terms";
    }

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {

        return new TermMatcher() {
            @Override
            public void match(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException {
                for (Term term : terms) {
                    if (StringUtils.isNotBlank(term.getId())) {
                        try {
                            SparqlClient sparqlClient = new SparqlClientImpl(
                                    resourceName -> ResourceUtil.asInputStream(resourceName, in -> in),
                                    PropertyAndValueDictionary.SPARQL_ENDPOINT_OPEN_BIODIV
                            );

                            final Taxon taxon = OpenBiodivUtil
                                    .retrieveTaxonHierarchyById(term.getId(), sparqlClient);
                            if (taxon == null) {
                                termMatchListener.foundTaxonForTerm(null, term, new TaxonImpl(term.getName(), term.getId()), NameType.NONE);
                            } else {
                                termMatchListener.foundTaxonForTerm(null, term, taxon, NameType.SAME_AS);
                            }
                        } catch (IOException e) {
                            throw new PropertyEnricherException("failed to query openbiodiv with [" + term.getId() + "]", e);
                        }
                    }
                }
            }
        };
    }
}
