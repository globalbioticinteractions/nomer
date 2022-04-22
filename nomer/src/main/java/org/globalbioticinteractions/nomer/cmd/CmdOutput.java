package org.globalbioticinteractions.nomer.cmd;

import picocli.CommandLine;

public abstract class CmdOutput extends CmdMatcherParams {

    public static final OutputFormat OUTPUT_FORMAT_DEFAULT = OutputFormat.tsv;

    @CommandLine.Option(
            names = {"-o", "--output-format"},
            description = "tsv, json"
    )
    private OutputFormat outputFormat = OUTPUT_FORMAT_DEFAULT;

    @CommandLine.Option(
            names = {"--include-header", "--with-header"},
            description = "include table header"
    )

    private Boolean includeHeader = false;

    @Override
    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    @Override
    abstract public void run();

    public Boolean getIncludeHeader() {
        return includeHeader;
    }
}
