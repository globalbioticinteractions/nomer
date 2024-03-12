package org.globalbioticinteractions.nomer.cmd;

import org.apache.commons.io.FileUtils;
import org.globalbioticinteractions.nomer.match.TermMatcherContextCaching;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;

@CommandLine.Command(name = "clean", description = "Cleans term matcher cache.")
public class CmdClean implements Runnable {
    private final static Logger LOG = LoggerFactory.getLogger(CmdClean.class);

    @Override
    public void run() {
        File cacheDir = TermMatcherContextCaching.getOrCreateDefaultCacheDir();
        LOG.info("cleaning cache at [" + cacheDir.getAbsolutePath() + "]...");
        FileUtils.deleteQuietly(cacheDir);
        LOG.info("cleaning cache at [" + cacheDir.getAbsolutePath() + "] done.");
    }
}
