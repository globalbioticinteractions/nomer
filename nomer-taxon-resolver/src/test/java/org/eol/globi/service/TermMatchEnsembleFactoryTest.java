package org.eol.globi.service;

import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.taxon.TaxonEnricherImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class TermMatchEnsembleFactoryTest {

    private PropertyEnricher taxonEnricher;

    @Before
    public void init() {
        taxonEnricher = new TaxonEnricherImpl() {{
            setServices(TermMatchEnsembleFactory.getEnrichers(null));
        }};
    }

    @After
    public void shutdown() {
        taxonEnricher.shutdown();
    }

    @Test
    @Ignore
    public void zikaVirus() throws PropertyEnricherException {
        Taxon taxon = new TaxonImpl("Zika virus (ZIKV)", "NCBI:64320");
        final Map<String, String> enriched = taxonEnricher.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), containsString("Flaviviridae"));
    }

    @Test
    public void gbifLongUrl() throws PropertyEnricherException {
        Taxon taxon = new TaxonImpl("Mickey", "https://www.gbif.org/species/110462373");
        final Map<String, String> enriched = taxonEnricher.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), containsString("Calyptra thalictri"));
    }

}