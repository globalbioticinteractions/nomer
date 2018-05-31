package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameters;

@Parameters(separators = "= ", commandDescription = "Show Version.")
public class CmdVersion implements Runnable {

    @Override
    public void run() {
        String version = CmdVersion.class.getPackage().getImplementationVersion();
        System.out.println(version == null ? "dev" : version);
    }
}
