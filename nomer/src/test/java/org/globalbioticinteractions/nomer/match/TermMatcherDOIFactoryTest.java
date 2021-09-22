package org.globalbioticinteractions.nomer.match;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TermMatcherDOIFactoryTest {

    private HashMap<String, String> testPropertyMap;

    @Test
    public void correct() throws PropertyEnricherException {
        testPropertyMap = new HashMap<String, String>() {{
            put(TermMatcherDOIFactory.NOMER_DOI_CACHE_URL, "map");
        }};

        TermMatcher termMatcher = new TermMatcherDOIFactory()
                .createTermMatcher(createTestContext());
        AtomicBoolean found = new AtomicBoolean(false);
        termMatcher.match(Collections.singletonList(new TermImpl(null, "some citation")), (nodeId, name, taxon, nameType) -> {
            assertThat(taxon.getExternalId(), Is.is("https://doi.org/10.123/456"));
            found.set(true);
        });
        assertTrue(found.get());
    }

    private TermMatcherContext createTestContext() {
        return new TermMatcherContext() {
            @Override
            public String getCacheDir() {
                return null;
            }

            @Override
            public InputStream getResource(String uri) throws IOException {
                if (StringUtils.equals("map", uri)) {
                    return IOUtils.toInputStream("uri\treference\nhttps://doi.org/10.123/456\tsome citation", StandardCharsets.UTF_8);
                } else {
                    throw new IOException("[" + uri + "] not found");
                }
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
                return testPropertyMap.get(key);
            }
        };
    }

    @Test
    public void resolveCitationUsingAPI() throws PropertyEnricherException {
        testPropertyMap = new HashMap<String, String>() {{
            put(TermMatcherDOIFactory.NOMER_DOI_CROSSREF_MIN_SCORE, "100");
        }};

        AtomicBoolean found = resolveDOI(new DOI("1017", "S0266467405002920"));
        assertTrue(found.get());
    }

    @Test
    public void resolveCitationUsingAPIHighMatchScore() throws PropertyEnricherException {
        testPropertyMap = new HashMap<String, String>() {{
            put(TermMatcherDOIFactory.NOMER_DOI_CROSSREF_MIN_SCORE, "10000");
        }};

        AtomicBoolean found = resolveDOI(null);
        assertFalse(found.get());
    }

    private AtomicBoolean resolveDOI(final DOI expectedDOI) throws PropertyEnricherException {
        TermMatcher termMatcher = new TermMatcherDOIFactory().createTermMatcher(createTestContext());
        AtomicBoolean found = new AtomicBoolean(false);
        termMatcher.match(Collections.singletonList(new TermImpl(null, "Kalka, Margareta, and Elisabeth K. V. Kalko. Gleaning Bats as Underestimated Predators of Herbivorous Insects: Diet of Micronycteris Microtis (Phyllostomidae) in Panama. Journal of Tropical Ecology 1 (2006): 1-10.")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long nodeId, Term term, Taxon taxon, NameType nameType) {
                try {
                    if (null != expectedDOI) {
                        assertThat(DOI.create(taxon.getId()), Is.is(expectedDOI));
                    }
                    found.set(nameType == NameType.SAME_AS);
                } catch (MalformedDOIException e) {
                    fail("malformed doi found");
                }
            }
        });
        return found;
    }

}