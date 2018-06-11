package org.eol.globi.taxon;

import org.apache.commons.io.IOUtils;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;

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

    @Test
    public void init() throws IOException {
        String suggested = new ManualSuggester() {{
            init(IOUtils.toInputStream("provided,corrected\nHolo grapiens,Homo sapiens"));
        }}.suggest("Holo grapiens");
        assertThat(suggested, Is.is("Homo sapiens"));
    }
}
