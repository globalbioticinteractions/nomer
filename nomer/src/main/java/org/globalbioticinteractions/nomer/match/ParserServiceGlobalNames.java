package org.globalbioticinteractions.nomer.match;

import org.apache.tika.utils.StringUtils;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.gbif.nameparser.NameParserGBIF;
import org.gbif.nameparser.api.NameParser;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.api.UnparsableNameException;
import org.globalnames.parser.ScientificNameParser;
import scala.Option;

public class ParserServiceGlobalNames extends ParserServiceAbstract {
    private final ScientificNameParser parser = ScientificNameParser.instance();

    @Override
    public Taxon parse(Term term, String name) throws PropertyEnricherException {
        final Option<String> canonized = parser.fromString(name).canonized(true);
        String canonicalName = canonized.isDefined() ? canonized.get() : name;
        ScientificNameParser.Result result = parser.fromString(name);
        TaxonImpl nameParsed = new TaxonImpl();
        nameParsed.setName(canonicalName);
        nameParsed.setAuthorship(StringUtils.isBlank(result.authorshipDelimited()) ? null : result.authorshipDelimited());
        return nameParsed;
    }

}
