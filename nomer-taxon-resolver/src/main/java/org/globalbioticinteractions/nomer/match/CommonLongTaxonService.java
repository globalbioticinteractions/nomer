package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.math.NumberUtils;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.util.ExternalIdUtil;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public abstract class CommonLongTaxonService extends CommonTaxonService<Long> {

    public CommonLongTaxonService(TermMatcherContext ctx) {
        super(ctx);
    }

    @Override
    public Long getIdOrNull(String key, TaxonomyProvider matchingTaxonomyProvider) {
        TaxonomyProvider taxonomyProvider = ExternalIdUtil.taxonomyProviderFor(key);
        String idString = ExternalIdUtil.stripPrefix(matchingTaxonomyProvider, key);
        return (matchingTaxonomyProvider.equals(taxonomyProvider)
                && NumberUtils.isCreatable(idString))
                ? Long.parseLong(idString)
                : null;
    }

}
