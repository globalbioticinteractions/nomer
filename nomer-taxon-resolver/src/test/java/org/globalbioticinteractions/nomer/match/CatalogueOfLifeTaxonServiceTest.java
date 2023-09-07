package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.globalbioticinteractions.nomer.cmd.OutputFormat;
import org.junit.Test;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CatalogueOfLifeTaxonServiceTest {

    @Test
    public void enrichById() throws PropertyEnricherException {
        assertEnrichById(createService());
    }

    @Test
    public void enrichByIdReverseSorted() throws PropertyEnricherException {
        CatalogueOfLifeTaxonService service = createService(
                "/org/globalbioticinteractions/nomer/match/" + getTestSetName() + "/NameUsageReverseSorted.tsv",
                "/org/globalbioticinteractions/nomer/match/" + getTestSetName() + "/metadata.yaml"
        );
        service.setReverseSorted(true);

        assertEnrichById(service);
    }

    public void assertEnrichById(CatalogueOfLifeTaxonService service) throws PropertyEnricherException {
        TaxonImpl taxon = new TaxonImpl(null, "COL:9916:63MJH");
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("COL:9916:63MJH"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Phryganella"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("genus"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Phryganellidae | Phryganella"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("COL:9916:625ZT | COL:9916:63MJH"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("family | genus"));
    }

    @Test
    public void enrichByName() throws PropertyEnricherException {
        CatalogueOfLifeTaxonService service = createService();

        Taxon phryganella = new TaxonImpl("Phryganella", null);
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(phryganella));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("COL:9916:63MJH"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Phryganella"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("genus"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Phryganellidae | Phryganella"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("COL:9916:625ZT | COL:9916:63MJH"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("family | genus"));
    }

    @Test
    public void enrichByNameWitIgnoreSubgenus() throws PropertyEnricherException {
        CatalogueOfLifeTaxonService service = createService();

        Taxon phryganella = new TaxonImpl("Pteronotus macleayii", null);
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(phryganella));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("COL:9916:7WP8W"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Pteronotus macleayii"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Pteronotus | Chilonycteris | Pteronotus macleayii"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("COL:9916:74SW | COL:9916:8P3CB | COL:9916:7WP8W"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("genus | subgenus | species"));
    }

    @Test
    public void enrichBySynonymId() throws PropertyEnricherException {
        CatalogueOfLifeTaxonService service = createService();

        String externalId = "COL:9916:4BP2T";
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(null, externalId)));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("COL:9916:6TH9B"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Ozyptila yosemitica"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Ozyptila yosemitica"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getAuthorship(), is("Schick, 1965"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("COL:9916:6TH9B"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("species"));
    }

    @Test
    public void enrichBySynonymName() throws PropertyEnricherException {
        CatalogueOfLifeTaxonService service = createService();

        TaxonImpl taxon = new TaxonImpl("Ozyptila schusteri", null);
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("COL:9916:6TH9B"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Ozyptila yosemitica"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Ozyptila yosemitica"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getAuthorship(), is("Schick, 1965"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("COL:9916:6TH9B"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("species"));
    }

    private CatalogueOfLifeTaxonService createService() {
        return createService(
                "/org/globalbioticinteractions/nomer/match/" + getTestSetName() + "/NameUsage.tsv",
                "/org/globalbioticinteractions/nomer/match/" + getTestSetName() + "/metadata.yaml"
        );
    }

    protected String getTestSetName() {
        return "col";
    }

    private CatalogueOfLifeTaxonService createService(final String nameUrl, final String metaDataUrl) {
        return new CatalogueOfLifeTaxonService(new TermMatcherContextClasspath() {
            @Override
            public String getCacheDir() {
                return new File("target/colCache" + UUID.randomUUID()).getAbsolutePath();
            }

            @Override
            public OutputFormat getOutputFormat() {
                return null;
            }

            @Override
            public String getProperty(String key) {
                return new TreeMap<String, String>() {
                    {
                        put("nomer.col.name_usage.url", nameUrl);
                        put("nomer.col.metadata.url", metaDataUrl);
                    }
                }.get(key);
            }
        });
    }


}