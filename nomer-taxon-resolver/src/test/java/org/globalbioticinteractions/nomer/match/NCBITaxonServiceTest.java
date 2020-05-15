package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.EnvoService;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class NCBITaxonServiceTest {

    @Test
    public void enrich() throws PropertyEnricherException {
        NCBITaxonService service = createService();

        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(null, "NCBI:2")));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Bacteria"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("NCBI:2"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Bacteria"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("superkingdom"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("NCBI:2"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("superkingdom"));
    }

    @Test
    public void enrichOBOPrefix() throws PropertyEnricherException {
        NCBITaxonService service = createService();

        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(null, "http://purl.obolibrary.org/obo/NCBITaxon_2")));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Bacteria"));
    }

    @Test
    public void enrichOtherPrefix() throws PropertyEnricherException {
        NCBITaxonService service = createService();

        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(null, "NCBITaxon:2")));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Bacteria"));
    }

    @Test
    public void enrichMerged() throws PropertyEnricherException {
        NCBITaxonService service = createService();

        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(null, "NCBI:666")));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Bacteria"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("NCBI:2"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Bacteria"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("superkingdom"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("NCBI:2"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("superkingdom"));
    }

    @Test
    public void enrichNoMatch() throws PropertyEnricherException {
        NCBITaxonService service = createService();

        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(null, "NCBI:999999999")));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is(nullValue()));
    }

    @Test
    public void enrichPrefixMismatch() throws PropertyEnricherException {
        NCBITaxonService service = createService();

        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(null, "FOO:2")));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is(nullValue()));
    }

    private NCBITaxonService createService() {
        return new NCBITaxonService(new TermMatcherContext() {
                @Override
                public String getCacheDir() {
                    return null;
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
                            put("nomer.ncbi.nodes", "/org/globalbioticinteractions/nomer/match/ncbi/nodes.dmp");
                            put("nomer.ncbi.names", "/org/globalbioticinteractions/nomer/match/ncbi/names.dmp");
                            put("nomer.ncbi.merged", "/org/globalbioticinteractions/nomer/match/ncbi/merged.dmp");
                        }
                    }.get(key);
                }
            });
    }

    @Test
    public void parseNodes() throws PropertyEnricherException {
        Map<String, Map<String,String>> node = new TreeMap<>();
        Map<String, String> childParent = new TreeMap<>();

        InputStream nodesStream = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/ncbi/nodes.dmp");
        NCBITaxonService.parseNodes(node, childParent, nodesStream);

        assertThat(childParent.get("NCBI:2"), is("NCBI:131567"));
        assertThat(TaxonUtil.mapToTaxon(node.get("NCBI:2")).getRank(), is("superkingdom"));

    }

    @Test
    public void parseMerged() throws PropertyEnricherException {
        Map<String, String> mergedIds = new TreeMap<>();

        InputStream mergedStream = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/ncbi/merged.dmp");
        NCBITaxonService.parseMerged(mergedIds, mergedStream);

        assertThat(mergedIds.get("NCBI:12"), is("NCBI:74109"));
        assertThat(mergedIds.get("NCBI:46"), is("NCBI:39"));

    }

    @Test
    public void denormalizeTaxa() throws PropertyEnricherException {
        Map<String, Map<String, String>> taxonMap = new TreeMap<String, Map<String, String>>() {{
            TaxonImpl one = new TaxonImpl(null, "1");
            one.setRank("rank one");
            put("1", TaxonUtil.taxonToMap(one));

            TaxonImpl two = new TaxonImpl(null, "2");
            two.setRank("rank two");
            put("2", TaxonUtil.taxonToMap(two));
        }};

        Map<String, Map<String, String>> taxonMapDenormalized = new TreeMap<>();

        Map<String, String> childParent = new TreeMap<String, String>() {{
            put("1", "2");
        }};

        Map<String, String> taxonNames = new TreeMap<String, String>() {{
            put("1", "one name");
            put("2", "two name");
        }};


        NCBITaxonService.denormalizeTaxa(taxonMap, taxonMapDenormalized, childParent, taxonNames);

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
    public void parseNames() throws PropertyEnricherException {
        Map<String, String> nameMap = new TreeMap<>();

        InputStream namesStream = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/ncbi/names.dmp");

        NCBITaxonService.parseNames(nameMap, namesStream);

        assertThat(nameMap.size(), is(2));
        assertThat(nameMap.get("NCBI:1"), is("root"));
        assertThat(nameMap.get("NCBI:2"), is("Bacteria"));

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