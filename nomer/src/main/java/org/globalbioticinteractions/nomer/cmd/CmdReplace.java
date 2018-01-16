package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameters;
import org.globalbioticinteractions.nomer.util.MatchUtil;

@Parameters(separators = "= ", commandDescription = "Replace")
public class CmdReplace extends CmdDefaultParams {

    @Override
    public void run() {
        MatchUtil.match(getMatchers(), this);
    }

    @Override
    public boolean shouldReplaceTerms() {
        return true;
    }
}
