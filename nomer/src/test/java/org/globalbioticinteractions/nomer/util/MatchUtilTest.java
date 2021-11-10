package org.globalbioticinteractions.nomer.util;

import org.apache.commons.io.IOUtils;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.match.MatchUtil;
import org.globalbioticinteractions.nomer.match.ResourceServiceFactoryImpl;
import org.junit.Test;

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

    @Test
    public void defaultOnEmpty() {
        TermMatcher termMatcher = MatchUtil.getTermMatcher(Collections.emptyList(), new MatchTestUtil.TermMatcherContextDefault());
        assertThat(termMatcher, is(notNullValue()));
    }

    @Test(expected = RuntimeException.class)
    public void defaultWithGBIFLiteMissingResource() throws IOException, PropertyEnricherException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MatchTestUtil.TermMatcherContextDefault ctx = getGBIFContext();
        MatchUtil.apply(IOUtils.toInputStream("\tHomo sapiens", StandardCharsets.UTF_8),
                MatchUtil.getRowHandler(ctx, os));

        assertThat(os.toString(StandardCharsets.UTF_8.name()), is("bla"));
    }

    @Test
    public void defaultWithGBIFLite() throws IOException, PropertyEnricherException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MatchTestUtil.TermMatcherContextDefault ctx = new TermMatcherContextGBIF() {
            @Override
            public InputStream retrieve(URI uri) throws IOException {
                return new ResourceServiceFactoryImpl(this)
                        .createResourceService()
                        .retrieve(uri);
            }

        };
        MatchUtil.apply(IOUtils.toInputStream("\tHomo sapiens", StandardCharsets.UTF_8),
                MatchUtil.getRowHandler(ctx, os));

        assertThat(os.toString(StandardCharsets.UTF_8.name()), is("bla"));
    }

    private MatchTestUtil.TermMatcherContextDefault getGBIFContext() {
        return new TermMatcherContextGBIF();
    }

    private static class TermMatcherContextGBIF extends MatchTestUtil.TermMatcherContextDefault {

        @Override
        public String getProperty(String key) {
            Map<String, String> properties = new TreeMap<>();
            properties.put("nomer.gbif.ids", "gz:https://example.org/ids.gz!/ids");
            properties.put("nomer.gbif.names", "gz:https://example.org/ids.gz!/names");
            properties.put("nomer.preston.version", "hash://sha256/7f607bb8389e3d6ba1f2e9d2c9b5a1c6ad4fd7421cbe8ad858b05721a9dc8273");
            properties.put("nomer.preston.remotes", "file://" + getDataDir());
            return properties.get(key);
        }

        @Override
        public String getCacheDir() {
            return getDataDir();

        }

        private String getDataDir() {
            URL resource = getClass().getResource("/org/globalbioticinteractions/nomer/match/preston/data/2a/5d/2a5de79372318317a382ea9a2cef069780b852b01210ef59e06b640a3539cb5a");

            try {
                File dataDir = new File(resource.toURI())
                        .getParentFile()
                        .getParentFile()
                        .getParentFile();
                String absolutePath = dataDir.getAbsolutePath();
                System.out.println(absolutePath);
                return absolutePath;
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }


        @Override
        public List<String> getMatchers() {
            return Collections.singletonList("gbif");
        }

    }
}