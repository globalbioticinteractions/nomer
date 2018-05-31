package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.taxon.RowHandler;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.AppendingRowHandler;
import org.globalbioticinteractions.nomer.util.MatchUtil;
import org.globalbioticinteractions.nomer.util.AppendingRowHandlerJson;

@Parameters(separators = "= ", commandDescription = "Append term match to row using id and name columns specified in input schema. Multiple matches result in multiple rows.")
public class CmdAppend extends CmdMatcherParams {

    @Parameter(names = {"-o", "--output-format"}, description = "[tsv, json]", validateWith = JsonTsvFormatValidator.class)
    private String outputFormat = "tsv";

    @Override
    public void run() {
        TermMatcher matcher = MatchUtil.getTermMatcher(getMatchers(), this);
        RowHandler handler;
        if ("json".equalsIgnoreCase(outputFormat)) {
            handler = new AppendingRowHandlerJson(System.out, matcher, this);
        } else {
            handler = new AppendingRowHandler(System.out, matcher, this);
        }
        MatchUtil.match(handler);
    }

}
