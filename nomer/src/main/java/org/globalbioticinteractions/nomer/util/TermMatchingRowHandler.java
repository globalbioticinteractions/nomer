package org.globalbioticinteractions.nomer.util;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.RowHandler;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.eol.globi.util.CSVTSVUtil;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TermMatchingRowHandler implements RowHandler {
    private final boolean shouldReplace;
    private final PrintStream p;
    private TermMatcher termMatcher;

    public TermMatchingRowHandler(OutputStream os, TermMatcher termMatcher, TermMatcherContext ctx) {
        this.shouldReplace = ctx.shouldReplaceTerms();
        this.p = new PrintStream(os);
        this.termMatcher = termMatcher;
    }

    static Taxon asTaxon(String[] row) {
        Taxon taxon;
        if (row.length == 1) {
            taxon = new TaxonImpl(null, row[0]);
        } else if (row.length > 1) {
            taxon = new TaxonImpl(row[1], row[0]);
        } else {
            taxon = new TaxonImpl("", "");
        }
        return taxon;
    }

    public static void linesForTaxa(String[] row, Stream<Taxon> resolvedTaxa, boolean shouldReplace, PrintStream p, NameTypeOf nameTypeOf) {
        Stream<String> provided = Stream.of(row);

        Stream<Stream<String>> lines = resolvedTaxa.map(taxon -> Stream.of(
                nameTypeOf.nameTypeOf(taxon).name(),
                taxon.getExternalId(), taxon.getName(),
                taxon.getRank(),
                taxon.getCommonNames(),
                taxon.getPath(),
                taxon.getPathIds(),
                taxon.getPathNames(),
                taxon.getExternalUrl(),
                taxon.getThumbnailUrl(),
                taxon.getNameSource(),
                taxon.getNameSourceURL(),
                taxon.getNameSourceAccessedAt()))
                .map(resolved -> shouldReplace
                        ? Stream.concat(resolved.skip(1).limit(2), provided.skip(2))
                        : Stream.concat(provided, resolved));

        lines.map(combinedLine -> CSVTSVUtil.mapEscapedValues(combinedLine)
                .collect(Collectors.joining("\t")))
                .forEach(p::println);
    }



        @Override
        public void onRow(final String[] row) throws PropertyEnricherException {
            Taxon taxonProvided = asTaxon(row);
            termMatcher.findTerms(Arrays.asList(taxonProvided), new TermMatchListener() {
                @Override
                public void foundTaxonForName(Long id, String name, Taxon taxon, NameType nameType) {
                    Taxon taxonWithServiceInfo = (TaxonUtil.mapToTaxon(TaxonUtil.appendNameSourceInfo(TaxonUtil.taxonToMap(taxon), termMatcher.getClass(), new Date())));
                    linesForTaxa(row, Stream.of(taxonWithServiceInfo), shouldReplace, p, taxon1 -> nameType);
                }
            });
        }
    }
