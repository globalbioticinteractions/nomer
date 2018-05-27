package org.eol.globi.taxon;

import org.eol.globi.service.NameSuggester;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class RemoveStopWordServiceTest {

    @Test
    public void removeStopword() throws IOException {
        NameSuggester stopWordRemover = new RemoveStopWordService();

        String suggest = stopWordRemover.suggest("unidentified object");
        assertThat(suggest, is("object"));
    }

    @Test
    public void removeStopwordCasing() throws IOException {
        NameSuggester stopWordRemover = new RemoveStopWordService();

        String suggest = stopWordRemover.suggest("Unidentified object");
        assertThat(suggest, is("object"));
    }

}