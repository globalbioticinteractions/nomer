package org.eol.globi.taxon;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.PropertyEnricherException;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ORCIDServiceTest {

    @Test
    public void lookupAuthorById() throws PropertyEnricherException {
        Map<String, String> enriched = new ORCIDService().enrich(new TreeMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, "http://orcid.org/0000-0002-6601-2165");
        }});

        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("http://orcid.org/0000-0002-6601-2165"));
        assertThat(enriched.get(PropertyAndValueDictionary.NAME), is("Christopher Mungall"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_NAMES), is("given-names | family-name"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH), is("Christopher | Mungall"));
    }

    @Test
    public void lookupAuthorById2() throws PropertyEnricherException {
        Map<String, String> enriched = new ORCIDService().enrich(new TreeMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, "ORCID:0000-0002-6601-2165");
        }});

        assertThat(enriched.get(PropertyAndValueDictionary.NAME), is("Christopher Mungall"));
    }

    @Test
    public void lookupAuthorById3() throws PropertyEnricherException {
        Map<String, String> enriched = new ORCIDService().enrich(new TreeMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, "https://orcid.org/0000-0002-6601-2165");
        }});

        assertThat(enriched.get(PropertyAndValueDictionary.NAME), is("Christopher Mungall"));
    }




}
