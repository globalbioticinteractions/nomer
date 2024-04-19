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

public class PBDBTaxonServiceTest {

    @Test
    public void enrichById() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl(null, "PBDB:83088");
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));
        assertTruncatedHomoSapiens(enriched);
    }

    private void assertTruncatedHomoSapiens(Map<String, String> enriched) {
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Homo sapiens"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("PBDB:83088"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getAuthorship(), is("C. Linnaeus 1758"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Hominini | Homo | Homo sapiens"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("PBDB:91486 | PBDB:40901 | PBDB:83088"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("tribe | genus | species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathAuthorships(), is(" | C. Linnaeus 1758 | C. Linnaeus 1758"));
    }

    @Test
    public void enrichByName() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl("Homo sapiens", null);
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));
        assertTruncatedHomoSapiens(enriched);
    }

    @Test
    public void enrichMergedId() throws PropertyEnricherException {
        PropertyEnricher service = createService();
        TaxonImpl taxon = new TaxonImpl(null, "PBDB:83072");
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));
        assertGenusHomo(enriched);
    }

    @Test
    public void enrichMergedName() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl("Protanthropus");
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));

        assertGenusHomo(enriched);
    }

    private void assertGenusHomo(Map<String, String> enriched) {
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("PBDB:40901"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Homo"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("genus"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("PBDB:91486 | PBDB:40901"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Hominini | Homo"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("tribe | genus"));
    }

    @Test
    public void enrichNoMatch() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(new TaxonImpl(null, "PBDB:999999999")));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is(nullValue()));
    }

    @Test
    public void enrichPrefixMismatch() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(new TaxonImpl(null, "FOO:2")));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is(nullValue()));
    }

    private PBDBTaxonService createService() {
        File file = new File("target/cache" + UUID.randomUUID());
        return new PBDBTaxonService(new TermMatcherContext() {
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
                        put("nomer.pbdb.taxa", "/org/globalbioticinteractions/nomer/match/pbdb/taxa.tsv");
                        put("nomer.pbdb.refs", "/org/globalbioticinteractions/nomer/match/pbdb/refs.tsv");
                    }
                }.get(key);
            }
        });
    }

}