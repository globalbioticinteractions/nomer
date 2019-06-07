package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.ExternalIdUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GBIFServiceTest {

    private Map<String, TaxonomyProvider> prefixForTaxonProvider = new HashMap<String, TaxonomyProvider>() {{
       put("https://www.gbif.org/species/", TaxonomyProvider.GBIF);
    }};

    @Test
    public void providerForExternalId() {
        String id = "http://www.gbif.org/species/110462373";
        TaxonomyProvider provider = null;

        List<String> keys = new ArrayList<>();
        TaxonomyProvider[] values = TaxonomyProvider.values();
        for (TaxonomyProvider value : values) {
            keys.add(value.getIdPrefix());
        }

        Map<String, String> urlPrefixMap = ExternalIdUtil.getURLPrefixMap();
        for (Map.Entry<String, String> entry : urlPrefixMap.entrySet()) {
            if (StringUtils.startsWith(id, entry.getValue())) {
                if (keys.contains(entry.getKey())) {
                    provider = TaxonomyProvider.valueOf(entry.getKey().substring(0, entry.getKey().length() - 1));
                    break;
                }
            }
        }
        assertThat(provider, is(TaxonomyProvider.GBIF));
    }

    @Test
    public void lookupByCode() throws IOException, PropertyEnricherException {
        Map<String, String> enriched = getTaxonInfo("2882753");
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
    public void lookupByCodeSynonym() throws IOException, PropertyEnricherException {
        Map<String, String> enriched = getTaxonInfo("2344811");
        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("GBIF:5202927"));
        assertThat(enriched.get(PropertyAndValueDictionary.NAME), is("Ariopsis felis"));
        assertThat(enriched.get(PropertyAndValueDictionary.RANK), is("species"));
    }

    @Test
    public void lookupByCodeSubspecies() throws IOException, PropertyEnricherException {
        Map<String, String> enriched = getTaxonInfo("6163936");
        assertThat(enriched.get(PropertyAndValueDictionary.NAME), is("Enhydra lutris nereis"));
        assertThat(enriched.get(PropertyAndValueDictionary.RANK), is("subspecies"));
        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("GBIF:6163936"));
        assertThat(enriched.get(PropertyAndValueDictionary.COMMON_NAMES), is("southern sea otter @en"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_IDS), is("GBIF:1 | GBIF:44 | GBIF:359 | GBIF:732 | GBIF:5307 | GBIF:2433669 | GBIF:2433670"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | order | family | genus | species"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH), is("Animalia | Chordata | Mammalia | Carnivora | Mustelidae | Enhydra | Enhydra lutris"));
    }

    protected Map<String, String> getTaxonInfo(final String gbifId) throws PropertyEnricherException {
        Map<String, String> props = Collections.unmodifiableMap(new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, TaxonomyProvider.GBIF.getIdPrefix() + gbifId);
        }});
        PropertyEnricher propertyEnricher = new GBIFService();
        return propertyEnricher.enrich(props);
    }

}
