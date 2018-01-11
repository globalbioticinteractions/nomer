package org.globalbioticinteractions.nomer.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.RowHandler;
import org.eol.globi.taxon.TermMatcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class MatchUtil {
    private final static Log LOG = LogFactory.getLog(MatchUtil.class);

    public static void match(final List<String> matcherIds, boolean shouldReplace) {
        try {
            final Stream<TermMatcher> matchers =
                    matcherIds
                            .stream()
                            .map(matcherId -> {
                                TermMatcher e = TermMatcherRegistry.termMatcherFor(matcherId);
                                return Optional.ofNullable(e);
                            }).filter(Optional::isPresent)
                            .map(Optional::get);

            Optional<TermMatcher> firstMatcher = matchers.findFirst();
            TermMatcher matcher = firstMatcher.orElseGet(TermMatcherRegistry::defaultMatcher);
            LOG.info("using matcher [" + matcher.getClass().getName() + "]");
            resolve(System.in, new TermMatchingRowHandler(shouldReplace, System.out, matcher));
        } catch (IOException | PropertyEnricherException e) {
            throw new RuntimeException("failed to resolve taxon", e);
        }
    }

    public static void resolve(InputStream is, RowHandler rowHandler) throws IOException, PropertyEnricherException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        long counter = 0;
        while ((line = reader.readLine()) != null) {
            String[] row = line.split("\t");
            rowHandler.onRow(row);
            counter++;
            if (counter % 25 == 0) {
                System.err.print(".");
            }
            if (counter % (25 * 50) == 0) {
                System.err.println();
            }
        }
    }

    static Taxon resolveTaxon(PropertyEnricher enricher, Taxon taxonProvided) throws PropertyEnricherException {
        Map<String, String> enriched = enricher.enrich(TaxonUtil.taxonToMap(taxonProvided));
        return TaxonUtil.mapToTaxon(enriched);
    }

}
