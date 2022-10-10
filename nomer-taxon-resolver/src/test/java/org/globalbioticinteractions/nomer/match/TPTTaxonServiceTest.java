package org.globalbioticinteractions.nomer.match;

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

public class TPTTaxonServiceTest {

    @Test
    public void enrichById() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl(null, "GBIF:10766101");
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));
        assertGardineri(enriched);
    }

    private void assertGardineri(Map<String, String> enriched) {
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Animalia | Arthropoda | Arachnida | Holothyrida | Holothyridae | Dicrogonatus | gardineri"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("kingdom | phylum | class | order | family | genus | specificEpithet"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("GBIF:acari_10766101"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Dicrogonatus gardineri"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getAuthorship(), is("(Warburton, 1912)"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
    }

    @Test
    public void enrichByName() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl("Dicrogonatus gardineri", null);
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));

        assertGardineri(enriched);
    }

    @Test
    public void enrichSynonym() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl(null, "GBIF:6892348");
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("GBIF:acari_6892347"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getAuthorship(), is("(Berlese, 1923)"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Haplothyrus expolitissimus"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Animalia | Arthropoda | Arachnida | Holothyrida | Holothyridae | Haplothyrus | expolitissimus"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("kingdom | phylum | class | order | family | genus | specificEpithet"));
    }

    @Test
    public void enrichNoMatch() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        Map<String, String> noMatch = TaxonUtil.taxonToMap(new TaxonImpl(null, "GBIF:999999999"));
        Map<String, String> enriched = service.enrichFirstMatch(noMatch);

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is(nullValue()));
    }

    @Test
    public void enrichPrefixMismatch() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        Map<String, String> prefixMismatch = TaxonUtil.taxonToMap(new TaxonImpl(null, "FOO:2"));
        Map<String, String> enriched = service.enrichFirstMatch(prefixMismatch);

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is(nullValue()));
    }

    private TPTTaxonService createService() {
        File file = new File("target/cache" + UUID.randomUUID());
        return new TPTTaxonService(new TermMatcherContext() {
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
                        put("nomer.tpt.taxon", "/org/globalbioticinteractions/nomer/match/tpt/acari.csv");
                    }
                }.get(key);
            }
        });
    }

}