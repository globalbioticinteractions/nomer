package org.eol.globi.taxon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

public interface TaxonParser {
    void parse(InputStream is, TaxonImportListener listener) throws IOException;
}
