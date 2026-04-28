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

public class IRMNGTaxonServiceTest {

    @Test
    public void enrichById() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl(null, "IRMNG:4");
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));
        assertFungi(enriched);
    }

    private static void assertFungi(Map<String, String> enriched) {
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Fungi"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("IRMNG:4"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("kingdom"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getAuthorship(), is(""));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Biota | Fungi"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is(" | kingdom"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("IRMNG:1 | IRMNG:4"));
    }

    @Test
    public void enrichByName() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl("Fungi", null);
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));
        assertFungi(enriched);
    }

    @Test
    public void enrichByName2() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl("Plantae", null);
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Plantae"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("IRMNG:3"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("kingdom"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getAuthorship(), is("Haeckel, 1866"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Biota | Plantae"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is(" | kingdom"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("IRMNG:1 | IRMNG:3"));
    }

    @Test
    public void enrichAcceptedByName() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl("Protista", null);
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("IRMNG:5"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Protozoa"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("kingdom"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getAuthorship(), is("Owen, 1858"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Biota | Protozoa"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is(" | kingdom"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("IRMNG:1 | IRMNG:5"));
    }


    @Test
    public void enrichNoMatch() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(new TaxonImpl(null, "IRMNG:999999999")));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is(nullValue()));
    }

    @Test
    public void enrichPrefixMismatch() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(new TaxonImpl(null, "FOO:2")));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is(nullValue()));
    }

    private IRMNGTaxonService createService() {
        File file = new File("target/cache" + UUID.randomUUID());
        return new IRMNGTaxonService(new TermMatcherContext() {
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
                        put("nomer.irmng.taxa", "/org/globalbioticinteractions/nomer/match/irmng/taxa.txt");
                    }
                }.get(key);
            }
        });
    }

}