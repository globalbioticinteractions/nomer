package org.eol.globi.taxon;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TaxonNameCorrectorTest {

    private final static CorrectionService CORRECTOR = new TaxonNameCorrector() {{
        setSuggestors(Collections.singletonList(name -> name));
    }};

    @Test
    public void cleanName() {
        assertThat(CORRECTOR.correct(""), is(""));
        assertThat(CORRECTOR.correct("a"), is(""));
    }

    @Test
    public void taxonNameNoMatch() {
        assertThat(CORRECTOR.correct(PropertyAndValueDictionary.NO_MATCH), is(PropertyAndValueDictionary.NO_MATCH));
    }

    @Test
    public void taxonNameTooShort() {
        assertThat(CORRECTOR.correct("G"), is(""));
        assertThat(CORRECTOR.correct("H"), is(""));
        assertThat(CORRECTOR.correct("HH"), is("HH"));
    }

    @Test
    public void circularSuggestions() {
        TaxonNameCorrector corrector = new TaxonNameCorrector() {{
            setSuggestors(Collections.singletonList(
                    name -> "Mimesa bicolor".equals(name) ? "Mimesa equestris" : "Mimesa bicolor")
            );
        }};
        assertThat(corrector.correct("Mimesa bicolor"), is("Mimesa bicolor"));
    }

}
