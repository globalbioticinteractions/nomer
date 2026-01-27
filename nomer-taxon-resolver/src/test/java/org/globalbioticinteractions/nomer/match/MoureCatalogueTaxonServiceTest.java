package org.globalbioticinteractions.nomer.match;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.TermMatchListener;
import org.globalbioticinteractions.nomer.cmd.OutputFormat;
import org.junit.Test;

public class MoureCatalogueTaxonServiceTest {

    @Test
    public void queryAll() throws PropertyEnricherException {
        MoureCatalogueTaxonService service = createService();
        AtomicLong counter = new AtomicLong();
        Collection<String> ids = new TreeSet<>();
        Collection<String> names = new TreeSet<>();
        service.match(Collections.singletonList(
                new TaxonImpl(".*", ".*")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                counter.incrementAndGet();
                if (StringUtils.isNotBlank(term.getId())) {
                    ids.add(term.getId());
                }
                if (StringUtils.isNotBlank(taxon.getId())) {
                    ids.add(taxon.getId());
                }

                if (StringUtils.isNotBlank(taxon.getName())) {
                    names.add(taxon.getName());
                }
                if (StringUtils.isNotBlank(term.getName())) {
                    names.add(term.getName());
                }
            }
        });

        assertThat(ids, hasItem("MOURE:1"));
        assertThat(ids, hasItem("MOURE:30"));
        assertThat(ids, hasItem("MOURE:54"));

        assertThat(names, hasItem("Apidae"));
        assertThat(names, hasItem("Callandrena"));
        assertThat(names, hasItem("Acamptopoeum melanogaster"));

        assertThat(counter.get(), is(214L));

        assertThat(ids.size(), is(176));
        assertThat(names.size(), is(92));        
    }


    @Test
    public void enrichById() throws PropertyEnricherException {
        MoureCatalogueTaxonService service = createService();
        Map<String, String> enriched = service.enrich(
                TaxonUtil.taxonToMap(
                        new TaxonImpl(null, "MOURE:40")
                )
        );

        assertAndrenaToluca(enriched);
    }    

    @Test
    public void enrichByName() throws PropertyEnricherException {
        MoureCatalogueTaxonService service = createService();
        Taxon andrena = new TaxonImpl("Andrena toluca", null);
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(andrena));
        assertAndrenaToluca(enriched);
    }

    @Test
    public void enrichBySynonymId() throws PropertyEnricherException {
        MoureCatalogueTaxonService service = createService();

        Map<String, String> enriched = service.enrich(
                TaxonUtil.taxonToMap(new TaxonImpl(null, "MOURE:40")));

        assertAndrenaToluca(enriched);
    }

    @Test
    public void enrichBySynonymName() throws PropertyEnricherException {
        MoureCatalogueTaxonService service = createService();

        Map<String, String> enriched = service.enrich(
                TaxonUtil.taxonToMap(new TaxonImpl("Aporandrena kraussi", null)));

        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Andrena kraussi"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("MOURE:8"));
    }       

    public void assertAndrenaToluca(Map<String, String> enriched) {
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("MOURE:40"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Andrena toluca"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("species"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Apidae | Andreninae | Andrenini | Andrena | Charitandrena | Andrena toluca"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("MOURE:1 | MOURE:2 | MOURE:3 | MOURE:6 | MOURE:39 | MOURE:40"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("family | subfamily | tribe | genus | subgenus | species"));
    }

    private MoureCatalogueTaxonService createService() {    	
        return new MoureCatalogueTaxonService(new TermMatcherContextClasspath() {						
            @Override
            public String getCacheDir() {
                return new File("target/moureCache" + UUID.randomUUID()).getAbsolutePath();
            }

            @Override
            public OutputFormat getOutputFormat() {
                return null;
            }

            @Override
            public String getProperty(String key) {
                return new TreeMap<String, String>() {
                    {
                        put("nomer.moure.taxonomy", "/org/globalbioticinteractions/nomer/match/moure/taxonomy.tsv");
                        put("nomer.moure.synonyms", "/org/globalbioticinteractions/nomer/match/moure/synonyms.tsv");
                    }
                }.get(key);
            }
        });
    }


}