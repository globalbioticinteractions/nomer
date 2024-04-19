package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.globalbioticinteractions.nomer.util.CacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.DOIResolver;
import org.eol.globi.service.DOIResolverCache;
import org.eol.globi.service.DOIResolverImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TermMatcherDOIFactory implements TermMatcherFactory {

    public static final String NOMER_DOI_CACHE_URL = "nomer.doi.cache.url";
    public static final String NOMER_DOI_CROSSREF_MIN_SCORE = "nomer.doi.min.match.score";
    private static final Logger LOG = LoggerFactory.getLogger(TermMatcherDOIFactory.class);

    @Override
    public String getPreferredName() {
        return "crossref-doi";
    }

    @Override
    public String getDescription() {
        return "uses api.crossref.org to resolve doi associated with human readable citation";
    }

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        final DOIResolver doiResolver = createResolverCacheOrAPI(ctx);

        return new TermMatcher() {
            @Override
            public void match(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException {
                for (Term term : terms) {
                    try {
                        if (StringUtils.isNotBlank(term.getName())) {
                            DOI doi = doiResolver.resolveDoiFor(term.getName());
                            Taxon found = null == doi
                                    ? new TaxonImpl(term.getName())
                                    : new TaxonImpl(term.getName(), doi.toURI().toString());
                            NameType matchType = null == doi
                                    ? NameType.NONE
                                    : NameType.SAME_AS;
                            termMatchListener.foundTaxonForTerm(
                                    null,
                                    term,
                                    matchType,
                                    found
                            );
                        }
                    } catch (IOException e) {
                        throw new PropertyEnricherException("failed to resolver doi for [" + term.getName() + "]", e);
                    }
                }
            }
        };
    }

    private DOIResolver createResolverCacheOrAPI(TermMatcherContext ctx) {
        String taxonRankCacheUrl = ctx == null ? null : ctx.getProperty(NOMER_DOI_CACHE_URL);
        return StringUtils.isBlank(taxonRankCacheUrl)
                ? createWithAPI(ctx)
                : createWithCache(ctx, NOMER_DOI_CACHE_URL);
    }

    private DOIResolver createWithCache(TermMatcherContext ctx, String taxonRankCacheUrl) {
        DOIResolver doiResolver;
        try {
            InputStream resource = ctx.retrieve(CacheUtil.getValueURI(ctx, taxonRankCacheUrl));
            File cacheDir = new File(ctx.getCacheDir(), "doi-cache");
            DOIResolverCache doiResolverCache = new DOIResolverCache(cacheDir);
            doiResolverCache.init(new InputStreamReader(resource, StandardCharsets.UTF_8));
            doiResolver = doiResolverCache;
        } catch (IOException | PropertyEnricherException e) {
            throw new RuntimeException("failed to create doi resolver cache", e);
        }
        return doiResolver;
    }

    private DOIResolver createWithAPI(TermMatcherContext ctx) {
        DOIResolverImpl doiResolver = new DOIResolverImpl();
        if (ctx != null) {
            String property = ctx.getProperty(NOMER_DOI_CROSSREF_MIN_SCORE);
            if (StringUtils.isNotBlank(property)) {
                if (NumberUtils.isCreatable(property)) {
                    double minScore = Double.parseDouble(property);
                    doiResolver.setMinMatchScore(minScore);
                } else {
                    LOG.warn("ignoring non numeric value [" + property + "] for [" + NOMER_DOI_CROSSREF_MIN_SCORE + "]");
                }
            }
        }
        return doiResolver;
    }

}
