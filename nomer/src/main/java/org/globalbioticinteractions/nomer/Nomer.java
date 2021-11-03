package org.globalbioticinteractions.nomer;

/*
    Nomer - maps identifiers and names to other identifiers and names
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
