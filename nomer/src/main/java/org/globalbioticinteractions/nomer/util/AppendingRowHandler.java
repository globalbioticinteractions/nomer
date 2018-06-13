package org.globalbioticinteractions.nomer.util;

import org.eol.globi.domain.Taxon;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.RowHandler;
import org.eol.globi.taxon.TermMatcher;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.stream.Stream;

public class AppendingRowHandler implements RowHandler {
    private final PrintStream p;
    private final TermMatcherContext ctx;
    private final Appender appender;
    private TermMatcher termMatcher;

    public AppendingRowHandler(OutputStream os, TermMatcher termMatcher, TermMatcherContext ctx, Appender appender) {
        this.ctx = ctx;
        this.p = new PrintStream(os);
        this.termMatcher = termMatcher;
        this.appender = appender;
    }

    @Override
    public void onRow(final String[] row) throws PropertyEnricherException {
        Taxon taxonProvided = MatchUtil.asTaxon(row, ctx.getInputSchema());
        termMatcher.findTerms(Collections.singletonList(taxonProvided), (id, name, taxon, nameType) -> {
            Taxon taxonWithServiceInfo = TaxonUtil.mapToTaxon(TaxonUtil.taxonToMap(taxon));
            appender.appendLinesForRow(row, taxonProvided, Stream.of(taxonWithServiceInfo), p, taxon1 -> nameType);
        });
    }

}
