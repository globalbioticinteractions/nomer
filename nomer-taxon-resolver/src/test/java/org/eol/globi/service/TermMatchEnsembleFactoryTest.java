package org.eol.globi.service;

import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.taxon.TaxonEnricherImpl;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class TermMatchEnsembleFactoryTest {

    private PropertyEnricher taxonEnricher;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @After
    public void shutdown() {
        if (taxonEnricher != null) {
            taxonEnricher.shutdown();
        }
    }

    @Test
    @Ignore
    public void zikaVirus() throws PropertyEnricherException {
        Taxon taxon = new TaxonImpl("Zika virus (ZIKV)", "NCBI:64320");
        taxonEnricher = new TaxonEnricherImpl() {{
            setServices(TermMatchEnsembleFactory.getEnrichers(null));
        }};
        final Map<String, String> enriched = taxonEnricher.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), containsString("Flaviviridae"));
    }

    @Test
    public void gbifLongUrl() throws PropertyEnricherException, IOException {

         final String cachePath = folder.newFolder("cacheDir").getAbsolutePath();

        Taxon taxon = new TaxonImpl("Mickey", "https://www.gbif.org/species/1777631");
        taxonEnricher = new TaxonEnricherImpl() {{
            setServices(TermMatchEnsembleFactory.getEnrichers(new TermMatcherContext() {
                @Override
                public String getCacheDir() {
                    return cachePath;
                }

                @Override
                public InputStream getResource(String uri) throws IOException {
                    return null;
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
                public String getProperty(String key) {
                    return null;
                }
            }));
        }};

        final Map<String, String> enriched = taxonEnricher.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), containsString("Calyptra thalictri"));
    }

}