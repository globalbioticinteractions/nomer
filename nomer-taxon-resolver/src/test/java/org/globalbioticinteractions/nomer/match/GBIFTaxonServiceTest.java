package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class GBIFTaxonServiceTest {

    @Test
    public void enrichById() throws PropertyEnricherException {
        GBIFTaxonService service = createService();

        String externalId = "GBIF:3220631";
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(null, externalId)));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Desulfobacter vibrioformis"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("GBIF:3220631"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Desulfobacter vibrioformis"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("SPECIES"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("GBIF:3220631"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("SPECIES"));
    }

    @Test
    public void enrichByName() throws PropertyEnricherException {
        GBIFTaxonService service = createService();

        String name = "Desulfobacter vibrioformis";
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(name, null)));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Desulfobacter vibrioformis"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("GBIF:3220631"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Desulfobacter vibrioformis"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("SPECIES"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("GBIF:3220631"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("SPECIES"));
    }

    @Test
    public void enrichBySynonym() throws PropertyEnricherException {
        GBIFTaxonService service = createService();

        String externalId = "GBIF:3220667";
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(null, externalId)));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Desulfomusa hansenii"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("GBIF:3220667"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Desulfomusa hansenii"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("SPECIES"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("GBIF:3220667"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("SPECIES"));
    }

    private GBIFTaxonService createService() {
        return new GBIFTaxonService(new TermMatcherContext() {
            @Override
            public String getCacheDir() {
                return new File("target/gbifCache" + UUID.randomUUID()).getAbsolutePath();
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
                        put("nomer.gbif.taxon", "/org/globalbioticinteractions/nomer/match/gbif/backbone-current-simple.txt");
                    }
                }.get(key);
            }
        });
    }



}