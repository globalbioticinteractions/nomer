package org.globalbioticinteractions.nomer.cmd;

import org.globalbioticinteractions.nomer.Nomer;
import picocli.CommandLine;

@CommandLine.Command(name = "version", description = "Show Version")
public class CmdVersion implements Runnable {

    @Override
    public void run() {
        String version = Nomer.getVersionString();
        System.out.println(version == null ? "dev" : version);
    }

}
