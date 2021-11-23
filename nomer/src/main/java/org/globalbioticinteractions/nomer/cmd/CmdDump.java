package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameters;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.RowHandler;
import org.globalbioticinteractions.nomer.match.MatchUtil;
import org.globalbioticinteractions.nomer.match.TermMatchUtil;

import java.util.List;

@Parameters(separators = "= ", commandDescription = "Dumps all terms into the defined output schema.")
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
