package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.TermMatchListener;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsNull.nullValue;

public class NCBITaxonServiceTest {

    @Test
    public void enrich() throws PropertyEnricherException {
        NCBITaxonService service = createService();

        String externalId = "NCBI:2";
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(null, externalId)));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Bacteria"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("NCBI:2"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Bacteria"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("superkingdom"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("NCBI:2"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("superkingdom"));
    }

    @Test
    public void matchById() throws PropertyEnricherException {
        NCBITaxonService service = createService();

        AtomicBoolean gotReply = new AtomicBoolean(false);
        String externalId = "NCBI:2";
        service.match(Arrays.asList(new TaxonImpl(null, externalId)), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon ) {
                assertThat(nameType, is(NameType.SAME_AS));
                assertThat(resolvedTaxon.getName(), is("Bacteria"));
                gotReply.set(true);
            }
        });

        assertTrue(gotReply.get());
    }

    @Test
    public void matchByName() throws PropertyEnricherException {
        NCBITaxonService service = createService();

        AtomicBoolean gotReply = new AtomicBoolean(false);
        service.match(Arrays.asList(new TaxonImpl("Anteholosticha manca")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon ) {
                assertThat(nameType, is(NameType.SAME_AS));
                assertThat(resolvedTaxon.getName(), is("Anteholosticha manca"));
                assertThat(resolvedTaxon.getId(), is("NCBI:385028"));
                gotReply.set(true);
            }
        });

        assertTrue(gotReply.get());
    }

    @Test
    public void matchBySynonym() throws PropertyEnricherException {
        NCBITaxonService service = createService();

        AtomicBoolean gotReply = new AtomicBoolean(false);
        service.match(Arrays.asList(new TaxonImpl("Holosticha manca")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon ) {
                assertThat(nameType, is(NameType.SYNONYM_OF));
                assertThat(providedTerm.getName(), is("Holosticha manca"));
                assertThat(resolvedTaxon.getName(), is("Anteholosticha manca"));
                assertThat(resolvedTaxon.getId(), is("NCBI:385028"));
                gotReply.set(true);
            }
        });

        assertTrue(gotReply.get());
    }

    @Test
    public void matchByCommonName() throws PropertyEnricherException {
        NCBITaxonService service = createService();

        AtomicBoolean gotReply = new AtomicBoolean(false);
        service.match(Arrays.asList(new TaxonImpl("eubacteria")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon ) {
                assertThat(nameType, is(NameType.COMMON_NAME_OF));
                assertThat(providedTerm.getName(), is("eubacteria"));
                assertThat(resolvedTaxon.getName(), is("Bacteria"));
                assertThat(resolvedTaxon.getId(), is("NCBI:2"));
                gotReply.set(true);
            }
        });

        assertTrue(gotReply.get());
    }

    @Test
    public void matchByCommonNameDifferentCase() throws PropertyEnricherException {
        NCBITaxonService service = createService();

        AtomicBoolean gotReply = new AtomicBoolean(false);
        service.match(Collections.singletonList(new TaxonImpl("Eubacteria")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon ) {
                assertThat(nameType, is(NameType.NONE));
                gotReply.set(true);
            }
        });

        assertTrue(gotReply.get());
    }


    @Test
    public void matchBySynonymAndId() throws PropertyEnricherException {
        NCBITaxonService service = createService();
        final List<String> resolvedNames = new ArrayList<>();
        service.match(Arrays.asList(
                new TaxonImpl("Holosticha manca"),
                new TaxonImpl(null, "NCBI:2")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon ) {
                resolvedNames.add(resolvedTaxon.getName());
            }
        });

        assertThat(resolvedNames.size(), is(2));
        assertThat(resolvedNames, hasItem("Bacteria"));
        assertThat(resolvedNames, hasItem("Anteholosticha manca"));

    }

    @Test
    public void enrichNCBIPreferredPrefix() throws PropertyEnricherException {
        NCBITaxonService service = createService();

        String externalId = "NCBI:txid2";
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(null, externalId)));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Bacteria"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("NCBI:2"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Bacteria"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("superkingdom"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("NCBI:2"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("superkingdom"));
    }

    @Test
    public void enrichOBOPrefix() throws PropertyEnricherException {
        NCBITaxonService service = createService();

        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(null, "http://purl.obolibrary.org/obo/NCBITaxon_2")));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Bacteria"));
    }

    @Test
    public void enrichOtherPrefix() throws PropertyEnricherException {
        NCBITaxonService service = createService();

        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(null, "NCBITaxon:2")));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Bacteria"));
    }

    @Test
    public void enrichMerged() throws PropertyEnricherException {
        NCBITaxonService service = createService();

        String taxonId = "NCBI:666";
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(null, taxonId)));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Bacteria"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("NCBI:2"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Bacteria"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("superkingdom"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("NCBI:2"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("superkingdom"));
    }

    @Test
    public void enrichNoMatch() throws PropertyEnricherException {
        NCBITaxonService service = createService();

        Map<String, String> enriched = service.enrich
                (TaxonUtil.taxonToMap(new TaxonImpl(null, "NCBI:999999999"))
                );

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is(nullValue()));
    }

    @Test
    public void enrichPrefixMismatch() throws PropertyEnricherException {
        NCBITaxonService service = createService();

        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(null, "FOO:2")));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is(nullValue()));
    }

    private NCBITaxonService createService() {
        return new NCBITaxonService(new TermMatcherContextClasspath() {
            @Override
            public String getCacheDir() {
                return new File("target/ncbiCache" + UUID.randomUUID()).getAbsolutePath();
            }

            @Override
            public String getOutputFormat() {
                return null;
            }

            @Override
            public String getProperty(String key) {
                return new TreeMap<String, String>() {
                    {
                        put("nomer.ncbi.nodes", "/org/globalbioticinteractions/nomer/match/ncbi/nodes.dmp");
                        put("nomer.ncbi.names", "/org/globalbioticinteractions/nomer/match/ncbi/names.dmp");
                        put("nomer.ncbi.merged", "/org/globalbioticinteractions/nomer/match/ncbi/merged.dmp");
                    }
                }.get(key);
            }
        });
    }

    @Test
    public void parseNodes() throws PropertyEnricherException {
        Map<String, Map<String, String>> node = new TreeMap<>();
        Map<String, String> childParent = new TreeMap<>();

        InputStream nodesStream = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/ncbi/nodes.dmp");
        NCBITaxonService.parseNodes(node, childParent, nodesStream);

        assertThat(childParent.get("NCBI:2"), is("NCBI:131567"));
        assertThat(TaxonUtil.mapToTaxon(node.get("NCBI:2")).getRank(), is("superkingdom"));

    }

    @Test
    public void parseMerged() throws PropertyEnricherException {
        Map<String, String> mergedIds = new TreeMap<>();

        InputStream mergedStream = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/ncbi/merged.dmp");
        NCBITaxonService.parseMerged(mergedIds, mergedStream);

        assertThat(mergedIds.get("NCBI:12"), is("NCBI:74109"));
        assertThat(mergedIds.get("NCBI:46"), is("NCBI:39"));

    }

    @Test
    public void denormalizeTaxa() throws PropertyEnricherException {
        Map<String, Map<String, String>> taxonMap = new TreeMap<String, Map<String, String>>() {{
            TaxonImpl one = new TaxonImpl(null, "1");
            one.setRank("rank one");
            put("1", TaxonUtil.taxonToMap(one));

            TaxonImpl two = new TaxonImpl(null, "2");
            two.setRank("rank two");
            put("2", TaxonUtil.taxonToMap(two));
        }};

        Map<String, Map<String, String>> taxonMapDenormalized = new TreeMap<>();

        Map<String, String> childParent = new TreeMap<String, String>() {{
            put("1", "2");
        }};

        Map<String, String> taxonNames = new TreeMap<String, String>() {{
            put("1", "one name");
            put("2", "two name");
        }};


        NCBITaxonService.denormalizeTaxa(taxonMap, taxonMapDenormalized, childParent, taxonNames);

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

    @Test
    public void parseNames() throws PropertyEnricherException {
        Map<String, String> nameMap = new TreeMap<>();
        Map<String, List<String>> nameIds = new TreeMap<>();
        Map<String, List<String>> commonNameIds = new TreeMap<>();
        Map<String, List<String>> synonymIds = new TreeMap<>();

        InputStream namesStream = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/ncbi/names.dmp");

        NCBITaxonService.parseNames(namesStream, nameMap, nameIds, commonNameIds, synonymIds);

        assertThat(nameMap.size(), is(3));
        assertThat(nameMap.get("NCBI:1"), is("root"));
        assertThat(nameMap.get("NCBI:2"), is("Bacteria"));
        assertThat(nameMap.get("NCBI:385028"), is("Anteholosticha manca"));

        assertThat(nameIds.get("Anteholosticha manca"), hasItem("NCBI:385028"));
        assertThat(synonymIds.get("Holosticha manca"), hasItem("NCBI:385028"));
        assertThat(commonNameIds.get("eubacteria"), hasItem("NCBI:2"));
    }


}