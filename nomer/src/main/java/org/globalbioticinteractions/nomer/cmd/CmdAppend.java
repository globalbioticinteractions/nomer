package org.globalbioticinteractions.nomer.cmd;

import org.eol.globi.taxon.RowHandler;
import org.globalbioticinteractions.nomer.match.MatchUtil;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(
        name = "append",
        description = "Append term match to row from stdin using id and name columns specified in input schema. " +
                "Multiple matches result in multiple rows." +
                "%nExample:%n"+ "echo -e '\\tHomo sapiens' | nomer append col"
)
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
