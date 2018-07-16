package org.globalbioticinteractions.nomer.util;

import org.apache.commons.collections4.list.TreeList;
import org.apache.commons.lang.StringUtils;
import org.eol.globi.domain.Taxon;
import org.eol.globi.util.CSVTSVUtil;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
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
    public void appendLinesForRow(String[] row, Taxon taxonProvided, Stream<Taxon> resolvedTaxa, PrintStream p, NameTypeOf nameTypeOf) {
        Stream<String> provided = Stream.of(row);

        Stream<Stream<String>> appended = hasDefaultSchema()
                ? handleDefaultSchema(resolvedTaxa, nameTypeOf)
                : handleWithCustomOutputSchema(resolvedTaxa, nameTypeOf);

        Stream<Stream<String>> lines = appended
                .map(resolved -> Stream.concat(provided, resolved));

        lines.map(combinedLine -> CSVTSVUtil.mapEscapedValues(combinedLine)
                .collect(Collectors.joining("\t")))
                .forEach(p::println);
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
            String pathNames = taxon.getPathNames();
            List<String> ranks = splitAndTrim(pathNames);
            List<String> ids = splitAndTrim(taxon.getPathIds());
            List<String> names = splitAndTrim(taxon.getPath());
            for (int i = 0; i <= maxColumns; i++) {
                String colValue = "";
                if (ranks.size() > 0
                        && ranks.size() == ids.size()
                        && names.size() == ids.size()) {
                    String colName = outputSchema.get(i);
                    if (StringUtils.equalsIgnoreCase(colName, "id")) {
                        colValue = taxon.getExternalId();
                    } else if (StringUtils.equalsIgnoreCase(colName, "name")) {
                        colValue = taxon.getName();
                    } else if (StringUtils.equalsIgnoreCase(colName, "rank")) {
                        colValue = taxon.getRank();
                    } else if (StringUtils.startsWith(colName, "path.")) {
                        String[] split = StringUtils.split(colName, '.');
                        if (split != null && split.length > 1) {
                            String rank = split[1];
                            int i1 = ranks.indexOf(rank);
                            if (i1 > -1) {
                                if (split.length > 2) {
                                    boolean shouldUseId = "id".equalsIgnoreCase(split[2]);
                                    colValue = shouldUseId
                                            ? ids.get(i1)
                                            : names.get(i1);
                                } else {
                                    colValue = rank;
                                }
                            }
                        }
                    }
                }
                columns.add(colValue);
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

    private List<String> splitAndTrim(String pathNames) {
        return StringUtils.isBlank(pathNames)
                ? Collections.emptyList()
                : Arrays.stream(CSVTSVUtil.splitPipes(pathNames)).map(String::trim).collect(Collectors.toList());
    }
}
