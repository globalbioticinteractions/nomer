package org.globalbioticinteractions.nomer;

/*
    Nomer - a GloBI tool to help map identifiers and name to identifiers and names
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.Version;
import org.globalbioticinteractions.nomer.cmd.CmdLine;

import static java.lang.System.exit;

public class Nomer {
    private static final Log LOG = LogFactory.getLog(Nomer.class);

    public static void main(String[] args) {
        try {
            LOG.info(Version.getVersionInfo(Nomer.class));
            CmdLine.run(args);
            exit(0);
        } catch (Throwable t) {
            exit(1);
        }
    }
}
