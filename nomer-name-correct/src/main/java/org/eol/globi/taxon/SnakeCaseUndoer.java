package org.eol.globi.taxon;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.service.NameSuggester;

public class SnakeCaseUndoer implements NameSuggester {

    @Override
    public String suggest(final String name) {
        String suggestion = name;
        if (name.matches("(([a-z])+_*)+")) {
            suggestion = StringUtils.capitalize(StringUtils.replace(name, "_", " "));
        }
        return suggestion;
    }

}
