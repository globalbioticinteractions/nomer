package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class GBIFTaxonServiceTest {

    @Test
    public void enrichById() throws PropertyEnricherException {
        GBIFTaxonService service = createService();

        String externalId = "GBIF:3220631";
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(null, externalId)));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Desulfobacter vibrioformis"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("GBIF:3220631"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Desulfobacter vibrioformis"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("GBIF:3220631"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("species"));
    }

    @Test
    public void enrichByName() throws PropertyEnricherException {
        GBIFTaxonService service = createService();

        String name = "Desulfofaba hansenii";
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(name, null)));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Desulfofaba hansenii"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("GBIF:3220666"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Desulfofaba hansenii"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("GBIF:3220666"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("species"));
    }

    @Test
    public void enrichBySynonymById() throws PropertyEnricherException {
        GBIFTaxonService service = createService();

        String externalId = "GBIF:3220667";
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(null, externalId)));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Desulfofaba hansenii"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("GBIF:3220666"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Desulfofaba hansenii"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("GBIF:3220666"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
    }

    @Test
    public void enrichBySynonymByName() throws PropertyEnricherException {
        GBIFTaxonService service = createService();

        String name = "Desulfomusa hansenii";
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(name, null)));

        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Desulfofaba hansenii"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("GBIF:3220666"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Desulfofaba hansenii"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("GBIF:3220666"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("species"));
    }

    @Test
    public void comparison() {
        List<String> list = Arrays.asList("Zyxomma atlanticum", "Zyxomma", "Zyxmyia megachile", "Zyx notabilis");
        Collections.sort(list);
        Collections.reverse(list);
        assertThat(list, is(Arrays.asList("Zyxomma atlanticum", "Zyxomma", "Zyxmyia megachile", "Zyx notabilis")));
//
//          note however that
//         $ cat names.txt | sort -r
//         Zyxomma atlanticum
//         Zyxomma
//         Zyx notabilis <-- flipped
//         Zyxmyia megachile <-- flipped
//
//         whereas the java sort would produce:
//         Zyxomma atlanticum
//         Zyxomma
//         Zyxmyia megachile <--
//         Zyx notabilis <--
//
//         however, when
//         $ cat names.txt | LC_ALL=C sort -r
//         Zyxomma atlanticum
//         Zyxomma
//         Zyxmyia megachile
//         Zyx notabilis
//

    }

    @Test(expected = IllegalArgumentException.class)
    public void enrichWithUnsortedNameId() throws PropertyEnricherException {
        GBIFTaxonService service = createService("/org/globalbioticinteractions/nomer/match/gbif/backbone-current-name-id-issue.txt");

        try {
            service.enrich(TaxonUtil.taxonToMap(new TaxonImpl("Desulfomusa hansenii", null)));
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), is("Keys in 'source' iterator are not reverse sorted"));
            throw ex;
        }
    }

    private GBIFTaxonService createService() {
        return createService("/org/globalbioticinteractions/nomer/match/gbif/backbone-current-name-id-sorted.txt");
    }

    private GBIFTaxonService createService(final String nameUrl) {
        return new GBIFTaxonService(new TermMatcherContext() {
            @Override
            public String getCacheDir() {
                return new File("target/gbifCache" + UUID.randomUUID()).getAbsolutePath();
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
                        put("nomer.gbif.ids", "/org/globalbioticinteractions/nomer/match/gbif/backbone-current-simple-sorted.txt");
                        put("nomer.gbif.names", nameUrl);
                    }
                }.get(key);
            }
        });
    }


}