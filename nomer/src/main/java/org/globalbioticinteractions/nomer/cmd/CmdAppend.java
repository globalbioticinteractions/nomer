package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameters;
import org.eol.globi.taxon.RowHandler;
import org.globalbioticinteractions.nomer.match.MatchUtil;

import java.util.List;

@Parameters(separators = "= ", commandDescription = "Append term match to row using id and name columns specified in input schema. Multiple matches result in multiple rows.")
public class CmdAppend extends CmdOutput {

    @Override
    public void run() {
        List<RowHandler> handlers = MatchUtil.getAppendingRowHandlers(
                this,
                getIncludeHeader(),
                getOutputFormat(),
                System.out
        );
        MatchUtil.match(handlers.toArray(new RowHandler[0]));
    }

}
