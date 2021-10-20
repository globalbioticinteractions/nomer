package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameters;
import org.globalbioticinteractions.nomer.match.MatchUtil;

@Parameters(separators = "= ", commandDescription = "Append term match to row using id and name columns specified in input schema. Multiple matches result in multiple rows.")
public class CmdAppend extends CmdOutput {

    @Override
    public void run() {
        MatchUtil.match(getRowHandler(this));
    }

}
