package org.eol.globi.taxon;

import org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PeriodAsWhitespaceUndoer implements org.eol.globi.service.NameSuggester {

    final private Pattern pattern = Pattern.compile("(([a-zA-Z]+[.][a-z]+[.]*)+)+");

    @Override
    public String suggest(final String name) {
        String suggestion = name;
        Matcher matcher = pattern.matcher(name);
        if (matcher.matches()) {
            suggestion = Capitalizer.capitalize(StringUtils.replace(name, ".", " "));
        }
        return suggestion;
    }

}
