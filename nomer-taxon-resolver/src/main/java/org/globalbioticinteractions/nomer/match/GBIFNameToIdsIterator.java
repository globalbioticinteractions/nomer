package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.eol.globi.util.CSVTSVUtil;
import org.mapdb.Fun;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class GBIFNameToIdsIterator implements Iterator<Fun.Tuple2<String, List<Long>>> {
    private final BufferedReader reader;
    private Fun.Tuple2<String, List<Long>> currentEntry;
    private String nextEntryName = null;
    private List<Long> nextEntryIds = new ArrayList<>();
    private String line = null;

    public GBIFNameToIdsIterator(BufferedReader reader) {
        this.reader = reader;
    }

    @Override
    public boolean hasNext() {


        if (currentEntry == null) {
            try {
                parseNext();
            } catch (IOException e) {
                return false;
            }
        }

        return currentEntry != null || processingLastLine(line);
    }

    private void parseNext() throws IOException {
        while (currentEntry == null && (line = reader.readLine()) != null) {
            String[] values = CSVTSVUtil.splitTSV(line);
            if (values.length > 1) {
                String idString = values[1];
                if (NumberUtils.isDigits(idString)) {
                    String currentName = values[0];
                    if (!StringUtils.equals(nextEntryName, currentName)) {
                        if (StringUtils.isNotBlank(nextEntryName)) {
                            currentEntry = new Fun.Tuple2<>(nextEntryName, nextEntryIds);
                            nextEntryIds = new ArrayList<>();
                        }
                    }
                    nextEntryIds.add(Long.parseLong(idString));
                    nextEntryName = currentName;
                }
            }

        }
    }

    private boolean processingLastLine(String line) {
        return StringUtils.isBlank(line)
                && StringUtils.isNotBlank(nextEntryName)
                && nextEntryIds.size() > 0;
    }

    @Override
    public Fun.Tuple2<String, List<Long>> next() {
        Fun.Tuple2<String, List<Long>> entry = this.currentEntry;
        if (currentEntry == null && processingLastLine(line)) {
            entry = new Fun.Tuple2<>(nextEntryName, nextEntryIds);
            nextEntryName = null;
            nextEntryIds = Collections.emptyList();
        }
        this.currentEntry = null;

        return entry;
    }
}
