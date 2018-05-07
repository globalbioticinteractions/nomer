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

public class TermMatchingRowJsonHandlerTest {

    @Test
    public void resolveWithEnricher() throws IOException, PropertyEnricherException {
        String inputString = "ITIS:180596\tCanis lupus";
        String expectedOutput = "{\"species\":{\"@id\":\"ITIS:180596\",\"name\":\"Canis lupus\",\"equivalent_to\":{\"@id\":\"ITIS:180596\",\"name\":\"Canis lupus\"}}}";
        resolveAndAssert(inputString, expectedOutput);
    }

    private void resolveAndAssert(String inputString, String expectedOutput) throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream(inputString);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        final TermMatcher matcher = PropertyEnricherFactory.createTaxonMatcher(null);
        TermMatcherContext ctx = new MatchTestUtil.TermMatcherContextDefault();
        RowHandler rowHandler = new TermMatchingRowJsonHandler(os, matcher, ctx);
        MatchUtil.resolve(is, rowHandler);
        JsonNode jsonNode = new ObjectMapper().readTree(os.toString());
        JsonNode expectedJson = new ObjectMapper().readTree(expectedOutput);
        assertThat(jsonNode, Is.is(expectedJson));
    }

    @Test
    public void resolveNCBIWithEnricher() throws IOException, PropertyEnricherException {
        String inputString = "NCBI:9612\tCanis lupus";
        String expectedOutput = "{\"species\":{\"@id\":\"NCBITaxon:9612\",\"name\":\"Canis lupus\",\"equivalent_to\":{\"@id\":\"NCBITaxon:9612\",\"name\":\"Canis lupus\"}}}";
        resolveAndAssert(inputString, expectedOutput);
    }

}
