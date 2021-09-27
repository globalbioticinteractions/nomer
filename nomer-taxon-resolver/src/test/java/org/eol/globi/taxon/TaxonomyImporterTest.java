package org.eol.globi.taxon;

import org.apache.http.client.cache.Resource;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TaxonomyImporterTest {

    @Test
    public void stringFormat() {
        TaxonomyImporter taxonomyImporter = new TaxonomyImporter(new TaxonParser() {
            @Override
            public void parse(InputStream is, TaxonImportListener listener) throws IOException {

            }
        }, new TaxonReaderFactory() {
            @Override
            public Map<String, Resource> getResources() throws IOException {
                return null;
            }
        });
        taxonomyImporter.setCounter(123);
        String s = taxonomyImporter.formatProgressString(12.2);

        assertThat(s, is("123 12.2 terms/s"));

        taxonomyImporter.setCounter(798595);
        s = taxonomyImporter.formatProgressString(12.2);
        assertThat(s, is("798595 12.2 terms/s"));
    }

}
