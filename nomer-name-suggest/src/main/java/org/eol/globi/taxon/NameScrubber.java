package org.eol.globi.taxon;

import org.eol.globi.service.NameSuggester;

public class NameScrubber implements NameSuggester {

    @Override
    public String suggest(final String name) {
        return clean(name);
    }

    private static String clean(String name) {
        name = name.replaceAll("^\\d+$", "");
        name = name.replaceAll("^[\\pL\\pN\\p{Pc}]{1}\\s", "");
        name = name.replaceAll("((<[a-z0-9]+>)|(</[a-z0-9]+>)|(<[a-z0-9]+/>))", "");
        name = name.replaceAll("[^\\p{L}\\p{N}-\\.\\(\\)]", " ");
        name = name.replaceAll("[^\\pL\\pN\\p{Pc}]", " ");
        name = name.replaceAll("(^|\\s+)\\d+", " ");
        return name.trim();
    }

}
