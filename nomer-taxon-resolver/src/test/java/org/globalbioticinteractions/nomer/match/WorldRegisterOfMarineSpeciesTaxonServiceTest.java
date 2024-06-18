package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.PropertyEnricherSimple;
import org.globalbioticinteractions.nomer.cmd.OutputFormat;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class WorldRegisterOfMarineSpeciesTaxonServiceTest {

    @Rule

    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void enrichById() throws PropertyEnricherException {
        TaxonImpl taxon = new TaxonImpl(null, "WORMS:310843");
        Map<String, String> enriched = ((PropertyEnricherSimple) createService()).enrich(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("WORMS:158709"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Ariopsis felis"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalUrl(), is("https://www.marinespecies.org/aphia.php?p=taxdetails&id=158709"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getAuthorship(), is("(Linnaeus, 1766)"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Ariidae | Ariopsis | Ariopsis felis"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("WORMS:154659 | WORMS:158708 | WORMS:158709"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("family | genus | species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathAuthorships(), is("Bleeker, 1858 | Gill, 1861 | (Linnaeus, 1766)"));
    }

    @Test
    public void enrichById2() throws PropertyEnricherException {
        TaxonImpl taxon = new TaxonImpl(null, "WORMS:158709");
        Map<String, String> enriched = ((PropertyEnricherSimple) createService()).enrich(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("WORMS:158709"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Ariopsis felis"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalUrl(), is("https://www.marinespecies.org/aphia.php?p=taxdetails&id=158709"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getAuthorship(), is("(Linnaeus, 1766)"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Ariidae | Ariopsis | Ariopsis felis"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("WORMS:154659 | WORMS:158708 | WORMS:158709"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("family | genus | species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathAuthorships(), is("Bleeker, 1858 | Gill, 1861 | (Linnaeus, 1766)"));
    }

    @Test
    public void enrichById3() throws PropertyEnricherException {
        TaxonImpl taxon = new TaxonImpl(null, "WORMS:1");
        Map<String, String> enriched = ((PropertyEnricherSimple) createService()).enrich(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("WORMS:1"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Biota"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalUrl(), is("https://www.marinespecies.org/aphia.php?p=taxdetails&id=1"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getAuthorship(), is(""));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is(""));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Biota"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("WORMS:1"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is(""));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathAuthorships(), is(""));
    }


    private WorldRegisterOfMarineSpeciesTaxonService createService(final String nameUrl) {
        return new WorldRegisterOfMarineSpeciesTaxonService(new TermMatcherContextClasspath() {
            @Override
            public String getCacheDir() {
                try {
                    return folder.newFolder().getAbsolutePath();
                } catch (IOException e) {
                    throw new RuntimeException("boom!", e);
                }
            }

            @Override
            public OutputFormat getOutputFormat() {
                return null;
            }

            @Override
            public String getProperty(String key) {
                return new TreeMap<String, String>() {
                    {
                        put("nomer.worms.url", nameUrl);
                    }
                }.get(key);
            }
        });
    }

    private WorldRegisterOfMarineSpeciesTaxonService createService() {
        return createService("/org/globalbioticinteractions/nomer/match/worms/catfish.json");
    }



}