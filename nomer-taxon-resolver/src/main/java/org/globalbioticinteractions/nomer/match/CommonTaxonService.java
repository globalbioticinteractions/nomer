package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.eol.globi.data.CharsetConstant;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public abstract class CommonTaxonService extends PropertyEnricherSimple implements TermMatcher {

    private static final Logger LOG = LoggerFactory.getLogger(CommonTaxonService.class);
    protected static final String DENORMALIZED_NODES = "denormalizedNodes";
    protected static final String DENORMALIZED_NODE_IDS = "denormalizedNodeIds";
    protected static final String MERGED_NODES = "mergedNodes";


    private final TermMatcherContext ctx;

    protected BTreeMap<Long, Long> mergedNodes = null;
    protected BTreeMap<String, List<Map<String, String>>> denormalizedNodes = null;
    protected BTreeMap<Long, List<Map<String, String>>> denormalizedNodeIds = null;

    public CommonTaxonService(TermMatcherContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void match(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException {
        for (Term term : terms) {
            if (TermMatchUtil.shouldMatchAll(term)) {
                matchAll(termMatchListener);
            } else {
                Taxon taxon = new TaxonImpl(term.getName(), term.getId());
                TaxonomyProvider taxonomyProvider = ExternalIdUtil.taxonomyProviderFor(term.getId());
                if (getTaxonomyProvider().equals(taxonomyProvider)) {
                    String id = term.getId();
                    enrichMatches(TaxonUtil.taxonToMap(taxon), getIdOrNull(id, getTaxonomyProvider()), termMatchListener);
                } else if (StringUtils.isNoneBlank(term.getName())) {
                    enrichMatches(TaxonUtil.taxonToMap(taxon), term.getName(), termMatchListener);
                }
            }
        }
    }

    private void matchAll(TermMatchListener termMatchListener) throws PropertyEnricherException {
        checkInit();
        denormalizedNodeIds.forEach((id, taxonMap) -> {
            Long acceptedNameId = mergedNodes.get(id);
            if (acceptedNameId == null) {
                List<Map<String, String>> names = denormalizedNodeIds.get(id);
                if (names != null && names.size() > 0) {
                    termMatchListener.foundTaxonForTerm(
                            null,
                            TaxonUtil.mapToTaxon(names.get(0)),
                            TaxonUtil.mapToTaxon(names.get(0)),
                            NameType.HAS_ACCEPTED_NAME
                    );
                }
            } else {
                List<Map<String, String>> synonyms = denormalizedNodeIds.get(id);
                List<Map<String, String>> acceptedNames = denormalizedNodeIds.get(acceptedNameId);
                if (synonyms != null && synonyms.size() > 0
                        && acceptedNames != null && acceptedNames.size() > 0) {
                    termMatchListener.foundTaxonForTerm(
                            null,
                            TaxonUtil.mapToTaxon(synonyms.get(0)),
                            TaxonUtil.mapToTaxon(acceptedNames.get(0)),
                            NameType.SYNONYM_OF
                    );
                }
            }
        });
    }

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enriched = new TreeMap<>(properties);
        String externalId = properties.get(PropertyAndValueDictionary.EXTERNAL_ID);
        if (StringUtils.startsWith(externalId, getTaxonomyProvider().getIdPrefix())) {
            enriched = enrichMatches(enriched, getIdOrNull(externalId, getTaxonomyProvider()), noopListener());
        } else {
            String name = properties.get(PropertyAndValueDictionary.NAME);
            if (StringUtils.isNoneBlank(name)) {
                enriched = enrichMatches(enriched, name, noopListener());
            }
        }
        return enriched;
    }


    public abstract TaxonomyProvider getTaxonomyProvider();

    public TermMatcherContext getCtx() {
        return this.ctx;
    }

    private TermMatchListener noopListener() {
        return (requestId, providedTerm, resolvedTaxon, nameType) -> {

        };
    }

    private Map<String, String> enrichMatches(Map<String, String> enriched, Long key, TermMatchListener listener) throws PropertyEnricherException {
        checkInit();
        Long idForLookup = mergedNodes.getOrDefault(key, key);

        List<Map<String, String>> enrichedProperties = denormalizedNodeIds.get(idForLookup);

        if (enrichedProperties != null && enrichedProperties.size() > 0) {
            NameType type = (key.equals(idForLookup))
                    ? NameType.HAS_ACCEPTED_NAME
                    : NameType.SYNONYM_OF;
            for (Map<String, String> enrichedProperty : enrichedProperties) {
                listener.foundTaxonForTerm(
                        null,
                        TaxonUtil.mapToTaxon(enriched),
                        TaxonUtil.mapToTaxon(enrichedProperty),
                        type);
            }
            enriched = new TreeMap<>(enrichedProperties.get(0));
        }
        return enriched;
    }

    private Map<String, String> enrichMatches(Map<String, String> enriched, String key, TermMatchListener listener) throws PropertyEnricherException {
        checkInit();
        List<Map<String, String>> enrichedProperties = denormalizedNodes.get(key);

        if (enrichedProperties != null && enrichedProperties.size() > 0) {
            Map<String, String> resolved = new TreeMap<>(enrichedProperties.get(0));
            enriched = resolveAcceptedNameIfAvailable(listener, TaxonUtil.mapToTaxon(resolved), TaxonUtil.mapToTaxon(resolved));
        }
        return enriched;
    }

    private Map<String, String> resolveAcceptedNameIfAvailable(TermMatchListener listener,
                                                               Taxon resolvedTaxon,
                                                               Taxon providedTerm) {
        Map<String, String> enriched = null;
        Long providedId = getIdOrNull(resolvedTaxon.getExternalId(), getTaxonomyProvider());

        if (providedId != null) {
            final Long acceptedExternalId = mergedNodes.getOrDefault(providedId, providedId);
            if (acceptedExternalId.equals(providedId)) {
                listener.foundTaxonForTerm(null,
                        providedTerm,
                        resolvedTaxon,
                        NameType.HAS_ACCEPTED_NAME);
            } else {
                List<Map<String, String>> acceptedNameMap = denormalizedNodeIds.get(acceptedExternalId);
                for (Map<String, String> hasSynonym : acceptedNameMap) {
                    listener.foundTaxonForTerm(null,
                            providedTerm,
                            TaxonUtil.mapToTaxon(hasSynonym),
                            NameType.SYNONYM_OF);
                }
                enriched = acceptedNameMap.size() == 0 ? enriched : acceptedNameMap.get(0);
            }
        }
        return enriched == null ? TaxonUtil.taxonToMap(providedTerm) : enriched;
    }

    private void checkInit() throws PropertyEnricherException {
        if (needsInit()) {
            if (ctx == null) {
                throw new PropertyEnricherException("context needed to initialize");
            }
            lazyInit();
        }
    }

    private static Long getIdOrNull(String key, TaxonomyProvider matchingTaxonomyProvider) {
        TaxonomyProvider taxonomyProvider = ExternalIdUtil.taxonomyProviderFor(key);
        return matchingTaxonomyProvider.equals(taxonomyProvider)
                ? Long.parseLong(ExternalIdUtil.stripPrefix(matchingTaxonomyProvider, key))
                : null;
    }

    abstract protected void lazyInit() throws PropertyEnricherException;

    private boolean needsInit() {
        return denormalizedNodes == null;
    }

    @Override
    public void shutdown() {

    }

    protected File getCacheDir() {
        File cacheDir = new File(getCtx().getCacheDir(), StringUtils.lowerCase(getTaxonomyProvider().name()));
        cacheDir.mkdirs();
        return cacheDir;
    }

}
