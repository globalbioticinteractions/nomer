package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.globalbioticinteractions.nomer.cmd.OutputFormat;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static org.eol.globi.service.TaxonUtil.mapToTaxon;
import static org.eol.globi.service.TaxonUtil.taxonToMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

public class TPTTaxonServiceTest {

    @Test
    public void enrichById() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl(null, "TPT:10766101");
        Map<String, String> enriched = service.enrichFirstMatch(taxonToMap(taxon));
        assertGardineri(enriched);
    }

    private void assertGardineri(Map<String, String> enriched) {
        assertThat(mapToTaxon(enriched).getName(), is("Dicrogonatus gardineri"));
        assertThat(mapToTaxon(enriched).getExternalId(), is("acari_10766101"));
        assertThat(mapToTaxon(enriched).getAuthorship(), is("(Warburton, 1912)"));
        assertThat(mapToTaxon(enriched).getRank(), is("species"));
        assertThat(mapToTaxon(enriched).getPath(), is("Animalia | Arthropoda | Arachnida | Holothyrida | Holothyridae | Dicrogonatus | gardineri"));
        assertThat(mapToTaxon(enriched).getPathNames(), is("kingdom | phylum | class | order | family | genus | specificEpithet"));
        assertThat(mapToTaxon(enriched).getPathAuthorships(), is(" |  |  |  |  |  | "));
    }

    @Test
    public void enrichByName() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl("Dicrogonatus gardineri", null);
        Map<String, String> enriched = service.enrichFirstMatch(taxonToMap(taxon));

        assertGardineri(enriched);
    }

    @Test
    public void enrichByHigherOrderByName() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl("Allothyridae", null);
        Map<String, String> enriched = service.enrichFirstMatch(taxonToMap(taxon));

        Taxon enrichedTaxon = mapToTaxon(enriched);

        assertThat(enrichedTaxon.getName(), is("Allothyridae"));
        assertThat(enrichedTaxon.getPath(), is("Animalia | Arthropoda | Arachnida | Acari | Parasitiformes | Holothyrida | Holothyroidea | Allothyridae"));

    }

    @Test
    public void enrichSynonym() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl(null, "TPT:6892348");
        Map<String, String> enriched = service.enrichFirstMatch(taxonToMap(taxon));

        assertThat(mapToTaxon(enriched).getExternalId(), is("acari_6892347"));
        assertThat(mapToTaxon(enriched).getAuthorship(), is("(Berlese, 1923)"));
        assertThat(mapToTaxon(enriched).getName(), is("Haplothyrus expolitissimus"));
        assertThat(mapToTaxon(enriched).getRank(), is("species"));
        assertThat(mapToTaxon(enriched).getPath(), is("Animalia | Arthropoda | Arachnida | Holothyrida | Holothyridae | Haplothyrus | expolitissimus"));
        assertThat(mapToTaxon(enriched).getPathNames(), is("kingdom | phylum | class | order | family | genus | specificEpithet"));
    }

    @Test
    public void enrichSynonymPhthiraptera() throws PropertyEnricherException {
        PropertyEnricher service = getTptTaxonService("/org/globalbioticinteractions/nomer/match/tpt/phthiraptera.csv");

        TaxonImpl taxon = new TaxonImpl("Brueelia subalbicans", null);
        Map<String, String> enriched = service.enrichFirstMatch(taxonToMap(taxon));

        assertThat(mapToTaxon(enriched).getName(), is("Brueelia papuana"));
        assertThat(mapToTaxon(enriched).getAuthorship(), is("(Giebel, 1879)"));
        assertThat(mapToTaxon(enriched).getExternalId(), is("1032"));
        assertThat(mapToTaxon(enriched).getRank(), is("species"));
        assertThat(mapToTaxon(enriched).getPath(), is("Animalia | Arthropoda | Insecta | Psocodea | Troctomorpha | Nanopsocetae | Phthiraptera | Ischnocera | Philopteridae | Brueelia | papuana"));
        assertThat(mapToTaxon(enriched).getPathNames(), is("kingdom | phylum | class | order | suborder | infraorder | parvorder | nanorder | family | genus | specificEpithet"));
    }

    @Test
    public void getIdOrNullGBIFPrefix() {
        TPTTaxonService tptTaxonService = new TPTTaxonService(null);

        String id = tptTaxonService.getIdOrNull(
                new TaxonImpl("foo", "GBIF:123"),
                tptTaxonService.getTaxonomyProvider()
        );

        assertThat(id, is("123"));

    }

    @Test
    public void getIdOrNullTPTPrefix() {
        TPTTaxonService tptTaxonService = new TPTTaxonService(null);

        String id = tptTaxonService.getIdOrNull(
                new TaxonImpl("foo", "TPT:123"),
                tptTaxonService.getTaxonomyProvider()
        );

        assertThat(id, is("123"));

    }

    @Test
    public void getIdOrNullAcariPrefix() {
        TPTTaxonService tptTaxonService = new TPTTaxonService(null);

        String id = tptTaxonService.getIdOrNull(
                new TaxonImpl("foo", "Acari_123"),
                tptTaxonService.getTaxonomyProvider()
        );

        assertThat(id, is("123"));

    }

    @Test
    public void getIdOrNullacariPrefix() {
        TPTTaxonService tptTaxonService = new TPTTaxonService(null);

        String id = tptTaxonService.getIdOrNull(new TaxonImpl("foo", "acari_123"), tptTaxonService.getTaxonomyProvider());

        assertThat(id, is("123"));

    }

    @Test
    public void enrichNoMatch() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        Map<String, String> noMatch = taxonToMap(new TaxonImpl(null, "GBIF:999999999"));
        Map<String, String> enriched = service.enrichFirstMatch(noMatch);

        assertThat(mapToTaxon(enriched).getPath(), is(nullValue()));
    }

    @Test
    public void enrichPrefixMismatch() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        Map<String, String> prefixMismatch = taxonToMap(new TaxonImpl(null, "FOO:2"));
        Map<String, String> enriched = service.enrichFirstMatch(prefixMismatch);

        assertThat(mapToTaxon(enriched).getPath(), is(nullValue()));
    }

    @Test
    public void matchMultiple() throws PropertyEnricherException {
        PropertyEnricher service = getTptTaxonService(
                StringUtils.join(new String[]{
                        "/org/globalbioticinteractions/nomer/match/tpt/acari.csv",
                        "/org/globalbioticinteractions/nomer/match/tpt/siphonaptera.csv"}, ",")
        );


        assertThat(
                mapToTaxon(service
                        .enrichFirstMatch(
                                taxonToMap(
                                        new TaxonImpl("Ancistropsylla nepalensis")
                                )
                        )).getPath(),
                is("Animalia | Arthropoda | Insecta | Siphonaptera | Ancistropsyllidae | Ancistropsylla | nepalensis"));

        assertThat(
                mapToTaxon(service
                        .enrichFirstMatch(
                                taxonToMap(
                                        new TaxonImpl("Dicrogonatus gardineri")
                                )
                        )).getPath(),
                is("Animalia | Arthropoda | Arachnida | Holothyrida | Holothyridae | Dicrogonatus | gardineri"));
    }


    @Test
    public void matchMultipleNamesSomeWithoutTaxonId() throws PropertyEnricherException {
        // Ixodida appears to not have taxonIDs, whereas other TPT taxa did.
        // https://github.com/njdowdy/tpt-taxonomy/issues/17
        PropertyEnricher service = getTptTaxonService(
                StringUtils.join(new String[]{
                        "/org/globalbioticinteractions/nomer/match/tpt/acari.csv",
                        "/org/globalbioticinteractions/nomer/match/tpt/ixodida.csv"}, ",")
        );


        Taxon arcariTaxon = mapToTaxon(service
                .enrichFirstMatch(
                        taxonToMap(
                                new TaxonImpl("Argas africolumbae")
                        )
                ));
        assertThat(
                arcariTaxon.getPath(),
                is("Animalia | Arthropoda | Arachnida | Acari | Parasitiformes | Ixodida | Ixodoidea | Argasidae | Argas | africolumbae"));
        assertThat(
                arcariTaxon.getExternalId(),
                is(nullValue()));

        assertThat(
                mapToTaxon(service
                        .enrichFirstMatch(
                                taxonToMap(
                                        new TaxonImpl("Dicrogonatus gardineri")
                                )
                        )).getPath(),
                is("Animalia | Arthropoda | Arachnida | Holothyrida | Holothyridae | Dicrogonatus | gardineri"));
    }

    private TPTTaxonService createService() {
        return getTptTaxonService("/org/globalbioticinteractions/nomer/match/tpt/acari.csv");
    }

    private TPTTaxonService getTptTaxonService(String v) {
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
                        put("nomer.tpt.taxon", v);
                    }
                }.get(key);
            }
        });
    }

}