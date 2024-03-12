package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.service.TaxonUtil;

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class TermMatchUtil {
    public static final String WILDCARD_MATCH = ".*";

    public static boolean shouldMatchAll(Term term, Map<Integer, String> inputSchema) {
        boolean matchesAll = true;
        if (inputSchema != null && term instanceof Taxon) {
            Map<String, String> taxonMap = TaxonUtil.taxonToMap((Taxon) term);
            for (String taxonPropertyName : inputSchema.values()) {
                String value = taxonMap.get(taxonPropertyName);
                matchesAll &= StringUtils.equals(WILDCARD_MATCH, value);
            }
        } else {
            matchesAll = StringUtils.equals(term.getName(), WILDCARD_MATCH)
                    && StringUtils.equals(term.getId(), WILDCARD_MATCH);
        }

        return matchesAll;

    }

    public static String[] wildcardRowForSchema(Map<Integer, String> schema) {
        SortedSet<Integer> objects = new TreeSet<>(schema.keySet());

        int rowSize = objects.last() + 1;
        String[] row = new String[rowSize];
        for (int i = 0; i < rowSize; i++) {
            row[i] = WILDCARD_MATCH;
        }
        return row;
    }

    public static boolean isWildcardMatch(String[] row, Map<Integer, String> inputSchema) {
        Set<Integer> indexes = inputSchema.keySet();
        boolean hasWildcards = true;
        for (Integer index : indexes) {
            if (index < row.length) {
                hasWildcards &= StringUtils.equals(row[index], WILDCARD_MATCH);
            }
        }
        return hasWildcards;
    }
}
