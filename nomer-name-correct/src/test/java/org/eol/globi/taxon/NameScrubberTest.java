package org.eol.globi.taxon;

import org.eol.globi.service.NameSuggester;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class NameScrubberTest {

    protected NameSuggester getNameSuggester() {
        return new NameScrubber();
    }

    @Test
    public void dropNonLetterSuffix() {
        assertThat(getNameSuggester().suggest("Aegathoa oculata¬†"), is("Aegathoa oculata"));
        assertThat(getNameSuggester().suggest("Aegathoa oculata*"), is("Aegathoa oculata"));
        assertThat(getNameSuggester().suggest("Aegathoa oculata?"), is("Aegathoa oculata"));
    }

    @Test
    public void dropUnderscore() {
        assertThat(getNameSuggester().suggest("Homo_sapiens"), is("Homo sapiens"));
    }

    @Test
    public void dropTag() {
        assertThat(getNameSuggester().suggest("<a>Homo sapiens</a>"), is("Homo sapiens"));
        assertThat(getNameSuggester().suggest("<p>Homo sapiens</a>"), is("Homo sapiens"));
    }

    @Test
    public void digitsOnly() {
        assertThat(getNameSuggester().suggest("123"), is(""));
    }

}
