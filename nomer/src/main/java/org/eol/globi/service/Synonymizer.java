package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Synonymizer implements TermMatcher {

    private final TermMatcher matcher;

    private static final Pattern CAPITALIZED = Pattern.compile("^[A-Z].*");
    private static final Map<String, List<String>> SUFFIX_ALTERNATE_MAP = new TreeMap<String, List<String>>() {{
        put("us", Arrays.asList("a", "um", "or")); // e.g., Mops russatus -> Mops russata
        put("a", Arrays.asList("us", "um")); // e.g., Mops russata -> Mops russatus
        put("um", Arrays.asList("a", "us")); // e.g., Mops russatum -> Mops russatus
        put("is", Arrays.asList("e")); // e.g., Baeodon gracilis -> Baeodon gracile
        put("e", Arrays.asList("is")); // e.g., Styloctenium mindorense -> Styloctenium mindorensis
        put("or", Arrays.asList("us")); //e.g., major -> majus
        put("i", Arrays.asList("ii")); // e.g., Mops bemmeleni -> Mops bemmelenii
        put("ii", Arrays.asList("i")); // e.g., Plecotus christii -> Plecotus christi
    }};

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
                    List<String> synonyms = proposeSynonymForUpToTwoNonGenusNameParts(name);
                    try {
                        matcher.match(synonyms.stream().map(alt -> new TermImpl(term.getId(), alt)).collect(Collectors.toList()), new TermMatchListener() {
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

    static List<String> proposeSynonymForUpToTwoNonGenusNameParts(String name) {
        List<String> heads = Collections.emptyList();
        List<List<String>> alternates = new ArrayList<>();
        if (StringUtils.isNotBlank(name) && CAPITALIZED.matcher(name).matches()) {
            heads = proposeSynonymForUpToTwoNonGenusNameParts(name, heads, alternates);
        }
        List<String> expand = expand(heads, alternates);
        return expand
                .stream()
                .filter(alt -> !StringUtils.equals(name, alt))
                .collect(Collectors.toList());
    }

    private static List<String> proposeSynonymForUpToTwoNonGenusNameParts(String name, List<String> heads, List<List<String>> alternates) {
        String[] parts = StringUtils.split(name, ' ');
        if (parts != null && parts.length > 0) {
            heads = Collections.singletonList(parts[0]);
            int alteredPartCount = 0;
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i];
                List<String> alternateForPart = new ArrayList<>();
                alternateForPart.add(part);
                if (StringUtils.isAllLowerCase(part) && alteredPartCount < 2) {
                    SUFFIX_ALTERNATE_MAP
                            .forEach((key, suffixAlternates) -> {
                                if (StringUtils.endsWith(part, key)) {
                                    String stem = StringUtils.substring(part, 0, part.length() - key.length());
                                    suffixAlternates.forEach(suffixAlt -> alternateForPart.add(stem + suffixAlt));
                                }
                            });
                }
                if (alternateForPart.size() > 1) {
                    alteredPartCount++;
                }
                alternates.add(alternateForPart);
            }
        }
        return heads;
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
