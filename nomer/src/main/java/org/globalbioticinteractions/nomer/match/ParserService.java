package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;

public interface ParserService {

    Taxon parse(Term term, String name) throws PropertyEnricherException;
}
