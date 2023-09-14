package org.globalbioticinteractions.nomer.util;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.RowHandler;
import org.eol.globi.taxon.TermMatcher;
import org.eol.globi.util.CSVTSVUtil;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReplacingRowHandler implements RowHandler {
    private final PrintStream p;
    private final TermMatcherContext ctx;
    private TermMatcher termMatcher;

    public ReplacingRowHandler(OutputStream os, TermMatcher termMatcher, TermMatcherContext ctx) {
        this.ctx = ctx;
        this.p = new PrintStream(os);
        this.termMatcher = termMatcher;
    }

    private static Map<Integer, Map<String, String>> asTaxon(String[] row, Map<Integer, String> schema) {
        Map<Integer, Map<String, String>> taxonMaps = new TreeMap<>();
        if (schema != null) {
            for (Map.Entry<Integer, String> indexType : schema.entrySet()) {
                Integer key = indexType.getKey();
                if (row.length > key) {
                    String[] values = CSVTSVUtil.splitPipes(row[key]);
                    for (int i = 0; i < values.length; i++) {
                        Map<String, String> taxonMap = taxonMaps.get(i);
                        if (taxonMap == null) {
                            taxonMap = new TreeMap<>();
                        }
                        taxonMap.put(indexType.getValue(), StringUtils.trim(values[i]));
                        taxonMaps.put(i, taxonMap);
                    }
                }
            }
        }
        return taxonMaps;
    }

    private static String[] mergeTaxonMapsIntoRow(
            String[] row,
            Collection<Map<String, String>> taxonMaps,
            Map<Integer, String> outputSchema) {
        String[] rowMerged = Arrays.copyOf(row, row.length);
        if (outputSchema != null) {
            for (Map.Entry<Integer, String> indexType : outputSchema.entrySet()) {
                String taxonPropertyName = indexType.getValue();
                if (indexType.getKey() < rowMerged.length) {
                    List<String> values = new ArrayList<>(taxonMaps.size());
                    for (Map<String, String> taxonMap : taxonMaps) {

                        Taxon taxon = TaxonUtil.mapToTaxon(taxonMap);

                        String taxonPropertyValue = AppenderUtil.valueForTaxonPropertyName2(taxon, taxonMap, taxonPropertyName);
                        values.add(taxonPropertyValue);
                    }
                    rowMerged[indexType.getKey()] = values
                            .stream()
                            .map(x -> StringUtils.replaceChars(x, '|', ' '))
                            .collect(Collectors.joining(CharsetConstant.SEPARATOR));

                }
            }
        }
        return rowMerged;
    }

    private Map<String, String> mergeTaxon(Taxon taxon, NameType nameType, Taxon otherTaxon) {
        return new TreeMap<String, String>() {{
            putAll(TaxonUtil.taxonToMap(taxon));
            if (!NameType.NONE.equals(nameType)) {
                putAll(TaxonUtil.taxonToMap(otherTaxon));
            }
            put("matchType", nameType.name());
        }};
    }


    @Override
    public void onRow(final String[] row) throws PropertyEnricherException {
        final Map<Integer, Map<String, String>> taxonProvided = asTaxon(row, ctx.getInputSchema());
        final TreeMap<Integer, Map<String, String>> taxonReplaced = new TreeMap<>(taxonProvided);

        for (Map.Entry<Integer, Map<String, String>> taxonMapEntry : taxonProvided.entrySet()) {
            Taxon providedTaxon = TaxonUtil.mapToTaxon(taxonMapEntry.getValue());
            List<Term> terms = Collections.singletonList(providedTaxon);
            AtomicBoolean replacedOne = new AtomicBoolean(false);
            termMatcher.match(terms, (id, nameToBeReplaced, nameType, resolvedTaxon) -> {
                if (!replacedOne.get()) {
                    replacedOne.set(true);
                    Taxon taxonToBeReplaced = new TaxonImpl(nameToBeReplaced.getName(), nameToBeReplaced.getId());
                    taxonReplaced.put(
                            taxonMapEntry.getKey(),
                            mergeTaxon(taxonToBeReplaced, nameType, resolvedTaxon)
                    );
                }
            });
        }

        String[] lineMerged = mergeTaxonMapsIntoRow(row, taxonReplaced.values(), ctx.getOutputSchema());
        printRow(lineMerged);

    }

    private void printRow(String[] rowMerged) {
        p.println(CSVTSVUtil.mapEscapedValues(Stream.of(rowMerged))
                .collect(Collectors.joining("\t")));
    }

}
