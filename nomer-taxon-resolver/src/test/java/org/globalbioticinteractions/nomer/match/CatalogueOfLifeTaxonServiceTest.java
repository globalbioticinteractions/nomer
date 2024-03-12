package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
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
import static org.hamcrest.core.IsNull.nullValue;

public class CatalogueOfLifeTaxonServiceTest {

    @Test
    public void enrichById() throws PropertyEnricherException {
        TaxonImpl taxon = new TaxonImpl(null, "COL:63MJH");
        Map<String, String> enriched = createService().enrich(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("COL:63MJH"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Phryganella"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("genus"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Phryganellidae | Phryganella"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("COL:625ZT | COL:63MJH"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("family | genus"));
    }

    @Test
    public void enrichByIdReverseSorted() throws PropertyEnricherException {
        CatalogueOfLifeTaxonService service = createService(
                "/org/globalbioticinteractions/nomer/match/" + getTestSetName() + "/NameUsageReverseSorted.tsv",
                "/org/globalbioticinteractions/nomer/match/" + getTestSetName() + "/metadata.yaml"
        );
        service.setReverseSorted(true);

        TaxonImpl taxon = new TaxonImpl(null, "COL:63MJH");
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("COL:63MJH"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Phryganella"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("genus"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Phryganellidae | Phryganella"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("COL:625ZT | COL:63MJH"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("family | genus"));
    }

    @Test
    public void enrichByIdReverseSorted2() throws PropertyEnricherException {
        CatalogueOfLifeTaxonService service = createService(
                "/org/globalbioticinteractions/nomer/match/" + getTestSetName() + "/NameUsageReverseSorted2.tsv",
                "/org/globalbioticinteractions/nomer/match/" + getTestSetName() + "/metadata.yaml"
        );
        service.setReverseSorted(true);

        TaxonImpl taxon = new TaxonImpl(null, "COL:339SL");
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("COL:3S9VJ"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Lappula stricta"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Lappula stricta"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("COL:3S9VJ"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("species"));
    }

    @Test
    public void enrichByIdNotReverseSorted3() throws PropertyEnricherException {
        CatalogueOfLifeTaxonService service = init3();
        service.setReverseSorted(false);

        assertSorted3(service);
    }

    private CatalogueOfLifeTaxonService init3() {
        return createService(
                    "/org/globalbioticinteractions/nomer/match/" + getTestSetName() + "/NameUsageReverseSorted3.tsv",
                    "/org/globalbioticinteractions/nomer/match/" + getTestSetName() + "/metadata.yaml"
            );
    }

    @Test
    public void enrichByIdReverseSorted3() throws PropertyEnricherException {
        CatalogueOfLifeTaxonService service = init3();
        service.setReverseSorted(true);
        assertSorted3(service);
    }

    private void assertSorted3(CatalogueOfLifeTaxonService service) throws PropertyEnricherException {
        TaxonImpl taxon = new TaxonImpl(null, "COL:faf67be9-dc50-4543-85dc-5c6488c26feb");
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("COL:faf67be9-dc50-4543-85dc-5c6488c26feb"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("subgenus"));

        taxon = new TaxonImpl(null, "COL:4HLSL");
        enriched = service.enrich(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("COL:4HLSL"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Phytoecia alinae"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
    }

    @Test
    public void enrichByIdReverseSortedSearchByName() throws PropertyEnricherException {
        CatalogueOfLifeTaxonService service = createService(
                "/org/globalbioticinteractions/nomer/match/" + getTestSetName() + "/NameUsageReverseSorted.tsv",
                "/org/globalbioticinteractions/nomer/match/" + getTestSetName() + "/metadata.yaml"
        );
        service.setReverseSorted(true);

        Taxon phryganella = new TaxonImpl("Pteronotus macleayii", null);
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(phryganella));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("COL:7WP8W"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Pteronotus macleayii"));
    }

    public void assertEnrichById(CatalogueOfLifeTaxonService service) throws PropertyEnricherException {
        TaxonImpl taxon = new TaxonImpl(null, "COL:63MJH");
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("COL:63MJH"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Phryganella"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("genus"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Phryganellidae | Phryganella"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("COL:625ZT | COL:63MJH"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("family | genus"));
    }

    @Test
    public void enrichByName() throws PropertyEnricherException {
        CatalogueOfLifeTaxonService service = createService();

        Taxon phryganella = new TaxonImpl("Phryganella", null);
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(phryganella));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("COL:63MJH"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Phryganella"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("genus"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Phryganellidae | Phryganella"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("COL:625ZT | COL:63MJH"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("family | genus"));
    }

    @Test
    public void enrichByNameWitIgnoreSubgenus() throws PropertyEnricherException {
        CatalogueOfLifeTaxonService service = createService();

        Taxon phryganella = new TaxonImpl("Pteronotus macleayii", null);
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(phryganella));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("COL:7WP8W"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Pteronotus macleayii"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Pteronotus | Chilonycteris | Pteronotus macleayii"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("COL:74SW | COL:8P3CB | COL:7WP8W"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("genus | subgenus | species"));
    }

    @Test
    public void enrichBySynonymId() throws PropertyEnricherException {
        CatalogueOfLifeTaxonService service = createService();

        String externalId = "COL:4BP2T";
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(null, externalId)));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("COL:6TH9B"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Ozyptila yosemitica"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Ozyptila yosemitica"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getAuthorship(), is("Schick, 1965"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("COL:6TH9B"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("species"));
    }

    @Test
    public void enrichBySynonymName() throws PropertyEnricherException {
        CatalogueOfLifeTaxonService service = createService();

        TaxonImpl taxon = new TaxonImpl("Ozyptila schusteri", null);
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("COL:6TH9B"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Ozyptila yosemitica"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Ozyptila yosemitica"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getAuthorship(), is("Schick, 1965"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("COL:6TH9B"));
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

    @Test
    public void matchingIdProvider() {
        CatalogueOfLifeTaxonService service = createService();

        String idOrNull = service.getIdOrNull(new TaxonImpl("Donald duckus", "COL:36427"), TaxonomyProvider.CATALOGUE_OF_LIFE);

        assertThat(idOrNull, is("36427"));
    }


}