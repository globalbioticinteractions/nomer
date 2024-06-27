package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Synonymizer implements TermMatcher {

    private final TermMatcher matcher;

    public Synonymizer(TermMatcher matcher) {
        this.matcher = matcher;
    }

    @Override
    public void match(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException {
        matcher.match(terms, new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                final AtomicBoolean rematched = new AtomicBoolean(false);
                if (NameType.NONE.equals(nameType)) {
                    String name = term.getName();
                    List<String> alternates = proposeNameAlternate(name);
                    try {
                        matcher.match(alternates.stream().map(alt -> new TermImpl(term.getId(), alt)).collect(Collectors.toList()), new TermMatchListener() {
                            @Override
                            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                                if (!NameType.NONE.equals(nameType)) {
                                    rematched.set(true);
                                    termMatchListener.foundTaxonForTerm(aLong, term, NameType.SYNONYM_OF, taxon);
                                }
                            }
                        });
                    } catch (PropertyEnricherException e) {
                        //
                    }

                }
                if (!rematched.get()) {
                    termMatchListener.foundTaxonForTerm(aLong, term, nameType, taxon);
                }
            }
        });

    }

    public static List<String> proposeNameAlternate(String name) {
        String[] parts = StringUtils.split(name, ' ');
        List<List<String>> alternates = new ArrayList<>();
        List<String> heads = Collections.emptyList();
        if (parts != null && parts.length > 0) {
            heads = Collections.singletonList(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i];
                List<String> alternateForPart = new ArrayList<>();
                alternateForPart.add(part);
                if (StringUtils.isAllLowerCase(part)) {
                    if (StringUtils.endsWith(part, "ii")) {
                        alternateForPart.add(StringUtils.substring(part, 0, part.length() - 1));
                    } else if (StringUtils.endsWith(part, "i")) {
                        alternateForPart.add(part + "i");
                    }
                    if (StringUtils.endsWith(part, "us")) {
                        alternateForPart.add(StringUtils.substring(part, 0, part.length() - 2) + "a");
                        alternateForPart.add(StringUtils.substring(part, 0, part.length() - 2) + "um");
                        alternateForPart.add(StringUtils.substring(part, 0, part.length() - 2) + "or");
                    }
                    if (StringUtils.endsWith(part, "a")) {
                        alternateForPart.add(StringUtils.substring(part, 0, part.length() - 1) + "us");
                        alternateForPart.add(StringUtils.substring(part, 0, part.length() - 1) + "um");
                    }
                    if (StringUtils.endsWith(part, "um")) {
                        String stem = StringUtils.substring(part, 0, part.length() - 2);
                        alternateForPart.add(stem + "us");
                        alternateForPart.add(StringUtils.substring(part, 0, part.length() - 2) + "a");
                    }
                    if (StringUtils.endsWith(part, "is")) {
                        alternateForPart.add(StringUtils.substring(part, 0, part.length() - 2) + "e");
                    }
                    if (StringUtils.endsWith(part, "e")) {
                        alternateForPart.add(StringUtils.substring(part, 0, part.length() - 1) + "is");
                    }
                    if (StringUtils.endsWith(part, "or")) {
                        alternateForPart.add(StringUtils.substring(part, 0, part.length() - 2) + "us");
                    }
                }
                alternates.add(alternateForPart);
            }
        }
        List<String> expand = expand(heads, alternates);
        return expand
                .stream()
                .filter(alt -> !StringUtils.equals(name, alt))
                .collect(Collectors.toList());
    }

    public static List<String> expand(List<String> heads, List<List<String>> tail) {
        List<String> newHeads = heads;
        if (tail.size() > 0) {
            List<String> positionAlternates = tail.get(0);
            newHeads = new ArrayList<>();
            for (String positionAlternate : positionAlternates) {
                for (String head : heads) {
                    newHeads.add(head + " " + positionAlternate);
                }
            }
            if (tail.size() > 1) {
                List<List<String>> newTail = new ArrayList<>(tail.subList(1, tail.size()));
                newHeads = expand(newHeads, newTail);
            }
        }
        return newHeads;
    }

}
