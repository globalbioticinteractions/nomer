package org.eol.globi.taxon;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.service.NameSuggester;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SnakeCaseUndoer implements NameSuggester {

    final private Pattern pattern = Pattern.compile("(([a-z])+_*)+");

    @Override
    public String suggest(final String name) {
        String suggestion = name;
        Matcher matcher = pattern.matcher(name);
        if (matcher.matches()) {
            suggestion = Capitalizer.capitalize(StringUtils.replace(name, "_", " "));
        }
        return suggestion;
    }

}
