package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.globalbioticinteractions.nomer.util.MatchTestUtil;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class TermMatcherRegistryTest {

    @Test(expected = IllegalArgumentException.class)
    public void createNonExistingMatcher() {
        try {
            TermMatcherRegistry.termMatcherFor("this doesn't exist", new MatchTestUtil.TermMatcherContextDefault());
        } catch (Throwable ex) {
            assertThat(ex.getMessage(), is("unknown matcher [this doesn't exist]"));
            throw ex;
        }
    }

    @Test
    public void createExistingMatcher() {
        assertNotNull(TermMatcherRegistry.termMatcherFor("itis-taxon-id", new MatchTestUtil.TermMatcherContextDefault()));
    }


    @Test
    public void checkSupportedMatchers() {
        String supportedMatchers = "ala-taxon\n" +
                "bold-web\n" +
                "crossref-doi\n" +
                "discoverlife-taxon\n" +
                "envo-term\n" +
                "eol-taxon-id\n" +
                "gbif-taxon\n" +
                "gbif-taxon-web\n" +
                "globi-correct\n" +
                "globi-enrich\n" +
                "globi-globalnames\n" +
                "globi-taxon-cache\n" +
                "globi-taxon-rank\n" +
                "gulfbase-taxon\n" +
                "inaturalist-taxon-id\n" +
                "itis-taxon-id\n" +
                "itis-taxon-id-web\n" +
                "nbn-taxon-id\n" +
                "ncbi-taxon\n" +
                "ncbi-taxon-id\n" +
                "ncbi-taxon-id-web\n" +
                "nodc-taxon-id\n" +
                "openbiodiv\n" +
                "plazi\n" +
                "pmid-doi\n" +
                "remove-stop-words\n" +
                "translate-names\n" +
                "wikidata-taxon-id-web\n" +
                "worms-taxon";

        String[] matcherNames = StringUtils.split(supportedMatchers, "\n");

        for (String matcherName : matcherNames) {
            assertNotNull("failed to get supported matcher [" + matcherName + "]", TermMatcherRegistry.termMatcherFor(matcherName, new MatchTestUtil.TermMatcherContextDefault()));

        }
    }

}