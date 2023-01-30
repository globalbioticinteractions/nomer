package org.globalbioticinteractions.nomer.util;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.NameType;
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
import java.util.Map;
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
    public void onRow(final String[] rowOrig) throws PropertyEnricherException {
        Taxon providedTaxon = MatchUtil.asTaxon(rowOrig, ctx.getInputSchema());
        termMatcher.match(
                Collections.singletonList(providedTaxon),
                (id, termToBeResolved, nameType, taxonResolved) -> {

                    Taxon taxonToBeResolved;
                    if (termToBeResolved instanceof Taxon) {
                        taxonToBeResolved = TaxonUtil.copy((Taxon) termToBeResolved);
                    } else {
                        taxonToBeResolved = new TaxonImpl(termToBeResolved.getName(), termToBeResolved.getId());
                    }

                    for (String matcher : ctx.getMatchers()) {
                        taxonResolved.setNameSource(matcher);
                    }

                    String[] rowToBeAppended = isWildcardMatch(rowOrig)
                            ? fillProvidedTaxon(rowOrig, taxonToBeResolved)
                            : rowOrig;


                    NameTypeOf typeMapperOf = t -> taxonResolved == null ? NameType.NONE : nameType;
                    appender.appendLinesForRow(
                            rowToBeAppended,
                            taxonToBeResolved,
                            typeMapperOf,
                            streamFor(taxonResolved == null ? taxonToBeResolved : taxonResolved),
                            out
                    );

                });
    }

    public Stream<Taxon> streamFor(Taxon taxonResolved) {
        Map<String, String> properties = TaxonUtil.taxonToMap(taxonResolved);
        Taxon taxonWithServiceInfo = TaxonUtil.mapToTaxon(properties);
        return Stream.of(taxonWithServiceInfo);
    }

    private String[] fillProvidedTaxon(String[] rowOrig, Taxon taxonToBeResolved) {
        String[] rowNew = new String[rowOrig.length];
        Map<Integer, String> inputSchema = ctx.getInputSchema();
        Map<String, String> providedTaxonMap = TaxonUtil.taxonToMap(taxonToBeResolved);
        for (Integer index : inputSchema.keySet()) {
            if (index < rowOrig.length) {
                rowNew[index] = providedTaxonMap.get(StringUtils.defaultString(inputSchema.get(index)));
            }
        }
        return rowNew;
    }

    private boolean isWildcardMatch(String[] row) {
        Map<Integer, String> inputSchema = ctx.getInputSchema();
        return TermMatchUtil.isWildcardMatch(row, inputSchema);
    }

}
