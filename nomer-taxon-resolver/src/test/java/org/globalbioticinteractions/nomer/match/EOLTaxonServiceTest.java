package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.globalbioticinteractions.nomer.cmd.OutputFormat;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class EOLTaxonServiceTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();


    @Test
    public void enrichById() throws PropertyEnricherException {
        PropertyEnricher service = createService("taxon.tab");

        TaxonImpl taxon = new TaxonImpl(null, "EOL:327955");
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Hominidae | Homininae | Homo | Homo sapiens"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("EOL:327955"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Homo sapiens"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("EOL:47049573 |  | EOL:42268 | EOL:327955"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("family | subfamily | genus | species"));
    }

    @Test
    public void enrichByName() throws PropertyEnricherException {
        PropertyEnricher service = createService("taxon.tab");

        TaxonImpl taxon = new TaxonImpl("Homo sapiens", null);
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Hominidae | Homininae | Homo | Homo sapiens"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("EOL:327955"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Homo sapiens"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("EOL:47049573 |  | EOL:42268 | EOL:327955"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("family | subfamily | genus | species"));
    }

    @Ignore("note that the 1.1 version of the Dynamic Hierachy did not provide canonical names for synonyms." +
            "This is why the synonym lookup does not work for the v1.1 style taxon table")
    @Test
    public void enrichBySynonymName() throws PropertyEnricherException {
        PropertyEnricher service = createService("taxon.tab");

        TaxonImpl taxon = new TaxonImpl("Arius felis", null);
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Ariopsis felis"));
    }

    @Test
    public void enrichByName2() throws PropertyEnricherException {
        PropertyEnricher service = createService("taxon2.tab");

        TaxonImpl taxon = new TaxonImpl("Homo sapiens", null);
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Hominidae | Homininae | Homo | Homo sapiens"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("EOL:327955"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Homo sapiens"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getAuthorship(), is("Linnaeus 1758"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("EOL:47049573 | EOL:52231771 | EOL:42268 | EOL:327955"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("family | subfamily | genus | species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathAuthorships(), is(" |  | Linnaeus 1758 | Linnaeus 1758"));
    }

    @Test
    public void synonym2() throws PropertyEnricherException {
        PropertyEnricher service = createService("taxon2.tab");

        TaxonImpl taxon = new TaxonImpl("Arius felis", null);
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Ariopsis | Ariopsis felis"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("EOL:223038"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Ariopsis felis"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getAuthorship(), is("(Linnaeus 1766)"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("EOL:47065416 | EOL:223038"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("genus | species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathAuthorships(), is(" | (Linnaeus 1766)"));
    }


    @Test
    public void enrichNoMatch() throws PropertyEnricherException {
        PropertyEnricher service = createService("taxon.tab");

        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(new TaxonImpl(null, "ITIS:999999999")));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is(nullValue()));
    }

    @Test
    public void enrichPrefixMismatch() throws PropertyEnricherException {
        PropertyEnricher service = createService("taxon.tab");

        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(new TaxonImpl(null, "FOO:2")));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is(nullValue()));
    }

    private PropertyEnricher createService(final String resourceName) throws PropertyEnricherException {
        try {
            final String absolutePath = folder.newFolder().getAbsolutePath();
            return new EOLTaxonService(new TermMatcherContextClasspath() {

                @Override
                public String getCacheDir() {
                    return absolutePath;
                }

                @Override
                public OutputFormat getOutputFormat() {
                    return null;
                }

                @Override
                public String getProperty(String key) {
                    return new TreeMap<String, String>() {
                        {
                            put("nomer.eol.taxon", "/org/globalbioticinteractions/nomer/match/eol/" + resourceName);
                        }
                    }.get(key);
                }
            });
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to create test folder", e);
        }

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