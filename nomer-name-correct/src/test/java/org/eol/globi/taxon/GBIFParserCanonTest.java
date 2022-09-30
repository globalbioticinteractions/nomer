package org.eol.globi.taxon;

import org.eol.globi.service.NameSuggester;
import org.junit.Ignore;
import org.junit.Test;


public class GBIFParserCanonTest extends NameParserTestBase {

    @Override
    public NameSuggester getNameSuggester() {
        return new GBIFParserCanon();
    }

    @Override
    @Test
    @Ignore("parsing timeout causes intermittent failures of parsing test; https://github.com/globalbioticinteractions/nomer/issues/89 https://github.com/gbif/name-parser/issues/12 https://github.com/gbif/name-parser/issues/51")
    public void parseBigString() {
        super.parseBigString();
    }


}
