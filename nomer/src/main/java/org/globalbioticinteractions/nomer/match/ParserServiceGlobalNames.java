package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
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
