package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.PropertyEnricherSimple;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.eol.globi.util.ExternalIdUtil;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.mapdb.BTreeMap;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
