package org.globalbioticinteractions.nomer.match;

import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

public abstract class TermMatcherContextClasspath implements TermMatcherContext {
    @Override
    public InputStream retrieve(URI uri) throws IOException {
        return getClass().getResourceAsStream(uri.toString());
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


}
