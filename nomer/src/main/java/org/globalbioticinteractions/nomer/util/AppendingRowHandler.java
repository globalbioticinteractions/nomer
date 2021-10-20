package org.globalbioticinteractions.nomer.util;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.RowHandler;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.match.MatchUtil;
import org.globalbioticinteractions.nomer.match.TermMatchUtil;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.stream.Stream;

public class AppendingRowHandler implements RowHandler {
    private final PrintStream out;
    private final TermMatcherContext ctx;
    private final Appender appender;
    private TermMatcher termMatcher;

    public AppendingRowHandler(OutputStream os, TermMatcher termMatcher, TermMatcherContext ctx, Appender appender) {
        this.ctx = ctx;
        this.out = new PrintStream(os);
        this.termMatcher = termMatcher;
        this.appender = appender;
    }

    @Override
    public void onRow(final String[] row) throws PropertyEnricherException {
        Taxon taxonProvided = MatchUtil.asTaxon(row, ctx.getInputSchema());
        termMatcher.match(Collections.singletonList(taxonProvided), (id, termToBeResolved, taxonResolved, nameType) -> {
            Taxon taxonWithServiceInfo = TaxonUtil.mapToTaxon(TaxonUtil.taxonToMap(taxonResolved));
            Taxon taxonToBeResolved = new TaxonImpl(termToBeResolved.getName(), termToBeResolved.getId());

            String[] replacedRow = row;
            if (isWildcardMatch(row)) {
                replacedRow = new String[]{
                        StringUtils.defaultString(termToBeResolved.getId(), ""),
                        StringUtils.defaultString(termToBeResolved.getName(), "")
                };
            }
            appender.appendLinesForRow(
                    replacedRow,
                    taxonToBeResolved,
                    taxon1 -> nameType, Stream.of(taxonWithServiceInfo),
                    out
            );
        });
    }

    private boolean isWildcardMatch(String[] row) {
        return row.length == 2
                && StringUtils.equals(row[0], TermMatchUtil.WILDCARD_MATCH)
                && StringUtils.equals(row[1], TermMatchUtil.WILDCARD_MATCH);
    }

}
