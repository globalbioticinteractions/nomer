package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.ExternalIdUtil;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class GBIFServiceTest {

    @Test
    public void providerForExternalId() {
        String id = "https://www.gbif.org/species/110462373";
        assertThat(ExternalIdUtil.taxonomyProviderFor(id), is(TaxonomyProvider.GBIF));
    }

    @Test
    public void lookupByCode() throws PropertyEnricherException {
        Map<String, String> enriched = lookupTaxonById("2882753");
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
        Map<String, String> enriched = lookupTaxonById("2344811");
        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("GBIF:5202927"));
        assertThat(enriched.get(PropertyAndValueDictionary.NAME), is("Ariopsis felis"));
        assertThat(enriched.get(PropertyAndValueDictionary.RANK), is("species"));
    }

    @Test
    public void lookupByCodeSubspecies() throws PropertyEnricherException {
        Map<String, String> enriched = lookupTaxonById("6163936");
        assertThat(enriched.get(PropertyAndValueDictionary.NAME), is("Enhydra lutris nereis"));
        assertThat(enriched.get(PropertyAndValueDictionary.RANK), is("subspecies"));
        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("GBIF:6163936"));
        assertThat(enriched.get(PropertyAndValueDictionary.COMMON_NAMES), containsString("Southern Sea Otter @en"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_IDS), is("GBIF:1 | GBIF:44 | GBIF:359 | GBIF:732 | GBIF:5307 | GBIF:2433669 | GBIF:2433670"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | order | family | genus | species"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH), is("Animalia | Chordata | Mammalia | Carnivora | Mustelidae | Enhydra | Enhydra lutris"));
    }

    protected Map<String, String> lookupTaxonById(final String gbifId) throws PropertyEnricherException {
        Map<String, String> props = Collections.unmodifiableMap(new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, TaxonomyProvider.GBIF.getIdPrefix() + gbifId);
        }});
        PropertyEnricher propertyEnricher = new GBIFService();
        return propertyEnricher.enrichFirstMatch(props);
    }

    protected Map<String, String> lookupTaxonByName(final String name) throws PropertyEnricherException {
        Map<String, String> props = Collections.unmodifiableMap(new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.NAME, name);
        }});
        PropertyEnricher propertyEnricher = new GBIFService();
        return propertyEnricher.enrichFirstMatch(props);
    }

    @Test
    public void lookupByName() throws PropertyEnricherException {
        Map<String, String> enriched = lookupTaxonByName("Gaultheria procumbens");

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
        Map<String, String> enriched = lookupTaxonByName("Arius felis");
        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("GBIF:5202927"));
        assertThat(enriched.get(PropertyAndValueDictionary.NAME), is("Ariopsis felis"));
        assertThat(enriched.get(PropertyAndValueDictionary.RANK), is("species"));
        assertThat(enriched.get(PropertyAndValueDictionary.COMMON_NAMES), containsString("Hardhead catfish"));
        List<String> commonNames = Arrays.asList(StringUtils.splitByWholeSeparator(enriched.get(PropertyAndValueDictionary.COMMON_NAMES), CharsetConstant.SEPARATOR));

        assertThat("expecting no duplicates in " + commonNames, commonNames.size(), is(new HashSet<String>(commonNames).size()));
    }

    @Test
    public void lookupBySubspeciesName() throws PropertyEnricherException {
        Map<String, String> enriched = lookupTaxonByName("Enhydra lutris nereis");
        assertThat(enriched.get(PropertyAndValueDictionary.NAME), is("Enhydra lutris nereis"));
        assertThat(enriched.get(PropertyAndValueDictionary.RANK), is("subspecies"));
        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("GBIF:6163936"));
        assertThat(enriched.get(PropertyAndValueDictionary.COMMON_NAMES), containsString("Southern Sea Otter @en"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_IDS), is("GBIF:1 | GBIF:44 | GBIF:359 | GBIF:732 | GBIF:5307 | GBIF:2433669 | GBIF:2433670"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | order | family | genus | species"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH), is("Animalia | Chordata | Mammalia | Carnivora | Mustelidae | Enhydra | Enhydra lutris"));
    }


}
