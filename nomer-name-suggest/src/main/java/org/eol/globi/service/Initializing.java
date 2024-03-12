package org.eol.globi.service;

import java.io.IOException;
import java.io.InputStream;

public interface Initializing {
    void init(InputStream is) throws IOException;
}
