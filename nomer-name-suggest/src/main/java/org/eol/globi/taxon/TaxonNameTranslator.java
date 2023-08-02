package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.NameSuggester;
import org.eol.globi.service.PropertyEnricherException;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class TaxonNameTranslator extends TaxonNameSuggestorBase {

    private static final Logger LOG = LoggerFactory.getLogger(TaxonNameTranslator.class);
    public static final Pattern PATTERN_US = Pattern.compile("^(?<prefix>[A-Z][a-z]+[ ][a-z]+)(?<suffix>(us|a))$");

    public TaxonNameTranslator() {
        this(null);
    }

    public TaxonNameTranslator(TermMatcherContext ctx) {
        super(ctx);
        setSuggestors(initSuggestors());
    }

    private List<NameSuggester> initSuggestors() {
        return Arrays.asList(new NameSuggester() {
            private final Map<String, String> SUFFIX = new TreeMap<String, String>() {{
                put("us", "a");
                put("a", "us");
            }};

            @Override
            public String suggest(String name) {
                Matcher matcher = PATTERN_US.matcher(name);
                return matcher.matches()
                        ? matcher.group("prefix") + SUFFIX.get(matcher.group("suffix"))
                        : name;
            }
        }, new NameSuggester() {

            @Override
            public String suggest(String name) {
                String suggestion = name;
                if (StringUtils.endsWith(name, "ii")) {
                    suggestion = name.substring(0, name.length() - 2) + "i";
                } else if (StringUtils.endsWith(name, "i")) {
                    suggestion = name.substring(0, name.length() - 1) + "ii";
                }
                return suggestion;
            }
        });
    }

    @Override
    public void match(List<Term> terms, TermMatchListener listener) throws PropertyEnricherException {
        // always do identity mapping
        terms.forEach(term -> {
            listener.foundTaxonForTerm(null,
                    term,
                    NameType.SAME_AS,
                    new TaxonImpl(term.getName(), term.getId())
            );
        });


        for (NameSuggester suggestor : getSuggestors()) {
            Stream<Triple<Term, NameType, Term>> correctedTerms = terms.stream()
                    .map(term -> Triple.of(term, NameType.SAME_AS, new TermImpl(term.getId(), suggestor.suggest(term.getName()))));


            correctedTerms.forEach(term -> {
                if (!StringUtils.equals(term.getLeft().getName(), term.getRight().getName())) {
                    listener.foundTaxonForTerm(null,
                            term.getLeft(),
                            term.getMiddle(),
                            new TaxonImpl(term.getRight().getName(), term.getRight().getId())
                    );
                }
            });
        }

    }

}
