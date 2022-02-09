package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.EnvoService;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

public class ITISTaxonServiceTest {

    @Test
    public void enrichById() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl(null, "ITIS:57");
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Bradyrhizobiaceae | Nitrobacter"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("ITIS:57"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Nitrobacter"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("genus"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("ITIS:956340 | ITIS:57"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("family | genus"));
    }

    @Test
    public void enrichByName() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl("Nitrobacter", null);
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Bradyrhizobiaceae | Nitrobacter"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("ITIS:57"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Nitrobacter"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("genus"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("ITIS:956340 | ITIS:57"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("family | genus"));
    }

    @Test
    public void enrichMerged() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl(null, "ITIS:57");
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Bradyrhizobiaceae | Nitrobacter"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("ITIS:57"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Nitrobacter"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("genus"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("ITIS:956340 | ITIS:57"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("family | genus"));
    }

    @Test
    public void enrichNoMatch() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(new TaxonImpl(null, "ITIS:999999999")));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is(nullValue()));
    }

    @Test
    public void enrichPrefixMismatch() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(new TaxonImpl(null, "FOO:2")));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is(nullValue()));
    }

    private ITISTaxonService createService() {
        File file = new File("target/cache" + UUID.randomUUID());
        return new ITISTaxonService(new TermMatcherContext() {
            @Override
            public String getCacheDir() {
                return file.getAbsolutePath();
            }

            @Override
            public InputStream retrieve(URI uri) throws IOException {
                return getClass().getResourceAsStream(uri.toString());
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
            public String getOutputFormat() {
                return null;
            }

            @Override
            public String getProperty(String key) {
                return new TreeMap<String, String>() {
                    {
                        put("nomer.itis.taxonomic_units", "/org/globalbioticinteractions/nomer/match/itis/taxonomic_units.psv");
                        put("nomer.itis.synonym_links", "/org/globalbioticinteractions/nomer/match/itis/synonym_links.psv");
                        put("nomer.itis.taxon_unit_types", "/org/globalbioticinteractions/nomer/match/itis/taxon_unit_types.psv");
                    }
                }.get(key);
            }
        });
    }

    @Test
    public void parseNodes() throws PropertyEnricherException {
        Map<Long, Map<String, String>> node = new TreeMap<>();
        Map<Long, Long> childParent = new TreeMap<>();
        Map<String, String> rankIdNameMap = new TreeMap<String, String>() {{
            put("1-180", "some rank");
        }};

        InputStream nodesStream = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/itis/taxonomic_units.psv");
        ITISTaxonService.parseNodes(node, childParent, rankIdNameMap, nodesStream);

        assertThat(childParent.get(57L), is(956340L));
        assertThat(TaxonUtil.mapToTaxon(node.get(57L)).getRank(), is("some rank"));

    }

    @Test
    public void parseMerged() throws PropertyEnricherException {
        Map<Long, Long> mergedIds = new TreeMap<>();

        InputStream mergedStream = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/itis/synonym_links.psv");
        ITISTaxonService.parseMerged(mergedIds, mergedStream);

        assertThat(mergedIds.get(51L), is(50L));
        assertThat(mergedIds.get(52L), is(50L));

    }

    @Test
    public void parseRankMap() throws PropertyEnricherException {
        Map<String, String> rankIds = new TreeMap<>();

        String resource = "/org/globalbioticinteractions/nomer/match/itis/taxon_unit_types.psv";
        InputStream mergedStream = getClass().getResourceAsStream(resource);
        ITISTaxonService.parseTaxonUnitTypes(rankIds, mergedStream);

        assertThat(rankIds.get("1-180"), is("genus"));
        assertThat(rankIds.get("1-220"), is("species"));

    }

    @Test
    public void denormalizeTaxa() {
        Map<Long, Map<String, String>> taxonMap = new TreeMap<Long, Map<String, String>>() {{
            TaxonImpl one = new TaxonImpl("one name", "1");
            one.setRank("rank one");
            put(1L, TaxonUtil.taxonToMap(one));

            TaxonImpl two = new TaxonImpl("two name", "2");
            two.setRank("rank two");
            put(2L, TaxonUtil.taxonToMap(two));
        }};

        Map<String, List<Map<String, String>>> taxonMapDenormalized = new TreeMap<>();
        Map<Long, List<Map<String, String>>> taxonMapDenormalizedIds = new TreeMap<>();

        Map<Long, Long> childParent = new TreeMap<Long, Long>() {{
            put(1L, 2L);
        }};

        createService().denormalizeTaxa(
                taxonMap,
                taxonMapDenormalized,
                taxonMapDenormalizedIds,
                childParent);

        assertOneName(TaxonUtil.mapToTaxon(taxonMapDenormalized.get("one name").get(0)));
        assertOneName(TaxonUtil.mapToTaxon(taxonMapDenormalizedIds.get(1L).get(0)));

        assertTwoName(TaxonUtil.mapToTaxon(taxonMapDenormalized.get("two name").get(0)));
        assertTwoName(TaxonUtil.mapToTaxon(taxonMapDenormalizedIds.get(2L).get(0)));


    }

    private void assertTwoName(Taxon two) {
        assertThat(two.getPath(), is("two name"));
        assertThat(two.getPathIds(), is("2"));
        assertThat(two.getPathNames(), is("rank two"));
        assertThat(two.getRank(), is("rank two"));
        assertThat(two.getName(), is("two name"));
        assertThat(two.getExternalId(), is("2"));
    }

    private void assertOneName(Taxon actual) {
        assertThat(actual.getPath(), is("two name | one name"));
        assertThat(actual.getPathIds(), is("2 | 1"));
        assertThat(actual.getPathNames(), is("rank two | rank one"));
        assertThat(actual.getRank(), is("rank one"));
        assertThat(actual.getName(), is("one name"));
        assertThat(actual.getExternalId(), is("1"));
    }

    @Test
    public void findIdByName() throws PropertyEnricherException {
        TaxonImpl taxon = new TaxonImpl();
        taxon.setName("detritus");
        assertThat(new EnvoService().enrich(TaxonUtil.taxonToMap(taxon)).get(PropertyAndValueDictionary.EXTERNAL_ID), is("ENVO:01001103"));
    }

    @Test
    public void findPathById() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.EXTERNAL_ID, "ENVO:01000155");
            }
        };
        Map<String, String> enrichedProperties = new EnvoService().enrich(properties);
        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enrichedProperties);
        assertThat(enrichedTaxon.getName(), is("organic material"));
        assertThat(enrichedTaxon.getExternalId(), is("ENVO:01000155"));
        assertThat(enrichedTaxon.getPath(), is("environmental material | organic material"));
        assertThat(enrichedTaxon.getPathIds(), is("ENVO:00010483 | ENVO:01000155"));
    }

}