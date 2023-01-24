package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.globalbioticinteractions.nomer.cmd.OutputFormat;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

public class MDDTaxonServiceTest {

    @Test
    public void enrichByName() throws PropertyEnricherException {
        PropertyEnricher service = createService();
        TaxonImpl taxon = new TaxonImpl("Rhinolophus sinicus");
        assertRhinolophusSinicus(service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon)));
    }

    @Test
    public void enrichByOriginalName() throws PropertyEnricherException {
        PropertyEnricher service = createService();
        TaxonImpl taxon = new TaxonImpl("Rhinolophus rouxi sinicus");
        assertRhinolophusSinicus(service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon)));
    }

    private void assertRhinolophusSinicus(Map<String, String> enriched) {
        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enriched);
        assertThat(enrichedTaxon.getName(), is("Rhinolophus sinicus"));
        assertThat(enrichedTaxon.getId(), is("https://www.mammaldiversity.org/explore.html#genus=Rhinolophus&species=sinicus&id=1004746"));
        assertThat(enrichedTaxon.getExternalId(), is("https://www.mammaldiversity.org/explore.html#genus=Rhinolophus&species=sinicus&id=1004746"));
        assertThat(enrichedTaxon.getExternalUrl(), is("https://www.mammaldiversity.org/explore.html#genus=Rhinolophus&species=sinicus&id=1004746"));
        assertThat(enrichedTaxon.getAuthorship(), is("K. Andersen, 1905"));
        assertThat(enrichedTaxon.getPath(), is("Theria | Placentalia | Boreoeutheria | Laurasiatheria | Chiroptera | Pteropodiformes |  |  | Rhinolophoidea | Rhinolophidae |  |  | Rhinolophus |  | sinicus"));
        assertThat(enrichedTaxon.getPathNames(), is("subclass | infraclass | magnorder | superorder | order | suborder | infraorder | parvorder | superfamily | family | subfamily | tribe | genus | subgenus | specificEpithet"));
    }

    @Test
    public void enrichBySubspeciesName() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl("Rhinolophus sinicus sinicus");
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));

        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enriched);
        assertThat(enrichedTaxon.getName(), is("Rhinolophus sinicus sinicus"));
        assertThat(enrichedTaxon.getExternalUrl(), is(nullValue()));
    }

    @Test
    public void enrichNameIgnoringUnsupportedId() throws PropertyEnricherException {
        PropertyEnricher service = createService();
        TaxonImpl taxon = new TaxonImpl("Rhinolophus sinicus", "https://batnames.org/species/Rhinolophus%20sinicus");
        assertRhinolophusSinicus(service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon)));
    }

    @Test
    public void enrichBySubspeciesNameAltAuthorship() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl("Rhinolophus sinicus septentrionalis");
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));

        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enriched);
        assertThat(enrichedTaxon.getName(), is("Rhinolophus sinicus septentrionalis"));
        assertThat(enrichedTaxon.getExternalUrl(), is("https://www.mammaldiversity.org/explore.html#genus=Rhinolophus&species=sinicus&id=1004746&subspecies=septentrionalis"));
        assertThat(enrichedTaxon.getAuthorship(), is("Sanborn, 1939"));
        assertThat(enrichedTaxon.getPath(), is("Theria | Placentalia | Boreoeutheria | Laurasiatheria | Chiroptera | Pteropodiformes |  |  | Rhinolophoidea | Rhinolophidae |  |  | Rhinolophus |  | sinicus | septentrionalis"));
        assertThat(enrichedTaxon.getPathNames(), is("subclass | infraclass | magnorder | superorder | order | suborder | infraorder | parvorder | superfamily | family | subfamily | tribe | genus | subgenus | specificEpithet | subspecificEpithet"));
    }


    private MDDTaxonService createService() {
        File file = new File("target/cache" + UUID.randomUUID());
        return new MDDTaxonService(new TermMatcherContext() {
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
            public OutputFormat getOutputFormat() {
                return null;
            }

            @Override
            public String getProperty(String key) {
                return new TreeMap<String, String>() {
                    {
                        put("nomer.mdd.url", "/org/globalbioticinteractions/nomer/match/mdd/mdd-short.csv");
                    }
                }.get(key);
            }
        });
    }


}