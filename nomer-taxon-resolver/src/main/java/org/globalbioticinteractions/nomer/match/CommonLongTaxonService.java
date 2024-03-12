package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.math.NumberUtils;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.util.ExternalIdUtil;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public abstract class CommonLongTaxonService extends CommonTaxonService<Long> {

    public CommonLongTaxonService(TermMatcherContext ctx) {
        super(ctx);
    }

    @Override
    public Long getIdOrNull(Taxon key, TaxonomyProvider matchingTaxonomyProvider) {
        TaxonomyProvider taxonomyProvider = ExternalIdUtil.taxonomyProviderFor(key.getExternalId());
        String idString = ExternalIdUtil.stripPrefix(matchingTaxonomyProvider, key.getExternalId());
        return (matchingTaxonomyProvider.equals(taxonomyProvider)
                && NumberUtils.isCreatable(idString))
                ? Long.parseLong(idString)
                : null;
    }

}
