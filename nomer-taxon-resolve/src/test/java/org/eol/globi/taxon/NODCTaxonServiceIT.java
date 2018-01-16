package org.eol.globi.taxon;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.PropertyEnricherException;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class NODCTaxonServiceIT {

    private NODCTaxonService nodcTaxonService;

    @Before
    public void init() throws IOException, PropertyEnricherException {
        nodcTaxonService = new NODCTaxonService(new TermMatcherContext() {
            @Override
            public String getCacheDir() {
                return "target";
            }

            @Override
            public String getProperty(String key) {
                return null;
            }

            @Override
            public InputStream getResource(String uri) throws IOException {
                return null;
            }

            @Override
            public List<String> getMatchers() {
                return null;
            }

            @Override
            public Map<Integer, String> getInputSchema() {
                return null;
            }

        });
        nodcTaxonService.init(NODCTaxonParserTest.getTestParser());
    }

    @After
    public void shutdown() {
        nodcTaxonService.shutdown();
    }

    @Test
    public void lookup() throws IOException, PropertyEnricherException {
        final Map<String, String> enriched = nodcTaxonService.enrich(new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.EXTERNAL_ID, "NODC:9227040101");
            }
        });

        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("ITIS:552761"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH), containsString("Pecari tajacu"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_IDS), containsString("ITIS:552761"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH), not(containsString("Pecari tajacu angulatus")));
    }

    @Test
    public void lookupReplacement() throws IOException, PropertyEnricherException {
        final Map<String, String> enriched = nodcTaxonService.enrich(new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.EXTERNAL_ID, "NODC:8831020404");
            }
        });

        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("ITIS:167424"));
    }


}