package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameter;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.taxon.RowHandler;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.Appender;
import org.globalbioticinteractions.nomer.util.AppenderJSON;
import org.globalbioticinteractions.nomer.util.AppenderTSV;
import org.globalbioticinteractions.nomer.util.AppendingRowHandler;
import org.globalbioticinteractions.nomer.match.MatchUtil;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public abstract class CmdOutput extends CmdMatcherParams {

    public static final String OUTPUT_FORMAT_DEFAULT = "tsv";

    @Parameter(names = {"-o", "--output-format"}, description = "tsv, json", validateWith = JsonTsvFormatValidator.class)
    private String outputFormat = OUTPUT_FORMAT_DEFAULT;

    @Parameter(names = {"--include-header", "--with-header"}, description = "include table header")
    private Boolean includeHeader = false;

    @Override
    public String getOutputFormat() {
        return outputFormat;
    }

    @Override
    abstract public void run();

    public Boolean getIncludeHeader() {
        return includeHeader;
    }
}
