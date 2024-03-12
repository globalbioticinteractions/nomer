package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.cmd.OutputFormat;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

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
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Fungi | Basidiomycota | Agaricomycotina | Agaricomycetes | Agaricomycetidae | Agaricales | Incertae sedis | Leucocybe candicans"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("kingdom | phylum | subphylum | class | subclass | order | family | "));
    }

    @Test
    public void enrichByName() throws PropertyEnricherException {
        PropertyEnricher service = createService();
        TaxonImpl taxon = new TaxonImpl("Clitocybe candicans", null);
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));
        assertIF808518(enriched);
    }

    @Test
    public void enrichByMismatchName() throws PropertyEnricherException {
        PropertyEnricher service = createService();
        TaxonImpl taxon = new TaxonImpl("Donald duck", null);
        Map<String, String> enriched = service.enrichFirstMatch(TaxonUtil.taxonToMap(taxon));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is(nullValue()));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Donald duck"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is(nullValue()));
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

    @Test
    public void matchNoMatchByName() throws PropertyEnricherException {

        AtomicBoolean foundMatch = new AtomicBoolean();
        PropertyEnricher service = createService();

        ((TermMatcher) service).match(Arrays.asList(new TermImpl(null, "Homo sapiens")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                assertThat(NameType.NONE, is(nameType));
                foundMatch.set(true);
            }
        });

        assertThat(foundMatch.get(), is(true));
    }

    @Test
    public void matchNoMatchByID() throws PropertyEnricherException {

        AtomicBoolean foundMatch = new AtomicBoolean(false);
        PropertyEnricher service = createService();

        ((TermMatcher) service).match(Arrays.asList(new TermImpl("IF:FFFFFF", "Homo sapiens")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                assertThat(NameType.NONE, is(nameType));
                foundMatch.set(true);
            }
        });

        assertThat(foundMatch.get(), is(true));
    }

    private PropertyEnricher createService() {
        File file = new File("target/cache" + UUID.randomUUID());
        return new IndexFungorumTaxonService(new TermMatcherContextClasspath() {
            @Override
            public String getCacheDir() {
                return file.getAbsolutePath();
            }

            @Override
            public OutputFormat getOutputFormat() {
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