package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "Show how to use Nomer.")
public class CmdHelp implements Runnable {
    private final JCommander jc;

    public CmdHelp(JCommander jc) {
        this.jc = jc;
    }

    @Override
    public void run() {
        StringBuilder out = new StringBuilder();
        jc.usage(out);
        System.out.append(out.toString());
    }
}
