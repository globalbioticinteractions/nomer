package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.util.List;
import java.util.Map;

public class TermMatcherPlaziFactory implements TermMatcherFactory {

    @Override
    public String getName() {
        return "plazi";
    }

    @Override
    public String getDescription() {
        return "Lookup Plazi taxon treatment by name or id using offline-enabled database dump";
    }

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new PlaziService(ctx);
    }
}
