package org.globalbioticinteractions.nomer.util;

import org.apache.commons.collections4.list.TreeList;
import org.eol.globi.domain.Taxon;
import org.eol.globi.util.CSVTSVUtil;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AppenderTSV implements Appender {

    private final Map<Integer, String> outputSchema;

    public AppenderTSV() {
        this(Collections.emptyMap());
    }

    public AppenderTSV(Map<Integer, String> outputSchema) {
        this.outputSchema = new TreeMap<>(outputSchema);
    }


    @Override
    public void appendLinesForRow(String[] row, Taxon taxonProvided, NameTypeOf nameTypeOf, Stream<Taxon> resolvedTaxa, PrintStream out) {
        Stream<String> provided = Stream.of(row);

        Stream<Stream<String>> appended = hasDefaultSchema()
                ? handleDefaultSchema(resolvedTaxa, nameTypeOf)
                : handleWithCustomOutputSchema(resolvedTaxa, nameTypeOf);

        Stream<Stream<String>> lines = appended
                .map(resolved -> Stream.concat(provided, resolved));

        lines.map(combinedLine -> CSVTSVUtil.mapEscapedValues(combinedLine)
                .collect(Collectors.joining("\t")))
                .forEach(out::println);
    }

    private Stream<Stream<String>> handleDefaultSchema(Stream<Taxon> resolvedTaxa, NameTypeOf nameTypeOf) {
        return resolvedTaxa.map(taxon -> Stream.of(
                nameTypeOf.nameTypeOf(taxon).name(),
                taxon.getExternalId(),
                taxon.getName(),
                taxon.getRank(),
                taxon.getCommonNames(),
                taxon.getPath(),
                taxon.getPathIds(),
                taxon.getPathNames(),
                taxon.getExternalUrl(),
                taxon.getThumbnailUrl()));
    }

    private Stream<Stream<String>> handleWithCustomOutputSchema(Stream<Taxon> resolvedTaxa, NameTypeOf nameTypeOf) {
        Stream<Stream<String>> appended;
        List<Integer> keys = new TreeList<>(outputSchema.keySet());
        Integer maxColumns = keys.get(keys.size() - 1);
        List<String> columns = new ArrayList<>(maxColumns);
        appended = resolvedTaxa.map(taxon -> {
            for (int i = 0; i <= maxColumns; i++) {
                String colName = outputSchema.get(i);

                String taxonPropertyValue = AppenderUtil.valueForTaxonPropertyName(taxon, colName);

                columns.add(taxonPropertyValue);
            }
            return Stream.concat(
                    Stream.of(nameTypeOf.nameTypeOf(taxon).name()),
                    columns.stream());
        });
        return appended;
    }


    private boolean hasDefaultSchema() {
        return outputSchema.isEmpty();
    }

}
