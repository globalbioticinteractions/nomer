package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameters;
import org.globalbioticinteractions.nomer.util.MatchUtil;

@Parameters(separators = "= ", commandDescription = "Replace")
public class CmdReplace implements Runnable {

    @Override
    public void run() {
        MatchUtil.match(new String[]{}, true);
    }
}
