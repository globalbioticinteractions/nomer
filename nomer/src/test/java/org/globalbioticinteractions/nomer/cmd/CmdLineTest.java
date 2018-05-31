package org.globalbioticinteractions.nomer.cmd;

import org.junit.Test;

public class CmdLineTest {

    @Test
    public void runNoThrow() throws Throwable {
        CmdLine.run(new String[]{"version"});
    }

}