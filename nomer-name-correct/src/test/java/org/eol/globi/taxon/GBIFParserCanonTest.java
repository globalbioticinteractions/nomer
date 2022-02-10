package org.eol.globi.taxon;

import org.eol.globi.service.NameSuggester;


public class GBIFParserCanonTest extends NameParserTestBase {

    @Override
    public NameSuggester getNameSuggester() {
        return new GBIFParserCanon();
    }


}
