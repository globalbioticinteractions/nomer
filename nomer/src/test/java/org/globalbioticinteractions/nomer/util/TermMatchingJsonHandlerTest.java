package org.globalbioticinteractions.nomer.util;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.PropertyEnricherFactory;
import org.eol.globi.taxon.RowHandler;
import org.eol.globi.taxon.TermMatcher;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class TermMatchingJsonHandlerTest {

    @Test
    public void resolveWithEnricher() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("ITIS:180596\tCanis lupus");
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        final TermMatcher matcher = PropertyEnricherFactory.createTaxonMatcher(null);
        TermMatcherContext ctx = new MatchTestUtil.TermMatcherContextDefault();
        RowHandler rowHandler = new TermMatchingRowJsonHandler(os, matcher, ctx);
        MatchUtil.resolve(is, rowHandler);
        JsonNode jsonNode = new ObjectMapper().readTree(os.toString());
        InputStream resourceAsStream = getClass().getResourceAsStream("wolf.json");
        JsonNode expectedJson = new ObjectMapper().readTree("{\"species\":{\"@id\":\"ITIS:180596\",\"name\":\"Canis lupus\",\"same_as\":{\"@id\":\"ITIS:180596\",\"name\":\"Canis lupus\"}}}");
        assertThat(jsonNode, Is.is(expectedJson));
    }

}
