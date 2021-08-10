package org.eol.globi.taxon;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.junit.Test;

public class GBIFNameServiceTest {
    
    @Test
    public void lookupByName() throws PropertyEnricherException {
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
    public void lookupBySynonymName() throws PropertyEnricherException {
        Map<String, String> enriched = getTaxonInfo("Arius felis");
        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("GBIF:5202927"));
        assertThat(enriched.get(PropertyAndValueDictionary.NAME), is("Ariopsis felis"));
        assertThat(enriched.get(PropertyAndValueDictionary.RANK), is("species"));
        assertThat(enriched.get(PropertyAndValueDictionary.COMMON_NAMES), containsString("Hardhead catfish"));
        List<String> commonNames = Arrays.asList(StringUtils.splitByWholeSeparator(enriched.get(PropertyAndValueDictionary.COMMON_NAMES), CharsetConstant.SEPARATOR));

        assertThat("expecting no duplicates in " + commonNames, commonNames.size(), is(new HashSet<String>(commonNames).size()));
    }

    @Test
    public void lookupBySubspeciesName() throws PropertyEnricherException {
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
