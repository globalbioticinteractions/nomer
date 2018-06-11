package org.eol.globi.taxon;

import org.apache.commons.io.IOUtils;
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
    public void removeStopword2() throws IOException {
        NameSuggester stopWordRemover = new RemoveStopWordService();

        String suggest = stopWordRemover.suggest("Barnacle larvae");
        assertThat(suggest, is("Barnacle"));
    }

    @Test
    public void removeStopword3() throws IOException {
        NameSuggester stopWordRemover = new RemoveStopWordService();

        String suggest = stopWordRemover.suggest("COLEOPTERA (unidentified) (FRGAMENT)");
        assertThat(suggest, is("COLEOPTERA () ()"));
    }

    @Test
    public void removeStopword4() throws IOException {
        String suggest = new RemoveStopWordService().suggest("Amphipoda-unidentified");
        assertThat(suggest, is("Amphipoda-"));
    }

    @Test
    public void removeStopword6() throws IOException {
        String suggest = new RemoveStopWordService().suggest("Sycon (Scypha) raphanus");
        assertThat(suggest, is("Sycon (Scypha) raphanus"));
    }

    @Test
    public void removeStopword5() throws IOException {
        String suggest = new RemoveStopWordService().suggest("Amphipoda-nostopwordhere");
        assertThat(suggest, is("Amphipoda-nostopwordhere"));
    }

    @Test
    public void removeStopWordCasing() throws IOException {
        NameSuggester stopWordRemover = new RemoveStopWordService();

        String suggest = stopWordRemover.suggest("Unidentified object");
        assertThat(suggest, is("object"));
    }

    @Test
    public void init() throws IOException {
        NameSuggester stopWordRemover = new RemoveStopWordService() {{
            init(IOUtils.toInputStream("one\ntwo\nthree"));
        }};

        String suggest = stopWordRemover.suggest("one two three four");
        assertThat(suggest, is("four"));
    }

    @Test
    public void delimiters() throws IOException {
        NameSuggester stopWordRemover = new RemoveStopWordService() {{
            init(IOUtils.toInputStream("one\ntwo\nthree"));
        }};

        String suggest = stopWordRemover.suggest("one-two-three four");
        assertThat(suggest, is("-- four"));
    }

}