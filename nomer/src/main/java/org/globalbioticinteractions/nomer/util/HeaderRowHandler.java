package org.globalbioticinteractions.nomer.util;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.Capitalizer;
import org.eol.globi.taxon.RowHandler;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class HeaderRowHandler implements RowHandler {
    private final PrintStream out;
    private final Map<Integer, String> inputSchema;
    private final Map<Integer, String> outputSchema;
    private static final String SEPARATOR = "\t";
    private final AtomicBoolean isFirstLine = new AtomicBoolean(true);

    public HeaderRowHandler(OutputStream os,
                            Map<Integer, String> inputSchema,
                            Map<Integer, String> outputSchema) {
        this.out = new PrintStream(os);
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
    }

    @Override
    public void onRow(final String[] rowOrig) throws PropertyEnricherException {
        if (isFirstLine.get()) {
            printHeader(rowOrig);
        }
        isFirstLine.set(false);
    }

    public void printHeader(String[] rowOrig) {
        int totalWidth = rowOrig.length + 2 + Collections.max(outputSchema.keySet());

        for (int i = 0; i < totalWidth; i++) {
            String name;
            if (i == rowOrig.length) {
                name = "relationName";
            } else {
                if (i < rowOrig.length) {
                    name = "provided" + Capitalizer.capitalize(inputSchema.getOrDefault(i, "col" + i));
                } else {
                    name = "resolved" + Capitalizer.capitalize(outputSchema.getOrDefault(i - (rowOrig.length + 1), "col" + i));
                }
            }
            if (i > 0) {
                out.print(SEPARATOR);
            }
            out.print(name);
        }
        out.print("\n");
    }

}
