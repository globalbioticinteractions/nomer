package org.eol.globi.taxon;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.service.NameSuggester;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AllLowerCaseUndoer implements NameSuggester {

    final private Pattern pattern = Pattern.compile("(([a-z]){2,}\\s*)+");

    @Override
    public String suggest(final String name) {
        String suggestion = name;
        Matcher matcher = pattern.matcher(name);
        if (matcher.matches()) {
            suggestion = StringUtils.capitalize(StringUtils.lowerCase(name));
        }
        return suggestion;
    }

}
