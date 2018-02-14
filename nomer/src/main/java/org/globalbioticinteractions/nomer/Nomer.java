package org.globalbioticinteractions.nomer;

/*
    Nomer - a GloBI tool to help map identifiers and name to identifiers and names
 */

import org.globalbioticinteractions.nomer.cmd.CmdLine;

import static java.lang.System.exit;

public class Nomer {

    public static void main(String[] args) {
        try {
            CmdLine.run(args);
            exit(0);
        } catch (Throwable t) {
            exit(1);
        }
    }
}
