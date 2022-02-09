package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public abstract class CommonTaxonService<T> extends PropertyEnricherSimple implements TermMatcher {

    protected static final String DENORMALIZED_NODES = "denormalizedNodes";
    protected static final String DENORMALIZED_NODE_IDS = "denormalizedNodeIds";
    protected static final String MERGED_NODES = "mergedNodes";


    private final TermMatcherContext ctx;

    protected BTreeMap<T, T> mergedNodes = null;
    protected BTreeMap<String, List<Map<String, String>>> denormalizedNodes = null;
    protected BTreeMap<T, List<Map<String, String>>> denormalizedNodeIds = null;

    public CommonTaxonService(TermMatcherContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void match(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException {
        for (Term term : terms) {
            if (TermMatchUtil.shouldMatchAll(term, getCtx().getInputSchema())) {
                matchAll(termMatchListener);
            } else {
                Taxon taxon = new TaxonImpl(term.getName(), term.getId());
                TaxonomyProvider taxonomyProvider = ExternalIdUtil.taxonomyProviderFor(term.getId());
                if (getTaxonomyProvider().equals(taxonomyProvider)) {
                    String id = term.getId();
                    enrichMatches(TaxonUtil.taxonToMap(taxon), getIdOrNull(id, getTaxonomyProvider()), termMatchListener);
                } else if (StringUtils.isNoneBlank(term.getName())) {
                    enrichNameMatches(TaxonUtil.taxonToMap(taxon), term.getName(), termMatchListener);
                }
            }
        }
    }

    private void matchAll(TermMatchListener termMatchListener) throws PropertyEnricherException {
        checkInit();
        denormalizedNodeIds.forEach((id, taxonMap) -> {
            T acceptedNameId = mergedNodeOrDefault(null);
            if (acceptedNameId == null) {
                List<Map<String, String>> names = denormalizedNodeIds.get(id);
                if (names != null && names.size() > 0) {
                    termMatchListener.foundTaxonForTerm(
                            null,
                            TaxonUtil.mapToTaxon(names.get(0)),
                            NameType.HAS_ACCEPTED_NAME,
                            TaxonUtil.mapToTaxon(names.get(0))
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
                            NameType.SYNONYM_OF,
                            TaxonUtil.mapToTaxon(acceptedNames.get(0))
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
                enriched = enrichNameMatches(enriched, name, noopListener());
            }
        }
        return enriched;
    }


    public abstract TaxonomyProvider getTaxonomyProvider();

    public TermMatcherContext getCtx() {
        return this.ctx;
    }

    private TermMatchListener noopListener() {
        return new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {

            }
        };
    }

    private Map<String, String> enrichMatches(Map<String, String> enriched, T key, TermMatchListener listener) throws PropertyEnricherException {
        checkInit();
        if (key == null) {
            emitNoMatch(enriched, listener);
        } else {
            T idForLookup = mergedNodeOrDefault(key);

            List<Map<String, String>> enrichedProperties =
                    idForLookup == null
                            ? Collections.emptyList()
                            : denormalizedNodeIds.get(idForLookup);

            if (enrichedProperties != null && enrichedProperties.size() > 0) {
                NameType type = (key.equals(idForLookup))
                        ? NameType.HAS_ACCEPTED_NAME
                        : NameType.SYNONYM_OF;
                for (Map<String, String> enrichedProperty : enrichedProperties) {
                    listener.foundTaxonForTerm(
                            null,
                            TaxonUtil.mapToTaxon(enriched),
                            type,
                            TaxonUtil.mapToTaxon(enrichedProperty)
                    );
                }
                enriched = new TreeMap<>(enrichedProperties.get(0));
            } else {
                emitNoMatch(enriched, listener);
            }
        }
        return enriched;
    }

    private T mergedNodeOrDefault(T defaultKey) {
        return mergedNodes == null || defaultKey == null
                ? defaultKey
                : mergedNodes.getOrDefault(defaultKey, defaultKey);
    }

    private void emitNoMatch(Map<String, String> enriched, TermMatchListener listener) {
        listener.foundTaxonForTerm(
                null,
                TaxonUtil.mapToTaxon(enriched),
                NameType.NONE,
                TaxonUtil.mapToTaxon(enriched));
    }

    private Map<String, String> enrichNameMatches(Map<String, String> enriched,
                                                  String key,
                                                  TermMatchListener listener) throws PropertyEnricherException {
        checkInit();
        if (StringUtils.isBlank(key)) {
            emitNoMatch(enriched, listener);
        } else {
            List<Map<String, String>> enrichedProperties = denormalizedNodes.get(key);

            if (enrichedProperties != null && enrichedProperties.size() > 0) {
                Map<String, String> resolved = new TreeMap<>(enrichedProperties.get(0));
                enriched = resolveAcceptedNameIfAvailable(listener, TaxonUtil.mapToTaxon(resolved), TaxonUtil.mapToTaxon(resolved));
            } else {
                emitNoMatch(enriched, listener);
            }
        }
        return enriched;
    }

    private Map<String, String> resolveAcceptedNameIfAvailable(TermMatchListener listener,
                                                               Taxon resolvedTaxon,
                                                               Taxon providedTerm) {
        Map<String, String> enriched = null;
        T providedId = getIdOrNull(resolvedTaxon.getExternalId(), getTaxonomyProvider());

        if (providedId != null) {
            final T acceptedExternalId = mergedNodeOrDefault(providedId);
            if (acceptedExternalId.equals(providedId)) {
                listener.foundTaxonForTerm(null,
                        providedTerm,
                        NameType.HAS_ACCEPTED_NAME,
                        resolvedTaxon
                );
            } else {
                List<Map<String, String>> acceptedNameMap = denormalizedNodeIds.get(acceptedExternalId);
                for (Map<String, String> hasSynonym : acceptedNameMap) {
                    listener.foundTaxonForTerm(null,
                            providedTerm,
                            NameType.SYNONYM_OF,
                            TaxonUtil.mapToTaxon(hasSynonym)
                    );
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

    abstract public T getIdOrNull(String key, TaxonomyProvider matchingTaxonomyProvider);


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

    void denormalizeTaxa(Map<T, Map<String, String>> taxonMap,
                         Map<String, List<Map<String, String>>> taxonMapDenormalized,
                         Map<T, List<Map<String, String>>> taxonMapIdDenormalized,
                         Map<T, T> childParent) {
        Set<Map.Entry<T, Map<String, String>>> taxa = taxonMap.entrySet();
        for (Map.Entry<T, Map<String, String>> taxon : taxa) {
            denormalizeTaxa(taxonMap,
                    taxonMapDenormalized,
                    taxonMapIdDenormalized,
                    childParent,
                    taxon,
                    getTaxonomyProvider());
        }
    }

    private void denormalizeTaxa(Map<T, Map<String, String>> taxonMap,
                                 Map<String, List<Map<String, String>>> taxonEnrichMap,
                                 Map<T, List<Map<String, String>>> taxonEnrichIdMap,
                                 Map<T, T> childParent,
                                 Map.Entry<T, Map<String, String>> taxon,
                                 TaxonomyProvider taxonProvider) {
        Map<String, String> childTaxon = taxon.getValue();
        List<String> pathNames = new ArrayList<>();
        List<String> pathIds = new ArrayList<>();
        List<String> path = new ArrayList<>();

        Taxon origTaxon = TaxonUtil.mapToTaxon(childTaxon);

        path.add(StringUtils.defaultIfBlank(origTaxon.getName(), ""));

        pathIds.add(origTaxon.getExternalId());

        pathNames.add(StringUtils.defaultIfBlank(origTaxon.getRank(), ""));

        T parent = childParent.get(taxon.getKey());
        while (parent != null && !pathIds.contains(taxonProvider.getIdPrefix() + parent)) {
            Map<String, String> parentTaxonProperties = taxonMap.get(parent);
            if (parentTaxonProperties != null) {
                Taxon parentTaxon = TaxonUtil.mapToTaxon(parentTaxonProperties);
                path.add(StringUtils.defaultIfBlank(parentTaxon.getName(), ""));
                pathNames.add(StringUtils.defaultIfBlank(parentTaxon.getRank(), ""));
                pathIds.add(parentTaxon.getExternalId());
            }
            parent = childParent.get(parent);
        }

        Collections.reverse(pathNames);
        Collections.reverse(pathIds);
        Collections.reverse(path);

        origTaxon.setPath(StringUtils.join(path, CharsetConstant.SEPARATOR));
        origTaxon.setPathIds(StringUtils.join(pathIds, CharsetConstant.SEPARATOR));
        origTaxon.setPathNames(StringUtils.join(pathNames, CharsetConstant.SEPARATOR));

        updateId(taxonEnrichIdMap,
                taxon.getKey(),
                origTaxon);

        update(taxonEnrichMap, origTaxon.getName(), origTaxon);
    }

    private void update(Map<String, List<Map<String, String>>> taxonEnrichMap,
                        String key,
                        Taxon origTaxon) {
        List<Map<String, String>> existing = taxonEnrichMap.get(key);

        List<Map<String, String>> updated = existing == null
                ? new ArrayList<>()
                : new ArrayList<>(existing);

        updated.add(TaxonUtil.taxonToMap(origTaxon));
        taxonEnrichMap.put(key, updated);
    }

    private void updateId(
            Map<T, List<Map<String, String>>> taxonEnrichIdMap,
            T key,
            Taxon origTaxon
    ) {
        List<Map<String, String>> existing = taxonEnrichIdMap.get(key);

        List<Map<String, String>> updated = existing == null
                ? new ArrayList<>()
                : new ArrayList<>(existing);

        updated.add(TaxonUtil.taxonToMap(origTaxon));
        taxonEnrichIdMap.put(key, updated);
    }


}
