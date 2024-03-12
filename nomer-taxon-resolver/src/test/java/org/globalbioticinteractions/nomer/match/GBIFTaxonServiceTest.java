package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.PropertyEnricherSimple;
import org.globalbioticinteractions.nomer.cmd.OutputFormat;
import org.junit.Test;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class GBIFTaxonServiceTest {


    @Test
    public void enrichById() throws PropertyEnricherException {
        PropertyEnricherSimple service = createService();

        String externalId = "GBIF:3220631";
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(null, externalId)));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Desulfobacter vibrioformis"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("GBIF:3220631"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Desulfobacter vibrioformis"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getAuthorship(), is("Lien & Beeder, 1997"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("GBIF:3220631"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("species"));
    }

    @Test
    public void enrichByName() throws PropertyEnricherException {
        PropertyEnricherSimple service = createService();

        String name = "Desulfofaba hansenii";
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(name, null)));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Desulfofaba hansenii"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("GBIF:3220666"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Desulfofaba hansenii"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("GBIF:3220666"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("species"));
    }

    @Test
    public void enrichBySynonymById() throws PropertyEnricherException {
        PropertyEnricherSimple service = createService();

        String externalId = "GBIF:3220667";
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(null, externalId)));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Desulfofaba hansenii"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("GBIF:3220666"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Desulfofaba hansenii"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("GBIF:3220666"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
    }

    @Test
    public void enrichBySynonymByName() throws PropertyEnricherException {
        PropertyEnricherSimple service = createService();

        String name = "Desulfomusa hansenii";
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(name, null)));

        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Desulfofaba hansenii"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("GBIF:3220666"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Desulfofaba hansenii"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("GBIF:3220666"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("species"));
    }

    private PropertyEnricherSimple createService() {
        return createService(
                "/org/globalbioticinteractions/nomer/match/gbif/backbone-current-simple.txt",
                "/org/globalbioticinteractions/nomer/match/gbif/backbone-current-name-id-sorted.txt"
        );
    }

    protected PropertyEnricherSimple createService(final String idsUrl, final String namesUrl) {
        return new GBIFTaxonService(new TermMatcherContextClasspath() {
            @Override
            public String getCacheDir() {
                return new File("target/gbifCache" + UUID.randomUUID()).getAbsolutePath();
            }

            @Override
            public OutputFormat getOutputFormat() {
                return null;
            }

            @Override
            public String getProperty(String key) {
                return new TreeMap<String, String>() {
                    {
                        put("nomer.gbif.ids", idsUrl);
                        put("nomer.gbif.names", namesUrl);
                    }
                }.get(key);
            }
        });
    }


}