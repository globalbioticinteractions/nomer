package org.globalbioticinteractions.nomer.match;

import org.globalbioticinteractions.nomer.cmd.OutputFormat;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.MatcherAssert.assertThat;

public class ResourceServiceUtilTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File cacheDir;

    @Before
    public void init() throws IOException {
        cacheDir = folder.newFolder();
    }

    @Test
    public void createCachedFileInUnknownContentUniverse() throws IOException {
        File cachedFileName = ResourceServiceUtil.getCachedFileName(
                new MyTermMatcherContext(),
                URI.create("https://example.org/foo.txt.gz")
        );

        assertThat(cachedFileName.exists(), Is.is(false));
        assertThat(
                cachedFileName.getAbsolutePath(),
                endsWith("f0607a4e7cee76dd4be14b7f1f1fa46a1ac4c2426db2387ab0c8b2d99644ab46.gz")
        );
    }

    @Test
    public void createCachedFileInKnownPrestonNamespace() throws IOException {
        File cachedFileName = ResourceServiceUtil.getCachedFileName(
                new MyTermMatcherContext(),
                URI.create("https://example.org/foo.txt.gz")
        );

        assertThat(cachedFileName.exists(), Is.is(false));
        assertThat(
                cachedFileName.getAbsolutePath(),
                endsWith("hash/sha256/7f607bb8389e3d6ba1f2e9d2c9b5a1c6ad4fd7421cbe8ad858b05721a9dc8273/f0607a4e7cee76dd4be14b7f1f1fa46a1ac4c2426db2387ab0c8b2d99644ab46.gz")
        );
    }

    private class MyTermMatcherContext implements TermMatcherContext {
        @Override
        public String getCacheDir() {
            return cacheDir.getAbsolutePath();
        }

        @Override
        public List<String> getMatchers() {
            return null;
        }

        @Override
        public Map<Integer, String> getInputSchema() {
            return null;
        }

        @Override
        public Map<Integer, String> getOutputSchema() {
            return null;
        }

        @Override
        public OutputFormat getOutputFormat() {
            return null;
        }

        @Override
        public InputStream retrieve(URI uri) throws IOException {
            return null;
        }

        @Override
        public String getProperty(String key) {
            Map<String, String> properties = new TreeMap<>();
            properties.put(ResourceServiceUtil.NOMER_PRESTON_VERSION, "hash://sha256/7f607bb8389e3d6ba1f2e9d2c9b5a1c6ad4fd7421cbe8ad858b05721a9dc8273");
            return properties.get(key);
        }
    }
}