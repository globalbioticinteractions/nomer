package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.TermMatchListener;
import org.globalbioticinteractions.nomer.cmd.OutputFormat;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertNotNull;

public class IRMNGTaxonServiceTest {

    @Test
    public void enrichById() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl(null, "IRMNG:4");
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));
        assertFungi(enriched);
    }

    private static void assertFungi(Map<String, String> enriched) {
        Taxon resolvedTaxon = TaxonUtil.mapToTaxon(enriched);
        assertThat(resolvedTaxon.getName(), is("Fungi"));
        assertThat(resolvedTaxon.getExternalId(), is("IRMNG:4"));
        assertThat(resolvedTaxon.getRank(), is("kingdom"));
        assertThat(resolvedTaxon.getAuthorship(), is(""));
        assertThat(resolvedTaxon.getPath(), is("Biota | Fungi"));
        assertThat(resolvedTaxon.getPathNames(), is(" | kingdom"));
        assertThat(resolvedTaxon.getPathIds(), is("IRMNG:1 | IRMNG:4"));
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
        Taxon resolvedTaxa = TaxonUtil.mapToTaxon(enriched);
        assertThat(resolvedTaxa.getName(), is("Plantae"));
        assertThat(resolvedTaxa.getExternalId(), is("IRMNG:3"));
        assertThat(resolvedTaxa.getRank(), is("kingdom"));
        assertThat(resolvedTaxa.getAuthorship(), is("Haeckel, 1866"));
        assertThat(resolvedTaxa.getPath(), is("Biota | Plantae"));
        assertThat(resolvedTaxa.getPathNames(), is(" | kingdom"));
        assertThat(resolvedTaxa.getPathIds(), is("IRMNG:1 | IRMNG:3"));
    }

    @Test
    public void enrichAcceptedByName() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl("Protista", null);
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));
        Taxon resolvedTaxon = TaxonUtil.mapToTaxon(enriched);
        assertThat(resolvedTaxon.getExternalId(), is("IRMNG:5"));
        assertThat(resolvedTaxon.getName(), is("Protozoa"));
        assertThat(resolvedTaxon.getRank(), is("kingdom"));
        assertThat(resolvedTaxon.getAuthorship(), is("Owen, 1858"));
        assertThat(resolvedTaxon.getPath(), is("Biota | Protozoa"));
        assertThat(resolvedTaxon.getPathNames(), is(" | kingdom"));
        assertThat(resolvedTaxon.getPathIds(), is("IRMNG:1 | IRMNG:5"));
        assertThat(resolvedTaxon.getStatus().getName(), is("HAS_ACCEPTED_NAME"));

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

    @Test
    public void matchUncertainName() throws PropertyEnricherException {
        File file = new File("target/cache" + UUID.randomUUID());
        IRMNGTaxonService service = new IRMNGTaxonService(new TermMatcherContext() {
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

        List<Taxon> matches = new ArrayList<>();
        service.match(Arrays.asList(new TermImpl("", "Louisellida")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                assertThat(nameType, is(NameType.HAS_UNCHECKED_NAME));
                matches.add(taxon);
            }
        });

        assertThat(matches.size(), is(1));
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