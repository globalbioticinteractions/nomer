package org.eol.globi.taxon;

import org.eol.globi.service.NameSuggester;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
        assertThat(getNameSuggester().suggest("<h1>Homo sapiens</h1>"), is("Homo sapiens"));
    }

    @Test
    public void digitsOnly() {
        assertThat(getNameSuggester().suggest("123"), is(""));
    }

    @Test
    public void hyphens() {
        assertThat(getNameSuggester().suggest("-- four"), is("four"));
    }

    @Test
    public void parenthesis() {
        assertThat(getNameSuggester().suggest("COLEOPTERA () ()"), is("COLEOPTERA"));
    }

    @Test
    public void moreHyphen() {
        assertThat(getNameSuggester().suggest("Amphipoda-"), is("Amphipoda"));
    }

    @Test
    public void sigma() {
        assertThat(getNameSuggester().suggest("Σ Cephalopoda"), is("Cephalopoda"));
    }

    @Test
    public void singlePrefix() {
        assertThat(getNameSuggester().suggest("A Cephalopoda"), is("Cephalopoda"));
    }

    @Test
    public void singleNumbers() {
        assertThat(getNameSuggester().suggest("ostracod 1 0 1 4"), is("ostracod"));
    }

    @Test
    public void singleNumbers2() {
        assertThat(getNameSuggester().suggest("234 ostracod 1 0 1 4"), is("ostracod"));
    }

    @Test
    public void singleNumbers3() {
        assertThat(getNameSuggester().suggest("234 ostrac234od 1 0 1 4"), is("ostrac234od"));
    }

}
