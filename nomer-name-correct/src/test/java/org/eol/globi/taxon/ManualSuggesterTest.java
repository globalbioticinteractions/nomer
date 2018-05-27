package org.eol.globi.taxon;

import org.hamcrest.core.Is;
import org.junit.Test;

import static org.junit.Assert.assertThat;

public class ManualSuggesterTest {

    @Test
    public void throwOnDuplicates() {
        new ManualSuggester().suggest("bla");
    }

    @Test
    public void match() {
        String copepods = new ManualSuggester().suggest("copepods");
        assertThat(copepods, Is.is("Copepoda"));
    }
}
