package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Capitalizer {

    static final private Pattern pattern = Pattern.compile("(?<first>[a-z]{2,})(?<remainder>.*)");

    public static String capitalize(String name) {
        String suggestion = name;
        Matcher matcher = pattern.matcher(name);
        if (matcher.matches()) {
            suggestion = StringUtils.capitalize(matcher.group("first")) +
                    matcher.group("remainder");
        }
        return suggestion;
    }

}
