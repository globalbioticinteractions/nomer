package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.MatchTestUtil;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

public class TermMatcherRegistryTest {

    public static final List<String> SUPPORTED_SHORT_NAMES = Arrays.asList(
            "ala",
            "bold-web",
            "crossref-doi",
            "discoverlife",
            "envo",
            "eol-id",
            "gbif",
            "gbif-web",
            "globi-correct",
            "globi-enrich",
            "globalnames",
            "globi",
            "globi-rank",
            "gulfbase",
            "inaturalist-id",
            "itis",
            "itis-web",
            "nbn",
            "ncbi",
            "ncbi-web",
            "nodc",
            "openbiodiv",
            "plazi",
            "pmid-doi",
            "remove-stop-words",
            "translate-names",
            "wikidata-web",
            "worms");


    public static final Map<String, String> MATCH_NAME_MAPPER = new TreeMap<String, String>() {
        {
            put("ala-taxon", "ala");
            put("bold-web", "bold-web");
            put("crossref-doi", "crossref-doi");
            put("discoverlife-taxon", "discoverlife");
            put("envo-term", "envo");
            put("eol-taxon-id", "eol-id");
            put("gbif-taxon", "gbif");
            put("gbif-taxon-web", "gbif-web");
            put("globi-correct", "globi-correct");
            put("globi-enrich", "globi-enrich");
            put("globi-globalnames", "globalnames");
            put("globi-taxon-cache", "globi");
            put("globi-taxon-rank", "globi-rank");
            put("gulfbase-taxon", "gulfbase");
            put("inaturalist-taxon-id", "inaturalist-id");
            put("itis-taxon-id", "itis");
            put("itis-taxon-id-web", "itis-web");
            put("nbn-taxon-id", "nbn");
            put("ncbi-taxon", "ncbi");
            put("ncbi-taxon-id", "ncbi");
            put("ncbi-taxon-id-web", "ncbi-web");
            put("nodc-taxon-id", "nodc");
            put("openbiodiv", "openbiodiv");
            put("plazi", "plazi");
            put("pmid-doi", "pmid-doi");
            put("remove-stop-words", "remove-stop-words");
            put("translate-names", "translate-names");
            put("wikidata-taxon-id-web", "wikidata-web");
            put("worms-taxon", "worms");
        }
    };

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
        List<String> supportedMatchers = Arrays.asList(
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
                "ncbi-taxon-id",
                "ncbi-taxon-id-web",
                "nodc-taxon-id",
                "openbiodiv",
                "plazi",
                "pmid-doi",
                "remove-stop-words",
                "translate-names",
                "wikidata-taxon-id-web",
                "worms-taxon");

        for (String matcherName : supportedMatchers) {
            assertNotNull("failed to get supported matcher [" + matcherName + "]",
                    TermMatcherRegistry.termMatcherFor(
                            matcherName,
                            new MatchTestUtil.TermMatcherContextDefault())
            );
        }
    }

    @Test
    public void potatoPotato() {

    }

    @Test
    public void checkSupportedMatchersShortNames() {

        for (String supportedMatcher : SUPPORTED_SHORT_NAMES) {
            TermMatcher foundMatcher = null;
            for (Map.Entry<String, String> map : MATCH_NAME_MAPPER.entrySet()) {
                if (StringUtils.equals(supportedMatcher, map.getValue())) {
                    foundMatcher = TermMatcherRegistry.termMatcherFor(
                            map.getKey(),
                            new MatchTestUtil.TermMatcherContextDefault());
                }
            }
            assertNotNull("failed to find supported matcher for  [" + supportedMatcher + "]",
                    foundMatcher
            );
        }
    }

}