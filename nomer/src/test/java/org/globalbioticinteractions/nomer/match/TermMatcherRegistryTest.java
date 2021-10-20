package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.MatchTestUtil;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.fail;

public class TermMatcherRegistryTest {


    public static final List<String> SUPPORTED_LONG_NAMES = Collections.unmodifiableList(Arrays.asList(
            "ala-taxon",
            "bold-web",
            "crossref-doi",
            "discoverlife-taxon",
            "envo-term",
            "eol-taxon-id",
            "gbif-taxon",
            "gbif-taxon-web",
            "globi-correct",
            "globi-enrich",
            "globi-globalnames",
            "globi-taxon-cache",
            "globi-taxon-rank",
            "gulfbase-taxon",
            "inaturalist-taxon-id",
            "itis-taxon-id",
            "itis-taxon-id-web",
            "nbn-taxon-id",
            "ncbi-taxon",
            "ncbi-taxon-id-web",
            "nodc-taxon-id",
            "openbiodiv",
            "plazi",
            "pmid-doi",
            "remove-stop-words",
            "translate-names",
            "wikidata-taxon-id-web",
            "worms-taxon"));

    @Test(expected = IllegalArgumentException.class)
    public void createNonExistingMatcher() {
        try {
            TermMatcherRegistry.termMatcherFor("this doesn't exist",
                    new MatchTestUtil.TermMatcherContextDefault()
            );
        } catch (Throwable ex) {
            assertThat(ex.getMessage(), is("unknown matcher [this doesn't exist]"));
            throw ex;
        }
    }

    @Test
    public void createExistingMatcher() {
        assertNotNull(TermMatcherRegistry.termMatcherFor("itis-taxon-id",
                new MatchTestUtil.TermMatcherContextDefault())
        );
    }


    @Test
    public void checkSupportedMatchersLegacyNames() {
        for (String matcherName : SUPPORTED_LONG_NAMES) {
            assertNotNull("failed to get supported matcher [" + matcherName + "]",
                    TermMatcherRegistry.termMatcherFor(
                            matcherName,
                            new MatchTestUtil.TermMatcherContextDefault())
            );
        }
    }

    @Test
    public void checkAvailableVsMappedNames() {
        Map<String, TermMatcherFactory> registry = TermMatcherRegistry.getRegistry(null);
        Collection<String> matcherLongNames = registry.keySet();
        for (String matcherLongName : matcherLongNames) {
            assertThat(TermMatcherRegistry.MATCH_NAME_MAPPER.keySet(), hasItem(matcherLongName));
        }
    }

    @Test
    public void checkSupportedVsLongNames() {
        Map<String, TermMatcherFactory> registry = TermMatcherRegistry.getRegistry(null);
        Collection<String> matcherLongNames = registry.keySet();
        for (String matcherLongName : matcherLongNames) {
            assertThat(SUPPORTED_LONG_NAMES, hasItem(matcherLongName));
        }
    }

    @Test
    public void checkSupportedMatchersShortNames() {
        for (String supportedMatcher : TermMatcherRegistry.SUPPORTED_SHORT_NAMES) {
            TermMatcher foundMatcher = null;
            String longName = TermMatcherRegistry.getMatcherLongName(supportedMatcher);
            if (longName != null) {
                foundMatcher = TermMatcherRegistry.termMatcherFor(
                        longName,
                        new MatchTestUtil.TermMatcherContextDefault());
            }

            assertNotNull("failed to find supported matcher for  [" + supportedMatcher + "]",
                    foundMatcher
            );
        }
    }

}