package org.globalbioticinteractions.nomer.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.match.MatchUtil;
import org.globalbioticinteractions.nomer.match.ResourceServiceFactoryImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;

public class MatchUtilTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private File tmpDataDir;

    @Before
    public void init() throws IOException {
        tmpDataDir = folder.newFolder();
        FileUtils.copyDirectory(getDataDir(), new File(tmpDataDir, "data"));
    }

    @Test
    public void defaultOnEmpty() {
        TermMatcher termMatcher = MatchUtil.getTermMatcher(Collections.emptyList(), new TestTermMatcherContextDefault());
        assertThat(termMatcher, is(notNullValue()));
    }

    @Test(expected = RuntimeException.class)
    public void defaultWithGBIFLiteMissingResource() throws IOException, PropertyEnricherException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        TestTermMatcherContextDefault ctx = getGBIFContext();
        MatchUtil.apply(IOUtils.toInputStream("\tHomo sapiens", StandardCharsets.UTF_8),
                MatchUtil.getAppendingRowHandler(ctx, os));

        assertThat(os.toString(StandardCharsets.UTF_8.name()), is("bla"));
    }

    @Test
    public void defaultWithGBIFLite() throws IOException, PropertyEnricherException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        TestTermMatcherContextDefault ctx = new TermMatcherContextGBIF(getTmpDataDir()) {
            @Override
            public InputStream retrieve(URI uri) throws IOException {
                return new ResourceServiceFactoryImpl(this)
                        .createResourceService()
                        .retrieve(uri);
            }

        };
        MatchUtil.apply(IOUtils.toInputStream("\tHomo sapiens", StandardCharsets.UTF_8),
                MatchUtil.getAppendingRowHandler(ctx, os));

        assertThat(os.toString(StandardCharsets.UTF_8.name()), is("\tHomo sapiens\tNONE\t\tHomo sapiens\t\t\t\t\t\t\t\n"));
    }



    private TestTermMatcherContextDefault getGBIFContext() {
        return new TermMatcherContextGBIF(getTmpDataDir());
    }

    public class TermMatcherContextGBIF extends TestTermMatcherContextDefault {

        TermMatcherContextGBIF(File dataDir) {
            super(dataDir);
        }

        @Override
        public String getProperty(String key) {
            Map<String, String> properties = new TreeMap<>();
            properties.put("nomer.gbif.ids", "gz:https://example.org/ids.gz!/ids");
            properties.put("nomer.gbif.names", "gz:https://example.org/names.gz!/names");
            properties.put("nomer.append.schema.output", "[{\"column\":0,\"type\":\"externalId\"},{\"column\": 1,\"type\":\"name\"},{\"column\": 2,\"type\":\"rank\"},{\"column\": 3,\"type\":\"commonNames\"},{\"column\": 4,\"type\":\"path\"},{\"column\": 5,\"type\":\"pathIds\"},{\"column\": 6,\"type\":\"pathNames\"},{\"column\": 7,\"type\":\"externalUrl\"},{\"column\": 8,\"type\":\"thumbnailUrl\"}]\n");
            properties.put("nomer.preston.version", "hash://sha256/7f607bb8389e3d6ba1f2e9d2c9b5a1c6ad4fd7421cbe8ad858b05721a9dc8273");
            properties.put("nomer.preston.remotes", "file://" + getCacheDir());
            return properties.get(key);
        }

        @Override
        public List<String> getMatchers() {
            return Collections.singletonList("gbif");
        }

    }

    private File getTmpDataDir() {
        return MatchUtilTest.this.tmpDataDir;
    }


    private File getDataDir() {
        URL resource = getClass().getResource("/org/globalbioticinteractions/nomer/match/preston/data/2a/5d/2a5de79372318317a382ea9a2cef069780b852b01210ef59e06b640a3539cb5a");

        try {
            File dataDir = new File(resource.toURI())
                    .getParentFile()
                    .getParentFile()
                    .getParentFile();
            return dataDir;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

}