package org.globalbioticinteractions.nomer.util;

import org.eol.globi.domain.Taxon;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.RowHandler;
import org.eol.globi.taxon.TermMatcher;
import org.eol.globi.util.CSVTSVUtil;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AppendingRowHandler implements RowHandler {
    private final PrintStream p;
    private final TermMatcherContext ctx;
    private final Appender appender;
    private TermMatcher termMatcher;

    public AppendingRowHandler(OutputStream os, TermMatcher termMatcher, TermMatcherContext ctx) {
        this.ctx = ctx;
        this.p = new PrintStream(os);
        this.termMatcher = termMatcher;
        this.appender = new Appender() {

            @Override
            public void appendLinesForRow(String[] row, Stream<Taxon> resolvedTaxa, PrintStream p, NameTypeOf nameTypeOf) {
                Stream<String> provided = Stream.of(row);

                Stream<Stream<String>> lines = resolvedTaxa.map(taxon -> Stream.of(
                        nameTypeOf.nameTypeOf(taxon).name(),
                        taxon.getExternalId(),
                        taxon.getName(),
                        taxon.getRank(),
                        taxon.getCommonNames(),
                        taxon.getPath(),
                        taxon.getPathIds(),
                        taxon.getPathNames(),
                        taxon.getExternalUrl(),
                        taxon.getThumbnailUrl()))
                        .map(resolved -> Stream.concat(provided, resolved));

                lines.map(combinedLine -> CSVTSVUtil.mapEscapedValues(combinedLine)
                        .collect(Collectors.joining("\t")))
                        .forEach(p::println);
            }
        };
    }

    @Override
    public void onRow(final String[] row) throws PropertyEnricherException {
        Taxon taxonProvided = MatchUtil.asTaxon(row, ctx.getInputSchema());
        termMatcher.findTerms(Collections.singletonList(taxonProvided), (id, name, taxon, nameType) -> {
            Taxon taxonWithServiceInfo = TaxonUtil.mapToTaxon(TaxonUtil.taxonToMap(taxon));
            appender.appendLinesForRow(row, Stream.of(taxonWithServiceInfo), p, taxon1 -> nameType);
        });
    }
}
