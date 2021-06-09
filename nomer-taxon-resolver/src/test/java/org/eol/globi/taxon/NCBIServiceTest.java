package org.eol.globi.taxon;

import org.apache.commons.io.IOUtils;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class NCBIServiceTest {

    @Test
    public void lookupPathByTaxonId() throws PropertyEnricherException {
        PropertyEnricher enricher = new NCBIService();
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, "NCBI:9606");
        }};
        Map<String, String> enrich = enricher.enrichFirstMatch(props);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("NCBI:9606"));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Homo sapiens"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("cellular organisms | Eukaryota | Opisthokonta | Metazoa | Eumetazoa | Bilateria | Deuterostomia | Chordata | Craniata | Vertebrata | Gnathostomata | Teleostomi | Euteleostomi | Sarcopterygii | Dipnotetrapodomorpha | Tetrapoda | Amniota | Mammalia | Theria | Eutheria | Boreoeutheria | Euarchontoglires | Primates | Haplorrhini | Simiiformes | Catarrhini | Hominoidea | Hominidae | Homininae | Homo | Homo sapiens"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_IDS), is("NCBI:131567 | NCBI:2759 | NCBI:33154 | NCBI:33208 | NCBI:6072 | NCBI:33213 | NCBI:33511 | NCBI:7711 | NCBI:89593 | NCBI:7742 | NCBI:7776 | NCBI:117570 | NCBI:117571 | NCBI:8287 | NCBI:1338369 | NCBI:32523 | NCBI:32524 | NCBI:40674 | NCBI:32525 | NCBI:9347 | NCBI:1437010 | NCBI:314146 | NCBI:9443 | NCBI:376913 | NCBI:314293 | NCBI:9526 | NCBI:314295 | NCBI:9604 | NCBI:207598 | NCBI:9605 | NCBI:9606"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is(" | superkingdom | clade | kingdom | clade | clade | clade | phylum | subphylum | clade | clade | clade | clade | superclass | clade | clade | clade | class | clade | clade | clade | superorder | order | suborder | infraorder | parvorder | superfamily | family | subfamily | genus | species"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("species"));
        assertThat(enrich.get(PropertyAndValueDictionary.COMMON_NAMES), is("man @en | human @en"));
    }

    @Test
    public void lookupPathByTaxonIdPrefix() throws PropertyEnricherException {
        PropertyEnricher enricher = new NCBIService();
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, "NCBI:txid9606");
        }};
        Map<String, String> enrich = enricher.enrichFirstMatch(props);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("NCBI:9606"));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Homo sapiens"));
    }

    @Test
    public void lookupPathByTaxonIdNCBITaxon() throws PropertyEnricherException {
        PropertyEnricher enricher = new NCBIService();
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, "NCBITaxon:9606");
        }};
        Map<String, String> enrich = enricher.enrichFirstMatch(props);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Homo sapiens"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("NCBI:9606"));
    }

    @Test
    public void lookupPathByTaxonIdNCBIPurl() throws PropertyEnricherException {
        PropertyEnricher enricher = new NCBIService();
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, "http://purl.obolibrary.org/obo/NCBITaxon_9606");
        }};
        Map<String, String> enrich = enricher.enrichFirstMatch(props);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Homo sapiens"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("NCBI:9606"));
    }

    @Test
    public void lookupPathByTaxonIdNCBIPurlWhitespaces() throws PropertyEnricherException {
        PropertyEnricher enricher = new NCBIService();
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, "http://purl.obolibrary.org/obo/NCBITaxon_9606 ");
        }};
        Map<String, String> enrich = enricher.enrichFirstMatch(props);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Homo sapiens"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("NCBI:9606"));
    }


    @Test
    public void parseInconsistentWithAltNCBI191217() throws PropertyEnricherException {
        PropertyEnricher enricher = new NCBIService();
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, "NCBI:191217");
        }};
        Map<String, String> enrich = enricher.enrichFirstMatch(props);
        Taxon taxon = TaxonUtil.mapToTaxon(enrich);
        int expectedLength = 9;
        assertThat(taxon.getPath().split("\\|").length, is(expectedLength));
        assertThat(taxon.getPathNames().split("\\|").length, is(expectedLength));
        assertThat(taxon.getPathIds().split("\\|").length, is(expectedLength));
        assertThat(taxon.getPathIds(), containsString("NCBI:2170100"));
    }

    @Test
    public void parseInconsistentPathWithAltNCBI191217() throws IOException, PropertyEnricherException {
        String data = IOUtils.toString(getClass().getResourceAsStream("ncbi191217.xml"), StandardCharsets.UTF_8);
        HashMap<String, String> enriched = new HashMap<>();
        new NCBIService().parseAndPopulate(enriched, "bla", data);

        Taxon taxon = TaxonUtil.mapToTaxon(enriched);
        int expectedLength = 6;
        assertThat(taxon.getPath().split("\\|").length, is(expectedLength));
        assertThat(taxon.getPathNames().split("\\|").length, is(expectedLength));
        assertThat(taxon.getPathIds().split("\\|").length, is(expectedLength));
        assertThat(taxon.getPathIds(), containsString("NCBI:2170100"));
    }

    @Ignore(value = "2018-06-11 ncbi taxon query fails with 503")
    @Test
    public void lookupPathByTaxonId2() throws PropertyEnricherException {
        PropertyEnricher enricher = new NCBIService();
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, "NCBI:54642");
        }};
        Map<String, String> enrich = enricher.enrichFirstMatch(props);
        String expectedPathNames = " | superkingdom |  | kingdom |  |  |  |  |  | phylum |  |  | subphylum | class |  | subclass | infraclass | cohort | order | suborder | infraorder | superfamily | family | genus | species";
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is(expectedPathNames));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("NCBI:54642"));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Parantonae hispida"));
    }


    @Test
    public void lookupPathByNonNumericTaxonId() throws PropertyEnricherException {
        PropertyEnricher enricher = new NCBIService();
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, "NCBI:donaldduck");
        }};
        Map<String, String> enrich = enricher.enrichFirstMatch(props);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("NCBI:donaldduck"));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is(nullValue()));
    }

    @Test
    public void lookupPathByNonExistentTaxonId() throws PropertyEnricherException {
        PropertyEnricher enricher = new NCBIService();
        final String nonExistentId = "NCBI:11111111";
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, nonExistentId);
        }};
        Map<String, String> enrich = enricher.enrichFirstMatch(props);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is(nonExistentId));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is(nullValue()));
    }

    @Test(expected = PropertyEnricherException.class)
    public void lookupPathByInvalidTaxonId() throws PropertyEnricherException {
        PropertyEnricher enricher = new NCBIService();
        final String nonExistentId = "NCBI:111111111111111111111111111";
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, nonExistentId);
        }};
        enricher.enrichFirstMatch(props);
    }

    @Test
    public void lookupPathByName() throws PropertyEnricherException {
        PropertyEnricher enricher = new NCBIService();
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.NAME, "Homo sapiens");
        }};
        Map<String, String> enrich = enricher.enrichFirstMatch(props);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is(nullValue()));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Homo sapiens"));
    }

    @Test
    public void lookupPathByPreviouslyUnmatchedId() throws PropertyEnricherException {
        PropertyEnricher enricher = new NCBIService();
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, "NCBI:235106");
        }};
        Map<String, String> enrich = enricher.enrichFirstMatch(props);
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Influenza A virus (A/Taiwan/0562/1995(H1N1))"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), containsString("Influenza A virus"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("NCBI:235106"));
    }


}
