package org.globalbioticinteractions.nomer.util;

import org.eol.globi.domain.Taxon;
import org.eol.globi.util.CSVTSVUtil;

import java.io.PrintStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AppenderTSV implements Appender {

    @Override
    public void appendLinesForRow(String[] row, Taxon taxonProvided, Stream<Taxon> resolvedTaxa, PrintStream p, NameTypeOf nameTypeOf) {
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
}
