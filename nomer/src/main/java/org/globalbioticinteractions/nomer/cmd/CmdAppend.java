package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameters;
import org.globalbioticinteractions.nomer.util.MatchUtil;

@Parameters(separators = "= ", commandDescription = "Append term matches to row")
public class CmdAppend extends CmdDefaultParams {

    @Override
    public void run() {
        MatchUtil.match(getMatchers(), this);
    }

}
