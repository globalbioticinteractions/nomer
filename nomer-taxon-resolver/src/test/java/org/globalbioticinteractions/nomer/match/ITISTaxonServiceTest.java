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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ITISTaxonServiceTest {

    @Test
    public void enrich() throws PropertyEnricherException {
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

    private PropertyEnricher createService() {
        File file = new File("target/cache" + UUID.randomUUID());
        return new ITISTaxonService(new TermMatcherContext() {
            @Override
            public String getCacheDir() {
                return file.getAbsolutePath();
            }

            @Override
            public InputStream getResource(String uri) throws IOException {
                return getClass().getResourceAsStream(uri);
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
        Map<String, Map<String,String>> node = new TreeMap<>();
        Map<String, String> childParent = new TreeMap<>();
        Map<String, String> rankIdNameMap = new TreeMap<String, String>() {{
           put("1-180", "some rank");
        }};

        InputStream nodesStream = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/itis/taxonomic_units.psv");
        ITISTaxonService.parseNodes(node, childParent, rankIdNameMap, nodesStream);

        assertThat(childParent.get("ITIS:57"), is("ITIS:956340"));
        assertThat(TaxonUtil.mapToTaxon(node.get("ITIS:57")).getRank(), is("some rank"));

    }

    @Test
    public void parseMerged() throws PropertyEnricherException {
        Map<String, String> mergedIds = new TreeMap<>();

        InputStream mergedStream = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/itis/synonym_links.psv");
        ITISTaxonService.parseMerged(mergedIds, mergedStream);

        assertThat(mergedIds.get("ITIS:51"), is("ITIS:50"));
        assertThat(mergedIds.get("ITIS:52"), is("ITIS:50"));

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
    public void denormalizeTaxa() throws PropertyEnricherException {
        Map<String, Map<String, String>> taxonMap = new TreeMap<String, Map<String, String>>() {{
            TaxonImpl one = new TaxonImpl("one name", "1");
            one.setRank("rank one");
            put("1", TaxonUtil.taxonToMap(one));

            TaxonImpl two = new TaxonImpl("two name", "2");
            two.setRank("rank two");
            put("2", TaxonUtil.taxonToMap(two));
        }};

        Map<String, Map<String, String>> taxonMapDenormalized = new TreeMap<>();

        Map<String, String> childParent = new TreeMap<String, String>() {{
            put("1", "2");
        }};


        ITISTaxonService.denormalizeTaxa(taxonMap, taxonMapDenormalized, childParent);

        Taxon actual = TaxonUtil.mapToTaxon(taxonMapDenormalized.get("1"));
        assertThat(actual.getPath(), is("two name | one name"));
        assertThat(actual.getPathIds(), is("2 | 1"));
        assertThat(actual.getPathNames(), is("rank two | rank one"));
        assertThat(actual.getRank(), is("rank one"));
        assertThat(actual.getName(), is("one name"));
        assertThat(actual.getExternalId(), is("1"));

        Taxon two = TaxonUtil.mapToTaxon(taxonMapDenormalized.get("2"));
        assertThat(two.getPath(), is("two name"));
        assertThat(two.getPathIds(), is("2"));
        assertThat(two.getPathNames(), is("rank two"));
        assertThat(two.getRank(), is("rank two"));
        assertThat(two.getName(), is("two name"));
        assertThat(two.getExternalId(), is("2"));


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