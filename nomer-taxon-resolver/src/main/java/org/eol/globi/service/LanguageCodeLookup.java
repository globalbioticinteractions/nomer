package org.eol.globi.service;

// see http://www-01.sil.org/iso639-3/iso-639-3.tab

import org.eol.globi.util.CSVTSVUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;

public class LanguageCodeLookup {

    private static final Map<String, String> iso_639_3_to_639_1 = new TreeMap<>();

    public LanguageCodeLookup() {
        InputStream resourceAsStream = getClass().getResourceAsStream("language-codes.tab");
        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] row = CSVTSVUtil.splitTSV(line);
                if (row.length > 6) {
                    iso_639_3_to_639_1.put(row[0], row[3]);
                    iso_639_3_to_639_1.put(row[6], row[3]);
                }
            }
        } catch (IOException e) {
            // ignore
        }
    }


    public String lookupLanguageCodeFor(String code) {
        return iso_639_3_to_639_1.get(code);
    }

}
