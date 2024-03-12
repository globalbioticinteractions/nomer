package org.globalbioticinteractions.nomer.cmd;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;

@CommandLine.Command(name = "clean", description = "Cleans term matcher cache.")
public class CmdClean extends CmdMatcherParams {
    private final static Logger LOG = LoggerFactory.getLogger(CmdClean.class);

    @Override
    public void run() {
        LOG.info("cleaning cache at [" + getCacheDir() + "]...");
        FileUtils.deleteQuietly(new File(getCacheDir()));
        LOG.info("cleaning cache at [" + getCacheDir() + "] done.");
    }
}
