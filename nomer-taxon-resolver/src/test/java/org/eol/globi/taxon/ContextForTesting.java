package org.eol.globi.taxon;

import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class ContextForTesting implements TermMatcherContext {
    @Override
    public String getCacheDir() {
        return "target";
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
        return null;
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
