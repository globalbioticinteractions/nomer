package org.globalbioticinteractions.nomer.util;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.RowHandler;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class TermMatchingRowJsonHandlerTest {

    @Test
    public void resolveWithEnricher() throws IOException, PropertyEnricherException {
        String inputString = "ITIS:180596\tCanis lupus";
        String expectedOutput = "{\"species\":{\"@id\":\"ITIS:180596\",\"name\":\"Canis lupus\",\"equivalent_to\":{\"@id\":\"ITIS:180596\",\"name\":\"Canis lupus\"}}}";
        resolveAndAssert(inputString, expectedOutput, term -> new TaxonImpl(term.getName(), term.getId()));
    }

    @Test
    public void resolveNCBIWithEnricher() throws IOException, PropertyEnricherException {
        String inputString = "NCBI:9612\tCanis lupus";
        String expectedOutput = "{\"species\":{\"@id\":\"NCBITaxon:9612\",\"name\":\"Canis lupus\",\"equivalent_to\":{\"@id\":\"NCBITaxon:9612\",\"name\":\"Canis lupus\"}}}";
        resolveAndAssert(inputString, expectedOutput, term -> new TaxonImpl(term.getName(), term.getId()));
    }

    @Test
    public void resolveNCBIWithEnricherNoID() throws IOException, PropertyEnricherException {
        String inputString = "\tCanis lupus";
        String expectedOutput = "{\"species\":{\"@id\":\"EOL:328607\",\"name\":\"Canis lupus\",\"equivalent_to\":{\"name\":\"Canis lupus\"}}}";
        resolveAndAssert(inputString, expectedOutput, term -> new TaxonImpl(term.getName(), "EOL:328607"));
    }

    @Test
    public void resolveNCBIWithEnricherEmptyName() throws IOException, PropertyEnricherException {
        String inputString = "\tCanis lupus";
        String expectedOutput = "{\"species\":{\"@id\":\"EOL:328607\",\"equivalent_to\":{\"name\":\"Canis lupus\"}}}";
        resolveAndAssert(inputString, expectedOutput, term -> new TaxonImpl(null, "EOL:328607"));
    }

    @Test
    public void resolveNCBIWithEnricherWithPath() throws IOException, PropertyEnricherException {
        String inputString = "\tCanis lupus";
        String expectedOutput = "{\"species\":{\"@id\":\"EOL:328607\",\"name\":\"Canis lupus\",\"equivalent_to\":{\"name\":\"Canis lupus\"}},\"rank1\":{\"@id\":\"id1\",\"name\":\"name1\"},\"norank\":{\"@id\":\"id2\",\"name\":\"name2\"},\"path\":{\"names\":[\"name1\",\"name2\",\"name3\"],\"ids\":[\"id1\",\"id2\",\"id3\"],\"ranks\":[\"rank1\",\"\",\"species\"]}}";
        TermMapper termMapper = term -> {
            TaxonImpl taxon = new TaxonImpl(term.getName(), "EOL:328607");
            taxon.setRank("species");
            taxon.setPath("name1 | name2 | name3");
            taxon.setPathIds("id1 | id2 | id3");
            taxon.setPathNames("rank1 | | species");
            return taxon;
        };
        resolveAndAssert(inputString, expectedOutput, termMapper);
    }

    @Test
    public void resolveNCBIWithEnricherWithNullPath() throws IOException, PropertyEnricherException {
        String inputString = "\tCanis lupus";
        String expectedOutput = "{\"species\":{\"@id\":\"EOL:328607\",\"name\":\"Canis lupus\",\"equivalent_to\":{\"name\":\"Canis lupus\"}}}";
        TermMapper termMapper = term -> {
            TaxonImpl taxon = new TaxonImpl(term.getName(), "EOL:328607");
            taxon.setPath(null);
            taxon.setPathIds("id1 | id2 | id3");
            taxon.setPathNames("rank1 | | rank3");
            return taxon;
        };
        resolveAndAssert(inputString, expectedOutput, termMapper);
    }

    @Test
    public void resolveNCBIWithEnricherWithMisalignedPath() throws IOException, PropertyEnricherException {
        String inputString = "\tCanis lupus";
        String expectedOutput = "{\"species\":{\"@id\":\"EOL:328607\",\"name\":\"Canis lupus\",\"equivalent_to\":{\"name\":\"Canis lupus\"}}}";
        TermMapper termMapper = term -> {
            TaxonImpl taxon = new TaxonImpl(term.getName(), "EOL:328607");
            taxon.setPath("name1 | name2");
            taxon.setPathIds("id1 | id2 | id3");
            taxon.setPathNames("rank1 | | rank3");
            return taxon;
        };
        resolveAndAssert(inputString, expectedOutput, termMapper);
    }

    private void resolveAndAssert(String inputString, String expectedOutput, TermMapper termMapper) throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream(inputString);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        TermMatcherContext ctx = new MatchTestUtil.TermMatcherContextDefault();
        TermMatcher matcher = new MappingTermMatcher(termMapper);
        RowHandler rowHandler = new TermMatchingRowJsonHandler(os, matcher, ctx);
        MatchUtil.resolve(is, rowHandler);
        JsonNode jsonNode = new ObjectMapper().readTree(os.toString());
        JsonNode expectedJson = new ObjectMapper().readTree(expectedOutput);
        assertThat(jsonNode, Is.is(expectedJson));
    }

    public static class MappingTermMatcher implements TermMatcher {

        private final TermMapper mapper;

        public MappingTermMatcher(TermMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        public void findTermsForNames(List<String> list, TermMatchListener termMatchListener) throws PropertyEnricherException {
            findTerms(list.stream().map(name -> new TermImpl(null, name)).collect(Collectors.toList()), termMatchListener);
        }

        @Override
        public void findTerms(List<Term> list, TermMatchListener termMatchListener) throws PropertyEnricherException {
            for (Term term : list) {
                Taxon taxon = mapper.mapTerm(term);
                taxon.setRank("species");
                termMatchListener.foundTaxonForName(null, term.getName(), taxon, NameType.SAME_AS);
            }
        }

    }

    public interface TermMapper {
        Taxon mapTerm(Term term);
    }
}
