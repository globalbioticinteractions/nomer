package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.eol.globi.tool.TermRequestImpl;
import org.eol.globi.util.CSVTSVUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TermMatcherHierarchical implements TermMatcher {

    private final TermMatcher matcher;

    public TermMatcherHierarchical(TermMatcher matcher) {
        this.matcher = matcher;
    }

    @Override
    public void match(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException {

        Map<Long, Taxon> providedTaxonForId = new HashMap<>();
        Map<Long, Term> origTermForId = new HashMap<>();
        AtomicLong idGenerator = new AtomicLong(0);

        List<Term> unpacked = terms.stream()
                .map(x -> {

                    String name = lastOrNull(x.getName());
                    String id = lastOrNull(x.getId());

                    String path = allExceptLastOrNull(x.getName());
                    if (x instanceof Taxon && StringUtils.isNotBlank(((Taxon)x).getPath())) {
                        List<String> pathParts = Stream.of(path, ((Taxon) x).getPath())
                                .map(StringUtils::trim)
                                .filter(StringUtils::isNoneBlank)
                                .collect(Collectors.toList());
                        path = StringUtils.join(pathParts, CharsetConstant.SEPARATOR);
                    }

                    String pathIds = allExceptLastOrNull(x.getId());
                    if (x instanceof Taxon && StringUtils.isNotBlank(((Taxon)x).getPathIds())) {
                        List<String> pathIdParts = Stream.of(pathIds, ((Taxon) x).getPathIds())
                                .map(StringUtils::trim)
                                .filter(StringUtils::isNoneBlank)
                                .collect(Collectors.toList());
                        pathIds = StringUtils.join(pathIdParts, CharsetConstant.SEPARATOR);
                    }

                    Term unpackedTerm = x;
                    if ((StringUtils.isNotBlank(name) && StringUtils.isNotBlank(path))
                            || (StringUtils.isNotBlank(id) && StringUtils.isNotBlank(pathIds))) {
                        TaxonImpl taxon = new TaxonImpl(name, id);
                        taxon.setPath(path);
                        taxon.setPathIds(pathIds);
                        Long requestId = x instanceof TermRequestImpl ? ((TermRequestImpl) x).getNodeId() : null;
                        requestId = requestId == null ? idGenerator.getAndIncrement() : requestId;
                        providedTaxonForId.put(requestId, taxon);
                        origTermForId.put(requestId, x);
                        unpackedTerm = new TaxonRequestImpl(id, name, requestId);
                    }
                    return unpackedTerm;
                }).collect(Collectors.toList());

        matcher.match(unpacked, new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long requestId, Term term, NameType nameType, Taxon resolvedTaxon) {
                Long derivedRequestId = requestId == null ? (term instanceof TaxonRequestImpl ? ((TaxonRequestImpl) term).getRequestId() : requestId) : requestId;
                Taxon providedTaxon = providedTaxonForId.get(derivedRequestId);
                Term origTerm = origTermForId.get(derivedRequestId);
                NameType matchType = providedTaxon == null ? nameType : getMatchType(providedTaxon, resolvedTaxon, nameType);
                termMatchListener.foundTaxonForTerm(
                        derivedRequestId,
                        origTerm == null ? term : origTerm,
                        matchType,
                        resolvedTaxon
                );
            }

            private NameType getMatchType(Taxon providedTaxon, Taxon resolvedTaxon, NameType nameType) {
                NameType matchType = nameType;
                if (!NameType.NONE.equals(nameType)) {
                    boolean namesMatch = hasConsistentHierarchy(providedTaxon.getPath(), resolvedTaxon.getPath());
                    boolean idsMatch = hasConsistentHierarchy(providedTaxon.getPathIds(), resolvedTaxon.getPathIds());
                    matchType = (!namesMatch || !idsMatch) ? NameType.NONE : nameType;
                }
                return matchType;
            }

            private boolean hasConsistentHierarchy(String provided, String resolved) {
                boolean namesMatch = false;
                String[] providedNames = CSVTSVUtil.splitPipes(provided);
                String[] resolvedNames = CSVTSVUtil.splitPipes(resolved);
                if (providedNames != null && resolvedNames != null) {
                    List<String> providedNamesTrimmed = filterList(providedNames);
                    List<String> resolvedNamesTrimmed = filterList(resolvedNames);
                    namesMatch = resolvedNamesTrimmed.containsAll(providedNamesTrimmed);
                } else if (providedNames == null) {
                    namesMatch = true;
                }
                return namesMatch;
            }

            private List<String> filterList(String[] providedNames) {
                return Stream.of(providedNames)
                        .filter(StringUtils::isNotBlank)
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
            }
        });
    }

    private String lastOrNull(String name1) {
        String[] values1 = CSVTSVUtil.splitPipes(name1);
        return (values1 == null || values1.length == 0) ? null : StringUtils.trim(values1[values1.length - 1]);
    }

    private String allExceptLastOrNull(String name1) {
        int i = StringUtils.lastIndexOf(name1, CharsetConstant.SEPARATOR_CHAR);
        return i == -1 ? null : StringUtils.substring(name1, 0, i);
    }
}
