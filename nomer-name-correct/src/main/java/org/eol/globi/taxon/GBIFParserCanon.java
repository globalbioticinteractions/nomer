package org.eol.globi.taxon;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.service.NameSuggester;
import org.gbif.nameparser.NameParserGBIF;
import org.gbif.nameparser.api.NameParser;
import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.api.UnparsableNameException;
import org.globalnames.parser.ScientificNameParser;
import scala.Option;

public class GBIFParserCanon implements NameSuggester {
    private final NameParser parser = new NameParserGBIF();

    @Override
    public String suggest(String name) {
        // names ending with a capital V or containing "virus" are likely virus names
        return StringUtils.endsWith(name, "V")
                || StringUtils.containsIgnoreCase(name,"virus")
                ? name
                : parse(name);
    }

    private String parse(String name) {
        try {
            return parser.parse(name, Rank.UNRANKED, null).canonicalNameWithoutAuthorship();
        } catch (UnparsableNameException e) {
            return name;
        }
    }
}
