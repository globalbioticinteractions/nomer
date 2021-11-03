package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class EOLTaxonServiceTest {

    @Test
    public void enrich() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl(null, "EOL:327955");
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Homo | Homo sapiens"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("EOL:327955"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Homo sapiens"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("EOL:42268 | EOL:327955"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("genus | species"));
    }


    @Test
    public void enrichNoMatch() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(new TaxonImpl(null, "ITIS:999999999")));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is(nullValue()));
    }

    @Test
    public void enrichPrefixMismatch() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(new TaxonImpl(null, "FOO:2")));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is(nullValue()));
    }

    private PropertyEnricher createService() {
        EOLTaxonService taxonService = new EOLTaxonService(new TermMatcherContextClasspath() {
            @Override
            public String getCacheDir() {
                return new File("target/eolCache" + UUID.randomUUID()).getAbsolutePath();
            }

            @Override
            public String getProperty(String key) {
                return new TreeMap<String, String>() {
                    {
                        put("nomer.eol.taxon", "/org/globalbioticinteractions/nomer/match/eol/taxon.tab");
                    }
                }.get(key);
            }
        });
        return taxonService;
    }

    @Test
    public void parseNodes() throws PropertyEnricherException {
        Map<String, Map<String,String>> node = new TreeMap<>();
        Map<String, String> childParent = new TreeMap<>();

        InputStream nodesStream = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/eol/taxon.tab");
        EOLTaxonService.parseNodes(node, childParent, nodesStream);

        assertThat(childParent.get("EOL-000000641745"), is("EOL-000000641744"));
        Map<String, String> properties = node.get("EOL-000000641745");
        assertThat(TaxonUtil.mapToTaxon(properties).getExternalId(), is("EOL:327955"));
        assertThat(TaxonUtil.mapToTaxon(properties).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(properties).getName(), is("Homo sapiens"));

    }

    @Test
    public void denormalizeTaxa() throws PropertyEnricherException {
        Map<String, Map<String, String>> taxonMap = new TreeMap<String, Map<String, String>>() {{
            TaxonImpl one = new TaxonImpl("one name", "1");
            one.setRank("rank one");
            put("1", TaxonUtil.taxonToMap(one));

            TaxonImpl two = new TaxonImpl("two name", "2");
            two.setRank("rank two");
            put("2", TaxonUtil.taxonToMap(two));
        }};

        Map<String, Map<String, String>> taxonMapDenormalized = new TreeMap<>();

        Map<String, String> childParent = new TreeMap<String, String>() {{
            put("1", "2");
        }};


        EOLTaxonService.denormalizeTaxa(taxonMap, taxonMapDenormalized, childParent);

        Taxon actual = TaxonUtil.mapToTaxon(taxonMapDenormalized.get("1"));
        assertThat(actual.getPath(), is("two name | one name"));
        assertThat(actual.getPathIds(), is("2 | 1"));
        assertThat(actual.getPathNames(), is("rank two | rank one"));
        assertThat(actual.getRank(), is("rank one"));
        assertThat(actual.getName(), is("one name"));
        assertThat(actual.getExternalId(), is("1"));

        Taxon two = TaxonUtil.mapToTaxon(taxonMapDenormalized.get("2"));
        assertThat(two.getPath(), is("two name"));
        assertThat(two.getPathIds(), is("2"));
        assertThat(two.getPathNames(), is("rank two"));
        assertThat(two.getRank(), is("rank two"));
        assertThat(two.getName(), is("two name"));
        assertThat(two.getExternalId(), is("2"));

    }

}