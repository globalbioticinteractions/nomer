package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameters;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.RowHandler;
import org.globalbioticinteractions.nomer.match.MatchUtil;
import org.globalbioticinteractions.nomer.match.TermMatchUtil;

@Parameters(separators = "= ", commandDescription = "Dumps all terms into the defined output schema.")
public class CmdDump extends CmdOutput {

    @Override
    public void run() {
        RowHandler rowHandler = MatchUtil.getRowHandler(this, System.out);
        try {
            rowHandler.onRow(
                    TermMatchUtil.wildcardRowForSchema(getInputSchema())
            );
        } catch (PropertyEnricherException e) {
            throw new RuntimeException("failed to dump term list", e);
        }
    }

}
