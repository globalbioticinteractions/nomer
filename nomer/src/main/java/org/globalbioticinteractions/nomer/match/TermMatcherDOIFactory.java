package org.globalbioticinteractions.nomer.match;

import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.DOIResolver;
import org.eol.globi.service.DOIResolverCache;
import org.eol.globi.service.DOIResolverImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public class TermMatcherDOIFactory implements TermMatcherFactory {

    public static final String NOMER_DOI_CACHE_URL = "nomer.doi.cache.url";
    public static final String NOMER_DOI_CROSSREF_MIN_SCORE = "nomer.doi.min.match.score";
    private static final Log LOG = LogFactory.getLog(TermMatcherDOIFactory.class);

    @Override
    public String getName() {
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
            public void findTermsForNames(List<String> names, TermMatchListener termMatchListener) throws PropertyEnricherException {
                List<Term> terms = names.stream().map(name -> new TermImpl(null, name)).collect(Collectors.toList());
                findTerms(terms, termMatchListener);
            }

            @Override
            public void findTerms(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException {
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
                            termMatchListener.foundTaxonForName(null, term.getName(), found, matchType);
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
                : createWithCache(ctx, taxonRankCacheUrl);
    }

    private DOIResolver createWithCache(TermMatcherContext ctx, String taxonRankCacheUrl) {
        DOIResolver doiResolver;
        try {
            InputStream resource = ctx.getResource(taxonRankCacheUrl);
            DOIResolverCache doiResolverCache = new DOIResolverCache();
            doiResolverCache.init(new InputStreamReader(resource, Charsets.UTF_8));
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
                if (NumberUtils.isNumber(property)) {
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
