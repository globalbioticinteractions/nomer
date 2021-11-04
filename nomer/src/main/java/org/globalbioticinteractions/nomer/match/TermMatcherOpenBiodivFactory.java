package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.eol.globi.util.ResourceUtil;
import org.globalbioticinteractions.nomer.util.OpenBiodivUtil;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.globalbioticinteractions.nomer.util.UUIDUtil;
import org.globalbioticinteractions.util.SparqlClient;
import org.globalbioticinteractions.util.SparqlClientImpl;

import java.io.IOException;
import java.util.List;

public class TermMatcherOpenBiodivFactory implements TermMatcherFactory {

    @Override
    public String getPreferredName() {
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
                    if (UUIDUtil.isaUUID(term.getId())) {
                        try {
                            SparqlClient sparqlClient = new SparqlClientImpl(
                                    resourceName -> ResourceUtil.asInputStream(resourceName, in -> in),
                                    PropertyAndValueDictionary.SPARQL_ENDPOINT_OPEN_BIODIV
                            );

                            final Taxon taxon = OpenBiodivUtil
                                    .retrieveTaxonHierarchyById(term.getId(), sparqlClient);
                            if (taxon == null) {
                                termMatchListener.foundTaxonForTerm(
                                        null,
                                        term,
                                        NameType.NONE,
                                        new TaxonImpl(term.getName(), term.getId()));
                            } else {
                                termMatchListener.foundTaxonForTerm(
                                        null,
                                        term,
                                        NameType.SAME_AS,
                                        taxon
                                );
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
