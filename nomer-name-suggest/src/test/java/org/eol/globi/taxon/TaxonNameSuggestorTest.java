package org.eol.globi.taxon;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TaxonNameSuggestorTest {

    private final static SuggestionService CORRECTOR = new TaxonNameSuggestor() {{
        setSuggestors(Collections.singletonList(name -> name));
    }};

    @Test
    public void cleanName() {
        assertThat(CORRECTOR.suggest(""), is(""));
        assertThat(CORRECTOR.suggest("a"), is(""));
    }

    @Test
    public void batSARSCoV() {
        assertThat(CORRECTOR.suggest("Bat SARS CoV"), is("Bat SARS CoV"));
    }

    @Test
    public void taxonNameNoMatch() {
        assertThat(CORRECTOR.suggest(PropertyAndValueDictionary.NO_MATCH), is(PropertyAndValueDictionary.NO_MATCH));
    }

    @Test
    public void taxonNameTooShort() {
        assertThat(CORRECTOR.suggest("G"), is(""));
        assertThat(CORRECTOR.suggest("H"), is(""));
        assertThat(CORRECTOR.suggest("HH"), is("HH"));
    }

    @Test
    public void circularSuggestions() {
        TaxonNameSuggestor corrector = new TaxonNameSuggestor() {{
            setSuggestors(Collections.singletonList(
                    name -> "Mimesa bicolor".equals(name) ? "Mimesa equestris" : "Mimesa bicolor")
            );
        }};
        assertThat(corrector.suggest("Mimesa bicolor"), is("Mimesa bicolor"));
    }

}
