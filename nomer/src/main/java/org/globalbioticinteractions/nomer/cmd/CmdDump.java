package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.RowHandler;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.Appender;
import org.globalbioticinteractions.nomer.util.AppenderJSON;
import org.globalbioticinteractions.nomer.util.AppenderTSV;
import org.globalbioticinteractions.nomer.util.AppendingRowHandler;
import org.globalbioticinteractions.nomer.util.MatchUtil;

@Parameters(separators = "= ", commandDescription = "Dumps all terms into the defined output schema.")
public class CmdDump extends CmdOutput {

    private static final String[] MATCH_ALL = {MatchUtil.WILDCARD_MATCH, MatchUtil.WILDCARD_MATCH};

    @Override
    public void run() {
        RowHandler rowHandler = CmdAppend.getRowHandler(this);
        try {
            rowHandler.onRow(MATCH_ALL);
        } catch (PropertyEnricherException e) {
            throw new RuntimeException("failed to dump term list", e);
        }
    }

}
