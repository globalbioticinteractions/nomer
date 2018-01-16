package org.globalbioticinteractions.nomer.util;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.RowHandler;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.stream.Stream;

public class ResolvingRowHandler implements RowHandler {
    private final PropertyEnricher enricher;
    private final boolean shouldReplace;
    private final PrintStream p;

    public ResolvingRowHandler(OutputStream os, PropertyEnricher enricher, TermMatcherContext ctx) {
        this.enricher = enricher;
        this.shouldReplace = ctx.shouldReplaceTerms();
        this.p = new PrintStream(os);
    }

    @Override
    public void onRow(String[] row) throws PropertyEnricherException {
        Stream<Taxon> resolvedTaxa = Stream.of(MatchUtil.resolveTaxon(enricher, TermMatchingRowHandler.asTaxon(row)));
        TermMatchingRowHandler.linesForTaxa(row,
                resolvedTaxa,
                shouldReplace,
                p,
                taxon -> TaxonUtil.isResolved(taxon) ? NameType.SAME_AS : NameType.NONE);
    }

}
