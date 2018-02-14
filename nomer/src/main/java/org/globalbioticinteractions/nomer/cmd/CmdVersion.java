package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameters;

@Parameters(separators = "= ", commandDescription = "Show Version")
public class CmdVersion implements Runnable {

    @Override
    public void run() {
        System.out.println(CmdVersion.class.getPackage().getImplementationVersion());
    }
}
