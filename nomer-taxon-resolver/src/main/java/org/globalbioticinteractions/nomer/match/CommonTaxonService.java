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
import org.mapdb.Atomic;
import org.mapdb.BTreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public abstract class CommonTaxonService<T> extends PropertyEnricherSimple implements TermMatcher {

    private static final Logger LOG = LoggerFactory.getLogger(CommonTaxonService.class);

    static final String NODES = "nodes";
    static final String CHILD_PARENT = "childParent";
    static final String MERGED_NODES = "mergedNodes";
    static final String NAME_TO_NODE_IDS = "name2node";


    private final TermMatcherContext ctx;

    BTreeMap<T, T> mergedNodes = null;
    BTreeMap<T, Map<String, String>> nodes;
    BTreeMap<T, T> childParent;
    BTreeMap<String, List<T>> name2nodeIds;
    Atomic.Long datasetKey;
    private String cacheName = null;


    public CommonTaxonService(TermMatcherContext ctx) {
        this.ctx = ctx;
    }

    public void registerIdForName(T childTaxId, Taxon taxon, Map<String, List<T>> name2nodeIds) {
        // include authorship in name indexing/matching if provided
        // https://github.com/globalbioticinteractions/nomer/issues/104
        String nameAuthor = getNameAuthor(taxon);

        registerIdForName(childTaxId, nameAuthor, name2nodeIds);
        if (StringUtils.isNotBlank(taxon.getAuthorship())) {
            registerIdForName(childTaxId, taxon.getName(), name2nodeIds);
        }
    }

    private String getNameAuthor(Taxon name) {
        return StringUtils.isBlank(name.getAuthorship())
                ? name.getName()
                : name.getName() + name.getAuthorship();
    }


    protected void registerIdForName(T childTaxId, String name, Map<String, List<T>> name2nodeIds) {
        if (StringUtils.isNoneBlank(name)) {
            List<T> ids = name2nodeIds.get(name);
            List<T> updatedIds;
            if (ids == null) {
                updatedIds = Collections.singletonList(childTaxId);
            } else {
                TreeSet<T> ts = new TreeSet<>(ids);
                ts.add(childTaxId);
                updatedIds = new ArrayList<>(ts);
            }
            name2nodeIds.put(name, updatedIds);
        }
    }

    @Override
    public void match(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException {
        for (Term term : terms) {
            if (TermMatchUtil.shouldMatchAll(term, getCtx().getInputSchema())) {
                matchAll(termMatchListener);
            } else {
                Taxon providedTaxon =
                        term instanceof Taxon
                                ? (Taxon) term
                                : new TaxonImpl(term.getName(), term.getId());
                if (isIdSchemeSupported(term.getId())) {
                    enrichIdMatches(providedTaxon, termMatchListener);
                } else if (StringUtils.isNoneBlank(term.getName())) {
                    enrichNameMatches(providedTaxon, termMatchListener);
                }
            }
        }
    }

    private void matchAll(final TermMatchListener termMatchListener) throws PropertyEnricherException {
        checkInit();

        nodes.forEach((id, taxonMap) -> {
            Taxon taxonFrom = resolveTaxon(id, childParent, nodes, getTaxonomyProvider());
            Taxon taxonTo;
            NameType nameType;
            T acceptedId = mergedNodes == null ? null : mergedNodes.get(id);
            if (acceptedId == null) {
                taxonTo = taxonFrom;
                termMatchListener.foundTaxonForTerm(
                        null,
                        taxonFrom,
                        uncheckedOrAccepted(taxonFrom),
                        taxonTo
                );
            } else {
                taxonTo = resolveTaxon(acceptedId, childParent, nodes, getTaxonomyProvider());
                if (taxonTo == null) {
                    LOG.warn("failed to resolve [" + taxonFrom.getExternalId() + ";" + taxonFrom.getName() + "]: does accepted name id [" + acceptedId + "] exist?");
                } else {
                    if (!StringUtils.equals(taxonTo.getExternalId(), taxonFrom.getExternalId())) {
                        nameType = NameType.SYNONYM_OF;
                        termMatchListener.foundTaxonForTerm(
                                null,
                                taxonFrom,
                                nameType,
                                taxonTo
                        );
                    }
                }

            }

        });

        name2nodeIds.forEach((name, ids) -> {
            for (T id : ids) {
                Taxon taxonTo = resolveTaxon(id, childParent, nodes, getTaxonomyProvider());
                if (taxonTo != null) {
                    registerRelation(termMatchListener, name, id, taxonTo);
                }
            }
        });
    }

    private static boolean isUnchecked(Taxon taxon) {
        return taxon.getStatus() != null
                && NameType.HAS_UNCHECKED_NAME.name()
                .equals(taxon.getStatus().getName());
    }

    private void registerRelation(TermMatchListener termMatchListener, String name, T id, Taxon taxonTo) {
        T acceptedId = mergedNodes == null ? null : mergedNodes.get(id);
        if (mergedNodes != null && acceptedId == null) {
            termMatchListener.foundTaxonForTerm(
                    null,
                    new TaxonImpl(null, name),
                    NameType.HAS_ACCEPTED_NAME,
                    taxonTo);

        } else {
            Taxon taxonIdOrName = isIdSchemeSupported(name)
                    ? new TaxonImpl(null, name)
                    : new TaxonImpl(name, null);

            if (!StringUtils.equals(taxonIdOrName.getExternalId(), taxonTo.getExternalId())) {
                termMatchListener.foundTaxonForTerm(
                        null,
                        taxonIdOrName,
                        NameType.SYNONYM_OF,
                        taxonTo);
            }
        }
    }

    boolean isIdSchemeSupported(String externalId) {
        TaxonomyProvider provider = getTaxonomyProvider();
        return isIdSupportedBy(externalId, provider);
    }

    static boolean isIdSupportedBy(String externalId, TaxonomyProvider provider) {
        TaxonomyProvider taxonomyProvider = ExternalIdUtil.taxonomyProviderFor(externalId);
        return taxonomyProvider != null && taxonomyProvider.equals(provider);
    }

    protected String getIdPrefix() {
        return getTaxonomyProvider().getIdPrefix();
    }


    @Override
    public Map<String, String> enrich(Map<String, String> toBeEnriched) throws PropertyEnricherException {
        checkInit();
        Map<String, String> enriched = new TreeMap<>(toBeEnriched);
        String externalId = toBeEnriched.get(PropertyAndValueDictionary.EXTERNAL_ID);
        if (isIdSchemeSupported(externalId)) {
            enriched = enrichIdMatches(
                    TaxonUtil.mapToTaxon(enriched),
                    noopListener());
        } else {
            String name = toBeEnriched.get(PropertyAndValueDictionary.NAME);
            if (StringUtils.isNoneBlank(name)) {
                enriched = enrichNameMatches(TaxonUtil.mapToTaxon(enriched), noopListener());
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

    private Map<String, String> enrichIdMatches(Taxon toBeEnriched,
                                                TermMatchListener listener) throws PropertyEnricherException {
        checkInit();
        T key = getIdOrNull(toBeEnriched, getTaxonomyProvider());
        Map<String, String> enriched;
        if (key == null) {
            enriched = emitNoMatch2(toBeEnriched, listener);
        } else {
            T idForLookup = mergedNodeOrDefault(key);

            Taxon taxon = resolveTaxon(
                    idForLookup,
                    childParent,
                    nodes,
                    getTaxonomyProvider()
            );

            if (taxon == null) {
                enriched = emitNoMatch2(toBeEnriched, listener);
            } else {
                enriched = emitMatch(
                        toBeEnriched,
                        key,
                        listener,
                        idForLookup,
                        Collections.singletonList(TaxonUtil.taxonToMap(taxon))
                );
            }
        }
        return enriched;
    }

    private Map<String, String> emitNoMatch2(Taxon toBeEnriched, TermMatchListener listener) {
        emitNoMatch(toBeEnriched, listener);
        return TaxonUtil.taxonToMap(toBeEnriched);
    }

    private Map<String, String> emitMatch(Taxon toBeEnriched,
                                          T key,
                                          TermMatchListener listener,
                                          T idForLookup,
                                          List<Map<String, String>> enrichedProperties) {
        NameType type = (key.equals(idForLookup))
                ? NameType.HAS_ACCEPTED_NAME
                : NameType.SYNONYM_OF;
        for (Map<String, String> enrichedProperty : enrichedProperties) {
            Taxon taxon = TaxonUtil.mapToTaxon(enrichedProperty);
            if (isUnchecked(taxon)) {
                type = NameType.HAS_UNCHECKED_NAME;
            }
            listener.foundTaxonForTerm(
                    null,
                    toBeEnriched,
                    type,
                    taxon
            );
        }
        return new TreeMap<>(enrichedProperties.get(0));
    }

    private T mergedNodeOrDefault(T defaultKey) {
        return mergedNodes == null || defaultKey == null
                ? defaultKey
                : mergedNodes.getOrDefault(defaultKey, defaultKey);
    }

    private void emitNoMatch(Taxon term, TermMatchListener listener) {
        listener.foundTaxonForTerm(
                null,
                term,
                NameType.NONE,
                term);
    }

    private Map<String, String> enrichNameMatches(Taxon providedTaxon,
                                                  TermMatchListener listener) throws PropertyEnricherException {
        checkInit();
        Map<String, String> enriched = TaxonUtil.taxonToMap(providedTaxon);
        String taxonNameAndAuthorship = getNameAuthor(providedTaxon);
        if (StringUtils.isBlank(taxonNameAndAuthorship)) {
            emitNoMatch(providedTaxon, listener);
        } else {
            List<T> taxonKeys = name2nodeIds == null
                    ? Collections.emptyList()
                    : name2nodeIds.get(taxonNameAndAuthorship);

            if (taxonKeys == null || taxonKeys.isEmpty()) {
                emitNoMatch(providedTaxon, listener);
            } else {
                for (T taxonKey : new TreeSet<>(taxonKeys)) {
                    final T acceptedExternalId = mergedNodeOrDefault(taxonKey);
                    if (acceptedExternalId != null) {
                        Taxon resolvedTaxon = resolveTaxon(
                                acceptedExternalId, childParent, nodes, getTaxonomyProvider()
                        );
                        if (resolvedTaxon != null) {
                            enriched = TaxonUtil.taxonToMap(resolvedTaxon);

                            if (acceptedExternalId.equals(taxonKey)) {
                                listener.foundTaxonForTerm(null,
                                        resolvedTaxon,
                                        uncheckedOrAccepted(resolvedTaxon),
                                        resolvedTaxon
                                );
                            } else {
                                listener.foundTaxonForTerm(null,
                                        providedTaxon,
                                        NameType.SYNONYM_OF,
                                        resolvedTaxon
                                );
                            }
                        }
                    }
                }
            }
        }
        return enriched;
    }

    private NameType uncheckedOrAccepted(Taxon resolvedTaxon) {
        return isUnchecked(resolvedTaxon)
                ? NameType.HAS_UNCHECKED_NAME
                : NameType.HAS_ACCEPTED_NAME;
    }


    private void checkInit() throws PropertyEnricherException {
        if (needsInit()) {
            if (ctx == null) {
                throw new PropertyEnricherException("context needed to initialize");
            }
            File cacheDir = getCacheDir();
            if (!cacheDir.exists()) {
                if (!cacheDir.mkdirs()) {
                    throw new PropertyEnricherException("failed to create cache dir at [" + cacheDir.getAbsolutePath() + "]");
                }
            }

            lazyInit();
        }
    }

    abstract public T getIdOrNull(Taxon taxon, TaxonomyProvider matchingTaxonomyProvider);


    abstract protected void lazyInit() throws PropertyEnricherException;

    private boolean needsInit() {
        return nodes == null;
    }

    @Override
    public void shutdown() {

    }

    protected File getCacheDir() {
        File file = new File(getCtx().getCacheDir(), getCacheName());
        file.mkdirs();
        return file;
    }

    public String getCacheName() {
        return StringUtils.isBlank(cacheName) ?
                StringUtils.lowerCase(getTaxonomyProvider().name())
                : cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }


    private Taxon resolveTaxon(T focalTaxonKey,
                               Map<T, T> childParent,
                               Map<T, Map<String, String>> nodes,
                               TaxonomyProvider primaryTaxonProvider) {
        Taxon resolvedTaxon = null;
        Map<String, String> resolveTaxonMap = nodes.get(focalTaxonKey);
        if (resolveTaxonMap != null) {
            resolvedTaxon = TaxonUtil.mapToTaxon(resolveTaxonMap);
            resolveHierarchyIfNeeded(focalTaxonKey, childParent, nodes, primaryTaxonProvider, resolvedTaxon);
        }
        return resolvedTaxon;
    }

    private void resolveHierarchyIfNeeded(
            T focalTaxonKey,
            Map<T, T> childParent,
            Map<T, Map<String, String>> nodes,
            TaxonomyProvider primaryTaxonProvider,
            Taxon resolvedTaxon
    ) {
        if (shouldResolveHierarchy(childParent, resolvedTaxon)) {
            List<String> pathNames = new ArrayList<>();
            List<String> pathIds = new ArrayList<>();
            List<String> path = new ArrayList<>();


            path.add(StringUtils.defaultIfBlank(resolvedTaxon.getName(), ""));

            pathIds.add(resolvedTaxon.getExternalId());

            pathNames.add(StringUtils.defaultIfBlank(resolvedTaxon.getRank(), ""));

            T parent = childParent.get(focalTaxonKey);
            List<T> visitedParents = new ArrayList<T>();
            visitedParents.add(focalTaxonKey);
            while (parent != null
                    && !visitedParents.contains(parent)
                    && !pathIds.contains(getIdPrefix() + parent)) {
                Map<String, String> parentTaxonProperties = nodes.get(parent);
                if (parentTaxonProperties != null) {
                    Taxon parentTaxon = TaxonUtil.mapToTaxon(parentTaxonProperties);
                    path.add(StringUtils.defaultIfBlank(parentTaxon.getName(), ""));
                    pathNames.add(StringUtils.defaultIfBlank(parentTaxon.getRank(), ""));
                    pathIds.add(parentTaxon.getExternalId());
                }
                visitedParents.add(parent);
                parent = childParent.get(parent);
            }

            Collections.reverse(pathNames);
            Collections.reverse(pathIds);
            Collections.reverse(path);

            resolvedTaxon.setPath(StringUtils.join(path, CharsetConstant.SEPARATOR));
            resolvedTaxon.setPathIds(StringUtils.join(pathIds, CharsetConstant.SEPARATOR));
            resolvedTaxon.setPathNames(StringUtils.join(pathNames, CharsetConstant.SEPARATOR));
        }
    }

    protected boolean shouldResolveHierarchy(Map<T, T> childParent, Taxon resolvedTaxon) {
        return childParent != null && resolvedTaxon != null;
    }

}
