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

public class ITISTaxonServiceTest {

    @Test
    public void enrichById() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl(null, "ITIS:57");
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Bradyrhizobiaceae | Nitrobacter"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("ITIS:57"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Nitrobacter"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("genus"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getAuthorship(), is("ITIS:AUTHORSHIP:177805"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("ITIS:956340 | ITIS:57"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("family | genus"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathAuthorships(), is("ITIS:AUTHORSHIP:184763 | ITIS:AUTHORSHIP:177805"));
    }

    @Test
    public void enrichById2() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl(null, "ITIS:680665");
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("ITIS:680665"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Ariopsis felis"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getAuthorship(), is("(Linnaeus, 1766)"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Ariopsis felis"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("ITIS:680665"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathAuthorships(), is("(Linnaeus, 1766)"));
    }

    @Test
    public void enrichByName() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl("Nitrobacter", null);
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Bradyrhizobiaceae | Nitrobacter"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("ITIS:57"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Nitrobacter"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("genus"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("ITIS:956340 | ITIS:57"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("family | genus"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathAuthorships(), is("ITIS:AUTHORSHIP:184763 | ITIS:AUTHORSHIP:177805"));
    }

    @Test
    public void enrichMerged() throws PropertyEnricherException {
        PropertyEnricher service = createService();

        TaxonImpl taxon = new TaxonImpl(null, "ITIS:57");
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Bradyrhizobiaceae | Nitrobacter"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("ITIS:57"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Nitrobacter"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("genus"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("ITIS:956340 | ITIS:57"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("family | genus"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathAuthorships(), is("ITIS:AUTHORSHIP:184763 | ITIS:AUTHORSHIP:177805"));
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

    private ITISTaxonService createService() {
        File file = new File("target/cache" + UUID.randomUUID());
        return new ITISTaxonService(new TermMatcherContext() {
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
                        put("nomer.itis.taxonomic_units", "/org/globalbioticinteractions/nomer/match/itis/taxonomic_units.psv");
                        put("nomer.itis.synonym_links", "/org/globalbioticinteractions/nomer/match/itis/synonym_links.psv");
                        put("nomer.itis.taxon_unit_types", "/org/globalbioticinteractions/nomer/match/itis/taxon_unit_types.psv");
                        put("nomer.itis.taxon_authors_lkp", "/org/globalbioticinteractions/nomer/match/itis/taxon_authors_lkp.psv");
                    }
                }.get(key);
            }
        });
    }

    @Test
    public void parseNodes() throws PropertyEnricherException {
        Map<Long, Map<String, String>> node = new TreeMap<>();
        Map<Long, Long> childParent = new TreeMap<>();
        Map<String, List<Long>> name2NodeId = new TreeMap<>();
        Map<String, String> rankIdNameMap = new TreeMap<String, String>() {{
            put("1-180", "some rank");
        }};
        Map<Long, String> authorIds = new TreeMap<Long, String>() {{
            put(1L, "some name");
        }};

        InputStream nodesStream = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/itis/taxonomic_units.psv");
        createService().parseNodes(node, childParent, rankIdNameMap, name2NodeId, authorIds, nodesStream);

        assertThat(childParent.get(57L), is(956340L));
        assertThat(TaxonUtil.mapToTaxon(node.get(57L)).getRank(), is("some rank"));

    }

    @Test
    public void parseMerged() throws PropertyEnricherException {
        Map<Long, Long> mergedIds = new TreeMap<>();

        InputStream mergedStream = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/itis/synonym_links.psv");
        ITISTaxonService.parseMerged(mergedIds, mergedStream);

        assertThat(mergedIds.get(51L), is(50L));
        assertThat(mergedIds.get(52L), is(50L));

    }

    @Test
    public void parseRankMap() throws PropertyEnricherException {
        Map<String, String> rankIds = new TreeMap<>();

        String resource = "/org/globalbioticinteractions/nomer/match/itis/taxon_unit_types.psv";
        InputStream mergedStream = getClass().getResourceAsStream(resource);
        ITISTaxonService.parseTaxonUnitTypes(rankIds, mergedStream);

        assertThat(rankIds.get("1-180"), is("genus"));
        assertThat(rankIds.get("1-220"), is("species"));

    }

}