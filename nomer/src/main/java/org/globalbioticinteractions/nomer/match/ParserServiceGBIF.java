package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.gbif.nameparser.NameParserGBIF;
import org.gbif.nameparser.api.NameParser;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.api.UnparsableNameException;

public class ParserServiceGBIF extends ParserServiceAbstract {

    private final NameParser parser = new NameParserGBIF();

    @Override
    public Taxon parse(Term term, String nameString) throws PropertyEnricherException {
        try {
            ParsedName nameParsed = parser.parse(nameString, Rank.UNRANKED, null);
            Taxon taxonParsed = new TaxonImpl();
            taxonParsed.setName(nameParsed.canonicalNameWithoutAuthorship());
            taxonParsed.setAuthorship(nameParsed.authorshipComplete());
            return taxonParsed;
        } catch (UnparsableNameException e) {
            throw new PropertyEnricherException("failed to parse [" + term.getName() + "]", e);
        }
    }

}
