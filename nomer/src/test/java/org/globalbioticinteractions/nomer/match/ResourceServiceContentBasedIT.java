package org.globalbioticinteractions.nomer.match;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.globalbioticinteractions.nomer.cmd.CmdMatcherParams;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URI;

public class ResourceServiceContentBasedIT {

    @Rule
    public TemporaryFolder cacheDir = new TemporaryFolder();

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();


    @Test
    public void getCatalogueOfLife() throws IOException {
        CmdMatcherParams ctx = new CmdMatcherParams() {

            @Override
            public void run() {

            }

            @Override
            public String getProperty(String key) {
                    return "nomer.cache.dir".equals(key)
                            ? cacheDir.getRoot().getAbsolutePath()
                            : super.getProperty(key);
            }
        };

        ctx.setWorkDir(workDir.getRoot().getAbsolutePath());
        ResourceServiceContentBased resourceServiceContentBased = new ResourceServiceContentBased(ctx);

        IOUtils.copy(
                resourceServiceContentBased.retrieve(URI.create("zip:https://download.catalogueoflife.org/col/latest_coldp.zip!/NameUsage.tsv")),
                NullOutputStream.NULL_OUTPUT_STREAM
        );
    }

}