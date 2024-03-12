package org.globalbioticinteractions.nomer.match;

import org.eol.globi.service.ServiceUtil;
import org.eol.globi.service.UKSISuggestionService;
import org.eol.globi.taxon.TaxonNameSuggestor;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.util.Collections;

public class TermMatcherUSKIFactory implements TermMatcherFactory {

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        return new TaxonNameSuggestor(ctx) {{
            setSuggestors(Collections.singletonList(new UKSISuggestionService() {
                {
                    ServiceUtil.initWith(this, "nomer.taxon.name.uksi.url", ctx);
                }
            }));
        }};
    }

    @Override
    public String getPreferredName() {
        return "uksi-current-name";
    }

    @Override
    public String getDescription() {
        return "Use UK Species Inventory to find current taxonomic name.";
    }
}
