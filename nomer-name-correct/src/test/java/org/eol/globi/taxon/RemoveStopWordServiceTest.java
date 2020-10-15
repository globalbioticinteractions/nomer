package org.eol.globi.taxon;

import org.apache.commons.io.IOUtils;
import org.eol.globi.service.NameSuggester;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class RemoveStopWordServiceTest {

    private RemoveStopWordService removeStopWordService;

    @Before
    public void initService() throws IOException {
        removeStopWordService = new RemoveStopWordService();
        removeStopWordService.init(getClass().getResourceAsStream("non-taxa-words.tsv"));
    }


    @Test
    public void removeStopword() throws IOException {
        String suggest = removeStopWordService.suggest("one two object");
        assertThat(suggest, is("object"));
    }

    @Test
    public void removeStopword3() throws IOException {
        String suggest = removeStopWordService.suggest("COLEOPTERA (one) (test)");
        assertThat(suggest, is("COLEOPTERA () ()"));
    }

    @Test
    public void removeStopword4() throws IOException {
        String suggest = removeStopWordService.suggest("Amphipoda-test");
        assertThat(suggest, is("Amphipoda-"));
    }

    @Test
    public void removeStopword6() throws IOException {
        String suggest = removeStopWordService.suggest("Sycon (Scypha) raphanus");
        assertThat(suggest, is("Sycon (Scypha) raphanus"));
    }

    @Test
    public void removeStopword5() throws IOException {
        String suggest = removeStopWordService.suggest("Amphipoda-nostopwordhere");
        assertThat(suggest, is("Amphipoda-nostopwordhere"));
    }

    @Test
    public void removeStopWordCasing() throws IOException {
        String suggest = removeStopWordService.suggest("One object");
        assertThat(suggest, is("object"));
    }

    @Test
    public void init() throws IOException {
        NameSuggester stopWordRemover = new RemoveStopWordService() {{
            init(IOUtils.toInputStream("one\ntwo\nthree", StandardCharsets.UTF_8));
        }};

        String suggest = stopWordRemover.suggest("one two three four");
        assertThat(suggest, is("four"));
    }

    @Test
    public void delimiters() throws IOException {
        NameSuggester stopWordRemover = new RemoveStopWordService() {{
            init(IOUtils.toInputStream("one\ntwo\nthree", StandardCharsets.UTF_8));
        }};

        String suggest = stopWordRemover.suggest("one-two-three four");
        assertThat(suggest, is("-- four"));
    }

}