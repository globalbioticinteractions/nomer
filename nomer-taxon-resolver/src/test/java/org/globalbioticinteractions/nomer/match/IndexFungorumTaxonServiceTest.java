package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

public class IndexFungorumTaxonServiceTest {

    @Test
    public void enrichById() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl(null, "IF:808518");
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));

        assertIF808518(enriched);
    }

    private void assertIF808518(Map<String, String> enriched) {
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("IF:808518"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Leucocybe candicans"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is(nullValue()));
        assertThat(TaxonUtil.mapToTaxon(enriched).getAuthorship(), is("(Pers.) Vizzini, P. Alvarado, G. Moreno & Consiglio, 2015"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Fungi | Basidiomycota | Agaricomycotina | Agaricomycetes | Agaricomycetidae | Agaricales | Incertae sedis"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("kingdom | phylum | subphylum | class | subclass | order | family"));
    }

    private void assertIF177054(Map<String, String> enriched) {
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("IF:177054"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Clitocybe candicans"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is(nullValue()));
        assertThat(TaxonUtil.mapToTaxon(enriched).getAuthorship(), is("(Pers.) P. Kumm., 1871"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Fungi | Basidiomycota | Agaricomycotina | Agaricomycetes | Agaricomycetidae | Agaricales | Incertae sedis"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("kingdom | phylum | subphylum | class | subclass | order | family"));
    }

    @Test
    public void enrichByName() throws PropertyEnricherException {
        PropertyEnricher service = createService();
        TaxonImpl taxon = new TaxonImpl("Clitocybe candicans", null);
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));
        assertIF808518(enriched);
    }

    @Test
    public void acceptedName() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl(null, "IF:177054");
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));

        assertIF808518(enriched);
    }

    @Test
    public void enrichNoMatch() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        Map<String, String> unknownTaxon = TaxonUtil.taxonToMap(new TaxonImpl(null, "IF:999999999"));
        Map<String, String> enriched = service.enrichFirstMatch(unknownTaxon);

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is(nullValue()));
    }

    @Test
    public void enrichPrefixMismatch() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(new TaxonImpl(null, "FOO:177054")));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is(nullValue()));
    }

    private PropertyEnricher createService() {
        File file = new File("target/cache" + UUID.randomUUID());
        return new IndexFungorumTaxonService(new TermMatcherContext() {
            @Override
            public String getCacheDir() {
                return file.getAbsolutePath();
            }

            @Override
            public InputStream getResource(String uri) throws IOException {
                return getClass().getResourceAsStream(uri);
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
                return new TreeMap<String, String>() {
                    {
                        put("nomer.indexfungorum.export", "/org/globalbioticinteractions/nomer/match/indexfungorum/export.csv");
                    }
                }.get(key);
            }
        });
    }

}