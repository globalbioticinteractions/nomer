package org.globalbioticinteractions.nomer.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.RowHandler;
import org.eol.globi.taxon.TermMatcher;
import org.eol.globi.util.CSVTSVUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class MatchUtil {

    private final static Log LOG = LogFactory.getLog(MatchUtil.class);

    public static void match(final List<String> matcherIds, TermMatcherContext ctx) {
        TermMatcher matcher = getTermMatcher(matcherIds, ctx);
        LOG.info("using matcher [" + matcher.getClass().getName() + "]");
        match(new TermMatchingRowHandler(System.out, matcher, ctx));
    }

    public static void match(RowHandler handler) {
        try {
            resolve(System.in, handler);
        } catch (IOException | PropertyEnricherException e) {
            throw new RuntimeException("failed to resolve taxon", e);
        }
    }

    public static TermMatcher getTermMatcher(List<String> matcherIds, TermMatcherContext ctx) {
        final Stream<TermMatcher> matchers =
                matcherIds
                        .stream()
                        .map(matcherId -> {
                            TermMatcher e = TermMatcherRegistry.termMatcherFor(matcherId, ctx);
                            return Optional.ofNullable(e);
                        }).filter(Optional::isPresent)
                        .map(Optional::get);

        Optional<TermMatcher> firstMatcher = matchers.findFirst();
        return firstMatcher.orElseGet(() -> TermMatcherRegistry.defaultMatcher(ctx));
    }

    public static void resolve(InputStream is, RowHandler rowHandler) throws IOException, PropertyEnricherException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        long counter = 0;
        while ((line = reader.readLine()) != null) {
            String[] row = CSVTSVUtil.splitTSV(line);
            rowHandler.onRow(row);
            counter++;
            if (counter % 25 == 0) {
                System.err.print(".");
            }
            if (counter % (25 * 50) == 0) {
                System.err.println();
            }
        }
        if (counter % (25 * 50) != 0) {
            System.err.println();
        }
    }

}
