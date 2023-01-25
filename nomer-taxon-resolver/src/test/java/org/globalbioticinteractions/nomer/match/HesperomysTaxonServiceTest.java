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

public class HesperomysTaxonServiceTest {

    @Test
    public void enrichByName() throws PropertyEnricherException {
        PropertyEnricher service = createService();
        TaxonImpl taxon = new TaxonImpl("Rhinolophus sinicus sinicus");
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
        assertThat(enrichedTaxon.getName(), is("Rhinolophus sinicus sinicus"));
        assertThat(enrichedTaxon.getId(), is("HES:20586"));
        assertThat(enrichedTaxon.getExternalId(), is("HES:20586"));
        assertThat(enrichedTaxon.getExternalUrl(), is("http://hesperomys.com/n/20586"));
        assertThat(enrichedTaxon.getAuthorship(), is("Andersen, 1905"));
        assertThat(enrichedTaxon.getPath(), is("Mammalia | Chiroptera | Rhinolophidae | Rhinolophus | sinicus | sinicus"));
        assertThat(enrichedTaxon.getPathNames(), is("class | order | family | genus | specificEpithet | subspecificEpithet"));
    }

    @Test
    public void enrichNameIgnoringUnsupportedId() throws PropertyEnricherException {
        PropertyEnricher service = createService();
        TaxonImpl taxon = new TaxonImpl("Rhinolophus sinicus sinicus", "https://batnames.org/species/Rhinolophus%20sinicus");
        assertRhinolophusSinicus(service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon)));
    }

    @Test
    public void enrichBySubspeciesNameAltAuthorship() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl("Rhinolophus sinicus septentrionalis");
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));

        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enriched);
        assertThat(enrichedTaxon.getName(), is("Rhinolophus sinicus septentrionalis"));
        assertThat(enrichedTaxon.getRank(), is("subspecies"));
        assertThat(enrichedTaxon.getExternalId(), is("HES:20587"));
        assertThat(enrichedTaxon.getExternalUrl(), is("http://hesperomys.com/n/20587"));
        assertThat(enrichedTaxon.getAuthorship(), is("Sanborn, 1939"));
        assertThat(enrichedTaxon.getPath(), is("Mammalia | Chiroptera | Rhinolophidae | Rhinolophus | sinicus | septentrionalis"));
        assertThat(enrichedTaxon.getPathNames(), is("class | order | family | genus | specificEpithet | subspecificEpithet"));
    }


    private HesperomysTaxonService createService() {
        File file = new File("target/cache" + UUID.randomUUID());
        return new HesperomysTaxonService(new TermMatcherContext() {
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
                        put("nomer.hesperomys.url", "/org/globalbioticinteractions/nomer/match/hesperomys/mammals-short.csv");
                    }
                }.get(key);
            }
        });
    }


}