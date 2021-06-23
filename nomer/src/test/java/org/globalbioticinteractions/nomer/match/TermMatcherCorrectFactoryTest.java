package org.globalbioticinteractions.nomer.match;

import org.apache.commons.io.IOUtils;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.SuggesterFactory;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class TermMatcherCorrectFactoryTest {

    @Test
    public void init() {
        assertNotNull(new TermMatcherCorrectFactory().createTermMatcher(null));
    }

    @Test(expected = IllegalStateException.class)
    public void notInitialized() throws PropertyEnricherException {
        TermMatcher termMatcher = new TermMatcherCorrectFactory().createTermMatcher(null);
        termMatcher.match(Collections.singletonList(
                new TermImpl(null, "copepods")),
                (nodeId, name, taxon, nameType) -> {
                }
        );
    }

    @Test
    public void correct() throws PropertyEnricherException {
        assertCorrection("copepods", "Copepoda");
    }

    private TermMatcherContext createTestContext() {
        return new TermMatcherContext() {
            @Override
            public String getCacheDir() {
                return null;
            }

            @Override
            public InputStream getResource(String uri) throws IOException {
                return IOUtils.toInputStream("map".equals(uri) ? "copepods,Copepoda" : "unidentified", StandardCharsets.UTF_8);
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
            public String getProperty(String key) {
                return new HashMap<String, String>() {{
                    put(SuggesterFactory.NOMER_TAXON_NAME_CORRECTION_URL, "map");
                    put(SuggesterFactory.NOMER_TAXON_NAME_STOPWORD_URL, "words");
                }}.get(key);
            }
        };
    }

    @Test
    public void correctSingleTermWithStopword() throws PropertyEnricherException {
        assertCorrection("unidentified copepods", "Copepoda");
    }

    @Test
    public void correctSingleTermWithCapitalizedStopword() throws PropertyEnricherException {
        assertCorrection("Unidentified Copepods", "Copepoda");
    }

    @Test
    public void correctSnakeCase() throws PropertyEnricherException {
        assertCorrection("homo_sapiens", "Homo sapiens");
    }

    @Test
    public void correctBatSARSCoV() throws PropertyEnricherException {
        assertPassThroughName("Bat SARS CoV");
    }

    @Test
    public void correctVirusAcronymNPV() throws PropertyEnricherException {
        assertPassThroughName("Spodoptera frugiperda NPV");
    }

    private void assertPassThroughName(String passThoughName) throws PropertyEnricherException {
        assertCorrection(passThoughName, passThoughName);
    }

    private void assertCorrection(final String nameToBeCorrected, String expectedCorrection) throws PropertyEnricherException {
        TermMatcher termMatcher = new TermMatcherCorrectFactory().createTermMatcher(createTestContext());
        AtomicBoolean found = new AtomicBoolean(false);
        Term batVirusTerm = new TermImpl(null, nameToBeCorrected);
        termMatcher.match(
                Collections.singletonList(batVirusTerm),
                (nodeId, name1, taxon, nameType) -> {
                    assertThat(taxon.getName(), Is.is(expectedCorrection));
                    found.set(true);
                });
        assertTrue(found.get());
    }


}