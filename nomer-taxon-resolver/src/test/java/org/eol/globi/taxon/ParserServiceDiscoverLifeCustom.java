package org.eol.globi.taxon;

import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParserServiceDiscoverLifeCustom implements org.globalbioticinteractions.nomer.match.ParserService {

    public static final String NAME = "(?<name>([A-Z][a-z]+)([ ][a-z]+)([ ][a-z]+){0,1})";
    public static final String NAME_PARENTHESIS = "(?<name>[A-Z][a-z]+[ ][(][A-Z][a-z]+[)][ ][a-z]+)";
    public static final String AUTHORSHIP = "(?<authorship>[^,]+[,][ ][0-9]{4})";
    public static final String AUTHORSHIP_PARENTHESIS = "(?<authorship>[(][^,]+[,][ ][0-9]{4}[)])";
    public static final String AUTHORSHIP_AND = "(?<authorship>([A-Z][a-z]+)([ ]and[ ])([A-Z][a-z]+)[,][ ][0-9]{4})";
    public static final String SPACE = "[ ]+";
    public static final String NAME_AUTHORSHIP_MULTIPLE_AUTHORS
            = NAME + SPACE + AUTHORSHIP_AND;
    public static final String NAME_WITH_PARENTHESIS
            = NAME_PARENTHESIS + SPACE + AUTHORSHIP;
    public static final String NAME_AUTHORSHIP
            = NAME + SPACE + AUTHORSHIP;
    public static final String NAME_AUTHORSHIP_PARENTHESES
            = NAME + SPACE + AUTHORSHIP_PARENTHESIS;
    public static final String NOTE = "(?<note>[_][_a-z]+)";
    public static final String NAME_WITH_NOTE
            = NAME + NOTE + SPACE + AUTHORSHIP;

    public static final List<String> NAME_PATTERNS = Arrays.asList(
            NAME_AUTHORSHIP,
            NAME_AUTHORSHIP_PARENTHESES,
            NAME_WITH_NOTE,
            NAME_WITH_PARENTHESIS,
            NAME_AUTHORSHIP_MULTIPLE_AUTHORS);

    @Override
    public Taxon parse(Term term, String name) throws PropertyEnricherException {
        Taxon matched = null;

        for (String namePattern : NAME_PATTERNS) {
            Pattern compile = Pattern.compile(namePattern);
            Matcher matcher = compile.matcher(name);
            if (matcher.matches()) {
                matched = new TaxonImpl(matcher.group("name"));
                matched.setAuthorship(matcher.group("authorship"));
                break;
            }

        }

        DiscoverLifeUtilXML.inferStatus(name, matched);

        return matched;
    }
}
