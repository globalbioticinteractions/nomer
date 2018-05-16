package org.globalbioticinteractions.nomer.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.Taxon;
import org.eol.globi.taxon.TaxonCacheParser;
import org.eol.globi.taxon.TaxonMapParser;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.ExternalIdUtil;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class TermValidatorPredicates {

    public static final Predicate<String> SUPPORTED_ID = ExternalIdUtil::isSupported;

    public static final Predicate<String> VALID_NUMBER_OF_TERM_COLUMNS = line -> CSVTSVUtil.splitTSV(line).length == 9;

    public static final Predicate<String> VALID_NUMBER_OF_MAP_COLUMNS = line -> CSVTSVUtil.splitTSV(line).length == 4;

    public static final Predicate<String> SUPPORTED_RESOLVED_ID = line -> {
        Taxon taxon = TaxonMapParser.parseResolvedTaxon(CSVTSVUtil.splitTSV(line));
        return ExternalIdUtil.isSupported(taxon.getExternalId());
    };

    public static final Predicate<String> CONSISTENT_PATH = line -> {
        Taxon taxon = TaxonCacheParser.parseLine(line);
        int paths = getLength(taxon.getPath());
        int ids = getLength(taxon.getPathIds());
        int ranks = getLength(taxon.getPathNames());
        IntStream distinct = IntStream.of(paths, ids, ranks).filter(i -> i > 0).distinct();
        return paths > 0 && distinct.count() == 1;
    };

    public static final Predicate<String> SUPPORTED_PATH_IDS = line -> {
        Taxon taxon = TaxonCacheParser.parseLine(line);
        String ids = taxon.getPathIds();
        return StringUtils.isBlank(ids)
                || Stream.of(StringUtils.splitByWholeSeparatorPreserveAllTokens(ids, CharsetConstant.SEPARATOR_CHAR))
                .map(StringUtils::trim)
                .filter(StringUtils::isNotBlank)
                .allMatch(SUPPORTED_ID);
    };

    public static List<Pair<Predicate<String>, String>> TERM_PREDICATES = Arrays.asList(
            Pair.of(Objects::nonNull, "non empty"),
            Pair.of(VALID_NUMBER_OF_TERM_COLUMNS, "9 columns"),
            Pair.of(SUPPORTED_ID, "supported ids"),
            Pair.of(CONSISTENT_PATH, "consistent non empty term path"),
            Pair.of(SUPPORTED_PATH_IDS, "supported path ids")
    );

    public static List<Pair<Predicate<String>, String>> MAP_PREDICATES = Arrays.asList(
            Pair.of(Objects::nonNull, "non empty"),
            Pair.of(VALID_NUMBER_OF_MAP_COLUMNS, "4 columns"),
            Pair.of(SUPPORTED_RESOLVED_ID, "supported resolved id")
    );

    private static int getLength(String path) {
        int pathLength = 0;
        if (StringUtils.isNotBlank(path)) {
            pathLength = StringUtils.splitByWholeSeparatorPreserveAllTokens(path,"|").length;
        }
        return pathLength;
    }


}
