package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.taxon.RowHandler;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.Appender;
import org.globalbioticinteractions.nomer.util.AppenderJSON;
import org.globalbioticinteractions.nomer.util.AppenderTSV;
import org.globalbioticinteractions.nomer.util.AppendingRowHandler;
import org.globalbioticinteractions.nomer.util.MatchUtil;

@Parameters(separators = "= ", commandDescription = "Append term match to row using id and name columns specified in input schema. Multiple matches result in multiple rows.")
public class CmdAppend extends CmdMatcherParams {

    @Parameter(names = {"-o", "--output-format"}, description = "tsv, json", validateWith = JsonTsvFormatValidator.class)
    private String outputFormat = "tsv";

    @Override
    public void run() {
        TermMatcher matcher = MatchUtil.getTermMatcher(getMatchers(), this);

        Appender appender;
        if ("json".equalsIgnoreCase(outputFormat)) {
            appender = new AppenderJSON();
        } else {
            String property = getProperty("nomer.append.schema.output");
            if (StringUtils.isNotBlank(property)) {
                appender = new AppenderTSV(parseSchema(property));
            } else {
                appender = new AppenderTSV();
            }
        }

        RowHandler handler = new AppendingRowHandler(System.out, matcher, this, appender);

        MatchUtil.match(handler);
    }

}
