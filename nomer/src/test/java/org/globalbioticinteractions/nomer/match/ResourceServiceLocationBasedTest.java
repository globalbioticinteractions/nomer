package org.globalbioticinteractions.nomer.match;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eol.globi.service.ResourceService;
import org.globalbioticinteractions.nomer.cmd.OutputFormat;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ResourceServiceLocationBasedTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void retrieveLocal() throws IOException, URISyntaxException {
        File cache = folder.newFolder("cache");
        ResourceService localService = new ResourceServiceLocationBased(getCtx(cache));
        InputStream is = localService.retrieve(getClass().getResource("content.txt").toURI());
        assertThat(IOUtils.toString(is, StandardCharsets.UTF_8), is("foo"));
    }

    @Test(expected = IOException.class)
    public void retrieveNonExistingLocal() throws IOException, URISyntaxException {
        File cache = folder.newFolder("cache");
        ResourceService localService = new ResourceServiceLocationBased(getCtx(cache));
        File file = folder.newFile("tobedeleted.txt");
        FileUtils.delete(file);
        localService.retrieve(file.toURI());
    }

    @Test
    public void retrieveLocalGzippedWithoutGZExtension() throws IOException, URISyntaxException {
        File cache = folder.newFolder("cache");
        ResourceService localService = new ResourceServiceLocationBased(getCtx(cache));
        InputStream is = localService.retrieve(getClass().getResource("content.txt.bin").toURI());
        assertThat(IOUtils.toString(new GZIPInputStream(is), StandardCharsets.UTF_8), is("foo"));
    }

    @Test
    public void retrieveLocalGzippedWithGZExtension() throws IOException, URISyntaxException {
        File cache = folder.newFolder("cache");
        ResourceService localService = new ResourceServiceLocationBased(getCtx(cache));
        InputStream is = localService.retrieve(getClass().getResource("content.txt.gz").toURI());
        assertThat(IOUtils.toString(is, StandardCharsets.UTF_8), is("foo"));
    }

    private TermMatcherContext getCtx(File cache) {
        return new TermMatcherContext() {
            @Override
            public String getCacheDir() {
                return cache.getAbsolutePath();
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
            public InputStream retrieve(URI resourceName) throws IOException {
                return null;
            }

            @Override
            public String getProperty(String key) {
                return null;
            }
        };
    }

}