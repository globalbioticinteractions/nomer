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
import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.StringStartsWith.startsWith;

public class ResourceServiceContentBasedTest {

    @Rule
    public TemporaryFolder cacheDir = new TemporaryFolder();

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();


    @Test
    public void getMostUpToDateAlias() throws IOException {

        // with-update contains two versions of file:///tmp/preston-test/foo.txt
        // , the most recent one containing "bar2", the less recent one contains "`bar"
        unpack("with-update.zip");

        String anchor = "hash://sha256/30845fefa4a854fc67da113a06759f86902b591bf0708bd625e611680aa1c9c4";
        CmdMatcherParams ctx = ctxWithAnchor(anchor);
        ResourceServiceContentBased resourceServiceContentBased = new ResourceServiceContentBased(ctx);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        IOUtils.copy(
                resourceServiceContentBased.retrieve(URI.create("file:///tmp/preston-test/foo.txt")),
                os
        );

        assertThat(new String(os.toByteArray(), StandardCharsets.UTF_8), Is.is("bar2\n"));
    }

    private CmdMatcherParams ctxWithAnchor(String anchor) {
        CmdMatcherParams ctx = new CmdMatcherParams() {

            @Override
            public void run() {

            }

            @Override
            public String getProperty(String key) {
                String value;
                if ("nomer.cache.dir".equals(key)) {
                    value = cacheDir.getRoot().getAbsolutePath();
                } else if ("nomer.preston.dir".equals(key)) {
                    value = cacheDir.getRoot().getAbsolutePath() + "/data";
                } else if ("nomer.preston.version".equals(key)) {
                    value = anchor;
                } else if ("nomer.preston.remotes".equals(key)) {
                    value = "";
                } else {
                    value = super.getProperty(key);
                }
                return value;
            }
        };

        ctx.setWorkDir(workDir.getRoot().getAbsolutePath());
        return ctx;
    }

    private void unpack(String filename) throws IOException {
        ZipFile zipFile = new ZipFile(getClass().getResource(filename).getFile());

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
    }


    @Test
    public void getAliasFromPreviousFromAnchoredVersion() throws IOException {
        assertExampleDotOrg("https://example.org/");
    }

    @Test
    public void getAliasFromAnchoredVersion() throws IOException {
        assertExampleDotOrg("https://duckduckgo.com");
    }

    private void assertExampleDotOrg(String alias) throws IOException {
        unpack("get-alias-test.zip");
        CmdMatcherParams ctx = ctxWithAnchor("hash://sha256/18e35af484b642526be2ad322bf9aa2720f3ed17c4e164d5a22e6f2a42ca3033");

        ResourceServiceContentBased resourceServiceContentBased = new ResourceServiceContentBased(ctx);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        IOUtils.copy(
                resourceServiceContentBased.retrieve(URI.create(alias)),
                os
        );

        assertThat(new String(os.toByteArray(), StandardCharsets.UTF_8), anyOf(startsWith("<!DOCTYPE html"), startsWith("<!doctype html")));
    }

}