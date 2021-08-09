package org.eol.globi.taxon;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.junit.Test;

public class GBIFNameServiceTest {
    
    @Test
    public void lookupByCode() throws PropertyEnricherException {
        Map<String, String> enriched = getTaxonInfo("Gaultheria procumbens");
        
        assertThat(enriched.get(PropertyAndValueDictionary.NAME), is("Gaultheria procumbens"));
        assertThat(enriched.get(PropertyAndValueDictionary.RANK), is("species"));
        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("GBIF:2882753"));
        assertThat(enriched.get(PropertyAndValueDictionary.COMMON_NAMES), containsString("creeping wintergreen @en"));
        // for some reason gbif api is returning funny characters
        assertThat(enriched.get(PropertyAndValueDictionary.COMMON_NAMES), containsString("Wintergrün @de"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_IDS), is("GBIF:6 | GBIF:7707728 | GBIF:220 | GBIF:1353 | GBIF:2505 | GBIF:2882751 | GBIF:2882753"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | order | family | genus | species"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH), is("Plantae | Tracheophyta | Magnoliopsida | Ericales | Ericaceae | Gaultheria | Gaultheria procumbens"));
    }

    @Test
    public void lookupByCodeSynonym() throws PropertyEnricherException {
        Map<String, String> enriched = getTaxonInfo("Ariopsis felis");
        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("GBIF:5202927"));
        assertThat(enriched.get(PropertyAndValueDictionary.NAME), is("Ariopsis felis"));
        assertThat(enriched.get(PropertyAndValueDictionary.RANK), is("species"));
    }

    @Test
    public void lookupByCodeSubspecies() throws PropertyEnricherException {
        Map<String, String> enriched = getTaxonInfo("Enhydra lutris nereis");
        assertThat(enriched.get(PropertyAndValueDictionary.NAME), is("Enhydra lutris nereis"));
        assertThat(enriched.get(PropertyAndValueDictionary.RANK), is("subspecies"));
        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("GBIF:6163936"));
        assertThat(enriched.get(PropertyAndValueDictionary.COMMON_NAMES), containsString("southern sea otter @en"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_IDS), is("GBIF:1 | GBIF:44 | GBIF:359 | GBIF:732 | GBIF:5307 | GBIF:2433669 | GBIF:2433670"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | order | family | genus | species"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH), is("Animalia | Chordata | Mammalia | Carnivora | Mustelidae | Enhydra | Enhydra lutris"));
    }

    protected Map<String, String> getTaxonInfo(final String taxonName) throws PropertyEnricherException {
        Map<String, String> props = Collections.unmodifiableMap(new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.NAME, taxonName);
        }});
        PropertyEnricher propertyEnricher = new GBIFNameService();
        return propertyEnricher.enrichFirstMatch(props);
    }

}
