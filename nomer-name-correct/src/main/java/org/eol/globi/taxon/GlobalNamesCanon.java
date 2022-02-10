package org.eol.globi.taxon;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.service.NameSuggester;
import org.globalnames.parser.ScientificNameParser;
import scala.Option;

public class GlobalNamesCanon implements NameSuggester {
    private final ScientificNameParser parser = ScientificNameParser.instance();

    @Override
    public String suggest(String name) {
        // names ending with a capital V or containing "virus" are likely virus names
        return StringUtils.endsWith(name, "V") || StringUtils.containsIgnoreCase(name,"virus")
                ? name
                : parse(name);
    }

    private String parse(String name) {
        final Option<String> canonized = parser.fromString(name).canonized(true);
        return canonized.isDefined() ? canonized.get() : name;
    }
}
