package org.globalbioticinteractions.nomer.util;

import org.apache.commons.io.IOUtils;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.match.MatchUtil;
import org.globalbioticinteractions.nomer.match.ResourceServiceFactoryImpl;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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
            properties.put("nomer.preston.version", "hash://sha256/bb6dac6461b66212c5b1826447d7765529ff5cbadeac1915f7c3be9748eda991");
            properties.put("nomer.preston.remotes", "https://zenodo.org/record/5639794/files");
            return properties.get(key);
        }

        @Override
        public List<String> getMatchers() {
            return Collections.singletonList("gbif");
        }

    }
}