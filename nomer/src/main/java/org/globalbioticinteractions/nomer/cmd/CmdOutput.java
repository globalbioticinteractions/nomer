package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameter;

public abstract class CmdOutput extends CmdMatcherParams {

    public static final OutputFormat OUTPUT_FORMAT_DEFAULT = OutputFormat.tsv;
    @Parameter(names = {"-o", "--output-format"}, description = "tsv, json")
    private OutputFormat outputFormat = OUTPUT_FORMAT_DEFAULT;

    @Parameter(names = {"--include-header", "--with-header"}, description = "include table header")
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
