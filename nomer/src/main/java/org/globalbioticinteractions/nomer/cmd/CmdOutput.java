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

public abstract class CmdOutput extends CmdMatcherParams {

    @Parameter(names = {"-o", "--output-format"}, description = "tsv, json", validateWith = JsonTsvFormatValidator.class)
    private String outputFormat = "tsv";

    @Override
    abstract public void run();

    public static RowHandler getRowHandler(CmdOutput ctx) {
        TermMatcher matcher = MatchUtil.getTermMatcher(ctx.getMatchers(), ctx);

        Appender appender;
        if ("json".equalsIgnoreCase(ctx.outputFormat)) {
            appender = new AppenderJSON();
        } else {
            String property = ctx.getProperty("nomer.append.schema.output");
            if (StringUtils.isNotBlank(property)) {
                appender = new AppenderTSV(parseSchema(property));
            } else {
                appender = new AppenderTSV();
            }
        }

        return new AppendingRowHandler(System.out, matcher, ctx, appender);
    }

}
