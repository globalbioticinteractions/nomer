package org.globalbioticinteractions.nomer.util;

import org.eol.globi.domain.PropertyAndValueDictionary;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TestTermMatcherContextDefault implements TermMatcherContext {

    private final String dataDir;

    public TestTermMatcherContextDefault() {
        this.dataDir = "target/cache-dir";
    }

    TestTermMatcherContextDefault(File dataDir) {
        this.dataDir = dataDir.getAbsolutePath();
    }

    @Override
    public String getCacheDir() {
        return dataDir;
    }


    @Override
    public String getProperty(String key) {
        return null;
    }

    @Override
    public InputStream retrieve(URI uri) throws IOException {
        return null;
    }

    @Override
    public List<String> getMatchers() {
        return null;
    }

    @Override
    public Map<Integer, String> getInputSchema() {
        return new TreeMap<Integer, String>() {{
            put(0, PropertyAndValueDictionary.EXTERNAL_ID);
            put(1, PropertyAndValueDictionary.NAME);
        }};
    }

    @Override
    public Map<Integer, String> getOutputSchema() {
        return null;
    }

    @Override
    public String getOutputFormat() {
        return null;
    }

}
