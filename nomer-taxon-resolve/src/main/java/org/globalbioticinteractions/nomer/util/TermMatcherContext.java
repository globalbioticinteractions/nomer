package org.globalbioticinteractions.nomer.util;

import java.io.IOException;
import java.io.InputStream;

public interface TermMatcherContext {

    String getCacheDir();

    String getProperty(String key);

    InputStream getResource(String uri) throws IOException;

}
