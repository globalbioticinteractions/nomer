package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.TermMatchListener;
import org.globalbioticinteractions.nomer.cmd.OutputFormat;
import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

public class OpenTreeTaxonServiceTest {

    @Test
    public void queryAll() throws PropertyEnricherException {
        OpenTreeTaxonService service = createService();
        AtomicLong counter = new AtomicLong();
        Collection<String> ids = new TreeSet<>();
        service.match(Collections.singletonList(
                new TaxonImpl(".*", ".*")), new TermMatchListener() {
                    @Override
                    public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                        counter.incrementAndGet();
                        ids.add(taxon.getExternalId());
                    }
                });

        assertThat(counter.get(), is(8L));

        assertThat(ids.size(), is(8));

        assertThat(ids, hasItem("OTT:470454"));
//        assertThat(ids, hasItem("OTT:525972"));
    }


    @Test
    public void enrichById() throws PropertyEnricherException {
        OpenTreeTaxonService service = createService();
        Map<String, String> enriched = service.enrich(
                TaxonUtil.taxonToMap(
                        new TaxonImpl(null, "OTT:1098176")
                )
        );

        assertPasteurellaceae(enriched);
    }

    @Test
    public void enrichByName() throws PropertyEnricherException {
        OpenTreeTaxonService service = createService();
        Taxon phryganella = new TaxonImpl("Pasteurellaceae", null);
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(phryganella));
        assertPasteurellaceae(enriched);
    }

    @Test
    public void enrichBySynonymId() throws PropertyEnricherException {
        OpenTreeTaxonService service = createService();

        Map<String, String> enriched = service.enrich(
                TaxonUtil.taxonToMap(new TaxonImpl(null, "OTT:1098176")));

        assertPasteurellaceae(enriched);
    }

    @Test
    public void enrichBySynonymIdGBIF() throws PropertyEnricherException {
        OpenTreeTaxonService service = createService();

        Map<String, String> enriched = service.enrich(
                TaxonUtil.taxonToMap(new TaxonImpl(null, "GBIF:9536")));

        assertPasteurellaceae(enriched);
    }

    @Test
    public void enrichBySynonymIdWORMS() throws PropertyEnricherException {
        OpenTreeTaxonService service = createService();

        Map<String, String> enriched = service.enrich(
                TaxonUtil.taxonToMap(new TaxonImpl(null, "WORMS:394200")));

        assertPasteurellaceae(enriched);
    }

    public void assertPasteurellaceae(Map<String, String> enriched) {
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("OTT:1098176"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Pasteurellaceae"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("family"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("life | cellular organisms | Bacteria | Proteobacteria | Gammaproteobacteria | Pasteurellales | Pasteurellaceae"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("OTT:805080 | OTT:93302 | OTT:844192 | OTT:248067 | OTT:822744 | OTT:767311 | OTT:1098176"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is(" |  | domain | phylum | class | order | family"));
    }

    private OpenTreeTaxonService createService() {
        return new OpenTreeTaxonService(new TermMatcherContextClasspath() {
            @Override
            public String getCacheDir() {
                return new File("target/ottCache" + UUID.randomUUID()).getAbsolutePath();
            }

            @Override
            public OutputFormat getOutputFormat() {
                return null;
            }

            @Override
            public String getProperty(String key) {
                return new TreeMap<String, String>() {
                    {
                        put("nomer.ott.taxonomy", "/org/globalbioticinteractions/nomer/match/ott/taxonomy.tsv");
                        put("nomer.ott.synonyms", "/org/globalbioticinteractions/nomer/match/ott/synonyms.tsv");
                    }
                }.get(key);
            }
        });
    }

    private OpenTreeTaxonService createService(final String taxonomy, final String synonyms) {
        return new OpenTreeTaxonService(new TermMatcherContextClasspath() {
            @Override
            public String getCacheDir() {
                return new File("target/ottCache" + UUID.randomUUID()).getAbsolutePath();
            }

            @Override
            public OutputFormat getOutputFormat() {
                return null;
            }

            @Override
            public String getProperty(String key) {
                return new TreeMap<String, String>() {
                    {
                        put("nomer.ott.taxonomy", taxonomy);
                        put("nomer.ott.synonyms", synonyms);
                    }
                }.get(key);
            }
        });
    }


}