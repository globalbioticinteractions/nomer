package org.globalbioticinteractions.nomer.cmd;

import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.RowHandler;
import org.globalbioticinteractions.nomer.match.MatchUtil;
import org.globalbioticinteractions.nomer.match.TermMatchUtil;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(name = "list",
        aliases = {"ls", "dump", "export"},
        description = "Dumps all terms into the defined output schema."
)
public class CmdDump extends CmdOutput {

    @Override
    public void run() {
        List<RowHandler> handlers = MatchUtil.getAppendingRowHandlers(
                this,
                getIncludeHeader(),
                getOutputFormat(),
                System.out
        );

        try {
            for (RowHandler handler : handlers) {
                handler.onRow(
                        TermMatchUtil.wildcardRowForSchema(getInputSchema())
                );
            }
        } catch (PropertyEnricherException e) {
            throw new RuntimeException("failed to dump term list", e);
        }
    }

}
