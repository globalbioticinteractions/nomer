package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.Taxon;
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

public class WorldOfFloraOnlineTaxonServiceTest {

    @Test
    public void enrichById() throws PropertyEnricherException {
        assertEnrichById(createService());
    }

    public void assertEnrichById(PropertyEnricherSimple service) throws PropertyEnricherException {
        TaxonImpl taxon = new TaxonImpl(null, "WFO:0000000100");
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("WFO:0000000100"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Syneilesis palmata"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalUrl(), is("http://www.worldfloraonline.org/taxon/wfo-0000000100"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getAuthorship(), is("Maxim."));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Syneilesis | Syneilesis palmata"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("WFO:4000037295 | WFO:0000000100"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("genus | species"));
    }

    @Test
    public void enrichByName() throws PropertyEnricherException {
        WorldOfFloraOnlineTaxonService service = createService();

        Taxon phryganella = new TaxonImpl("Syneilesis palmata", null);
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(phryganella));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("WFO:0000000100"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Syneilesis palmata"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Syneilesis | Syneilesis palmata"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("WFO:4000037295 | WFO:0000000100"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("genus | species"));
    }

    @Test
    public void enrichBySynonymId() throws PropertyEnricherException {
        WorldOfFloraOnlineTaxonService service = createService();

        String externalId = "WFO:0000000001";
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(null, externalId)));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("WFO:0000027702"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Cirsium spinosissimum"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getAuthorship(), is("Scop."));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("WFO:0000027702"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("species"));
    }

    @Test
    public void enrichBySynonymName() throws PropertyEnricherException {
        WorldOfFloraOnlineTaxonService service = createService();

        TaxonImpl taxon = new TaxonImpl("Cirsium caput-medusae", null);
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("WFO:0000027702"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Cirsium spinosissimum"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getAuthorship(), is("Scop."));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Cirsium spinosissimum"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("WFO:0000027702"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("species"));
    }

    private WorldOfFloraOnlineTaxonService createService() {
        return createService("/org/globalbioticinteractions/nomer/match/wfo/classification.txt");
    }

    private WorldOfFloraOnlineTaxonService createService(final String nameUrl) {
        return new WorldOfFloraOnlineTaxonService(new TermMatcherContextClasspath() {
            @Override
            public String getCacheDir() {
                return new File("target/wfoCache" + UUID.randomUUID()).getAbsolutePath();
            }

            @Override
            public OutputFormat getOutputFormat() {
                return null;
            }

            @Override
            public String getProperty(String key) {
                return new TreeMap<String, String>() {
                    {
                        put("nomer.wfo.classification.url", nameUrl);
                    }
                }.get(key);
            }
        });
    }


}