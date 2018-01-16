package org.globalbioticinteractions.nomer.util;

import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface TermMatcherContext {

    String getCacheDir();

    String getProperty(String key);

    InputStream getResource(String uri) throws IOException;

    List<String> getMatchers();

    Pair<Integer, Integer> getSchema();

    boolean shouldReplaceTerms();
}
