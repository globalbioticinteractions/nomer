package org.globalbioticinteractions.nomer.match;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.globalbioticinteractions.nomer.cmd.CmdMatcherParams;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

import static org.hamcrest.MatcherAssert.assertThat;

public class ResourceServiceContentBasedTest {

    @Rule
    public TemporaryFolder cacheDir = new TemporaryFolder();

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();


    @Test
    public void getMostUpToDateAlias() throws IOException {

        // with-update contains two versions of file:///tmp/preston-test/foo.txt
        // , the most recent one containing "bar2", the less recent one contains "bar"
        ZipFile zipFile = new ZipFile(getClass().getResource("with-update.zip").getFile());

        Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();

        while (entries.hasMoreElements()) {
            ZipArchiveEntry zipArchiveEntry = entries.nextElement();
            File target = new File(cacheDir.getRoot(), zipArchiveEntry.getName());
            if (zipArchiveEntry.isDirectory()) {
                FileUtils.forceMkdir(target);
            } else {
                IOUtils.copy(zipFile.getInputStream(zipArchiveEntry), new FileOutputStream(target));
            }
        }

        CmdMatcherParams ctx = new CmdMatcherParams() {

            @Override
            public void run() {

            }

            @Override
            public String getProperty(String key) {
                String value = null;
                if ("nomer.cache.dir".equals(key)) {
                    value = cacheDir.getRoot().getAbsolutePath();
                } else if ("nomer.preston.dir".equals(key)) {
                    value = cacheDir.getRoot().getAbsolutePath() + "/data";
                } else if ("nomer.preston.version".equals(key)) {
                    value = "hash://sha256/30845fefa4a854fc67da113a06759f86902b591bf0708bd625e611680aa1c9c4";
                } else if ("nomer.preston.remotes".equals(key)) {
                    value = "";
                } else {
                    value = super.getProperty(key);
                }
                return value;
            }
        };

        ctx.setWorkDir(workDir.getRoot().getAbsolutePath());
        ResourceServiceContentBased resourceServiceContentBased = new ResourceServiceContentBased(ctx);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        IOUtils.copy(
                resourceServiceContentBased.retrieve(URI.create("file:///tmp/preston-test/foo.txt")),
                os
        );

        assertThat(new String(os.toByteArray(), StandardCharsets.UTF_8), Is.is("bar2\n"));
    }

}