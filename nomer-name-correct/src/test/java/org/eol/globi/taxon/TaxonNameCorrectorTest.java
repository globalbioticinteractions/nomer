package org.eol.globi.taxon;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.NameSuggester;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TaxonNameCorrectorTest {

    private final static CorrectionService CORRECTOR = new TaxonNameCorrector() {{
        setSuggestors(Collections.singletonList((NameSuggester) name -> name));
    }};

    @Test
    public void cleanName() {
        assertThat(CORRECTOR.correct(""), is("no name"));
        assertThat(CORRECTOR.correct("a"), is("no name"));
    }

    @Test
    public void taxonNameNoMatch() {
        assertThat(CORRECTOR.correct(PropertyAndValueDictionary.NO_MATCH), is(PropertyAndValueDictionary.NO_MATCH));
    }

    @Test
    public void taxonNameTooShort() {
        assertThat(CORRECTOR.correct("G"), is("no name"));
        assertThat(CORRECTOR.correct("H"), is("no name"));
        assertThat(CORRECTOR.correct("HH"), is("HH"));
    }
    
    @Test
    public void circularSuggestions() {
        TaxonNameCorrector corrector = new TaxonNameCorrector() {{
            setSuggestors(Arrays.asList(new NameSuggester() {
                @Override
                public String suggest(String name) {
                    return "Mimesa bicolor".equals(name) ? "Mimesa equestris" : "Mimesa bicolor";
                }
            }));
        }};
        assertThat(corrector.correct("Mimesa bicolor"), is("Mimesa bicolor"));
    }

}
