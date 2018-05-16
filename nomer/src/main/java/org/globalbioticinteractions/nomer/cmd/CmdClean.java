package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

@Parameters(separators = "= ", commandDescription = "Cleans term matcher cache.")
public class CmdClean extends CmdMatcherParams {
    private final static Log LOG = LogFactory.getLog(CmdClean.class);

    @Override
    public void run() {
        LOG.info("cleaning cache at [" + getCacheDir() + "]...");
        FileUtils.deleteQuietly(new File(getCacheDir()));
        LOG.info("cleaning cache at [" + getCacheDir() + "] done.");
    }
}
