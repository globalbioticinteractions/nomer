package org.eol.globi.service;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertThat;

public class TermMatcherHierarchicalTest {

    @Test
    public void hierarchyMatchByName() throws PropertyEnricherException {


        TermMatcher matcherBasic = new TermMatcher() {

            @Override
            public void match(List<Term> list, TermMatchListener termMatchListener) throws PropertyEnricherException {
                for (Term term : list) {
                    if ("Anura".equals(term.getName())) {
                        TaxonImpl homoSapiens = new TaxonImpl("Anura", "FOO:123");
                        homoSapiens.setPath("Amphibia | Anura");
                        termMatchListener.foundTaxonForTerm(null, term, homoSapiens, NameType.SAME_AS);

                        TaxonImpl anuraPlant = new TaxonImpl("Anura", "FOO:456");
                        anuraPlant.setPath("Plantae | Magnoliophyta | Anura");
                        termMatchListener.foundTaxonForTerm(null, term, anuraPlant, NameType.SAME_AS);
                    }
                }
            }
        };

        TermMatcher matcher = new TermMatcherHierarchical(matcherBasic);


        AtomicLong countTotal = new AtomicLong(0);
        AtomicLong countMatches = new AtomicLong(0);
        matcher.match(Arrays.asList(new TermImpl(null, "Amphibia | Anura")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, Taxon taxon, NameType nameType) {
                countTotal.incrementAndGet();
                if (!NameType.NONE.equals(nameType)) {
                    countMatches.incrementAndGet();
                }
            }
        });

        assertThat(countTotal.get(), Is.is(2L));
        assertThat(countMatches.get(), Is.is(1L));
    }

    @Test
    public void hierarchyMatchById() throws PropertyEnricherException {


        TermMatcher matcherBasic = new TermMatcher() {

            @Override
            public void match(List<Term> list, TermMatchListener termMatchListener) throws PropertyEnricherException {
                for (Term term : list) {
                    if ("FOO:123".equals(term.getId())) {
                        TaxonImpl homoSapiens = new TaxonImpl("Anura", "FOO:123");
                        homoSapiens.setPath("Amphibia | Anura");
                        homoSapiens.setPathIds("FOO:1 | FOO:123");
                        termMatchListener.foundTaxonForTerm(null, term, homoSapiens, NameType.SAME_AS);
                    }

                    else if ("FOO:456".equals(term.getId())) {
                        TaxonImpl anuraPlant = new TaxonImpl("Anura", "FOO:456");
                        anuraPlant.setPath("Plantae | Magnoliophyta | Anura");
                        anuraPlant.setPathIds("FOO:X | FOO:Y | FOO:456");
                        termMatchListener.foundTaxonForTerm(null, term, anuraPlant, NameType.SAME_AS);
                    }
                }
            }
        };

        TermMatcher matcher = new TermMatcherHierarchical(matcherBasic);


        AtomicLong countTotal = new AtomicLong(0);
        AtomicLong countMatches = new AtomicLong(0);
        matcher.match(Arrays.asList(new TermImpl("FOO:1 | FOO:123", null)), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, Taxon taxon, NameType nameType) {
                countTotal.incrementAndGet();
                if (!NameType.NONE.equals(nameType)) {
                    countMatches.incrementAndGet();
                }
            }
        });

        assertThat(countTotal.get(), Is.is(1L));
        assertThat(countMatches.get(), Is.is(1L));
    }

    @Test
    public void hierarchyMatchByNameAndId() throws PropertyEnricherException {


        TermMatcher matcherBasic = new TermMatcher() {

            @Override
            public void match(List<Term> list, TermMatchListener termMatchListener) throws PropertyEnricherException {
                for (Term term : list) {
                    if ("FOO:123".equals(term.getId())) {
                        TaxonImpl homoSapiens = new TaxonImpl("Anura", "FOO:123");
                        homoSapiens.setPath("Amphibia | Anura");
                        homoSapiens.setPathIds("FOO:1 | FOO:123");
                        termMatchListener.foundTaxonForTerm(null, term, homoSapiens, NameType.SAME_AS);
                    }

                    else if ("FOO:456".equals(term.getId())) {
                        TaxonImpl anuraPlant = new TaxonImpl("Anura", "FOO:456");
                        anuraPlant.setPath("Plantae | Magnoliophyta | Anura");
                        anuraPlant.setPathIds("FOO:X | FOO:Y | FOO:456");
                        termMatchListener.foundTaxonForTerm(null, term, anuraPlant, NameType.SAME_AS);
                    }
                }
            }
        };

        TermMatcher matcher = new TermMatcherHierarchical(matcherBasic);


        AtomicLong countTotal = new AtomicLong(0);
        AtomicLong countMatches = new AtomicLong(0);
        matcher.match(Arrays.asList(new TermImpl("FOO:1 | FOO:123", "Amphibia | Anura")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, Taxon taxon, NameType nameType) {
                countTotal.incrementAndGet();
                if (!NameType.NONE.equals(nameType)) {
                    countMatches.incrementAndGet();
                }
            }
        });

        assertThat(countTotal.get(), Is.is(1L));
        assertThat(countMatches.get(), Is.is(1L));
    }


    @Test
    public void hierarchyMismatchByNameAndId() throws PropertyEnricherException {


        TermMatcher matcherBasic = new TermMatcher() {

            @Override
            public void match(List<Term> list, TermMatchListener termMatchListener) throws PropertyEnricherException {
                for (Term term : list) {
                    if ("FOO:123".equals(term.getId())) {
                        TaxonImpl homoSapiens = new TaxonImpl("Anura", "FOO:123");
                        homoSapiens.setPath("Amphibia | Anura");
                        homoSapiens.setPathIds("FOO:1 | FOO:123");
                        termMatchListener.foundTaxonForTerm(null, term, homoSapiens, NameType.SAME_AS);
                    }

                    else if ("FOO:456".equals(term.getId())) {
                        TaxonImpl anuraPlant = new TaxonImpl("Anura", "FOO:456");
                        anuraPlant.setPath("Plantae | Magnoliophyta | Anura");
                        anuraPlant.setPathIds("FOO:X | FOO:Y | FOO:456");
                        termMatchListener.foundTaxonForTerm(null, term, anuraPlant, NameType.SAME_AS);
                    }
                }
            }
        };

        TermMatcher matcher = new TermMatcherHierarchical(matcherBasic);


        AtomicLong countTotal = new AtomicLong(0);
        AtomicLong countMatches = new AtomicLong(0);
        matcher.match(Arrays.asList(new TermImpl("FOO:1 | FOO:123", "Anura")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, Taxon taxon, NameType nameType) {
                countTotal.incrementAndGet();
                if (!NameType.NONE.equals(nameType)) {
                    countMatches.incrementAndGet();
                }
            }
        });

        assertThat(countTotal.get(), Is.is(1L));
        assertThat(countMatches.get(), Is.is(1L));
    }

    @Test
    public void hierarchyEmptyId() throws PropertyEnricherException {


        TermMatcher matcherBasic = new TermMatcher() {

            @Override
            public void match(List<Term> list, TermMatchListener termMatchListener) throws PropertyEnricherException {
                for (Term term : list) {
                    if ("Anura".equals(term.getName())) {
                        TaxonImpl homoSapiens = new TaxonImpl("Anura", "FOO:123");
                        homoSapiens.setPath("Amphibia | Anura");
                        homoSapiens.setPathIds("FOO:1 | FOO:123");
                        termMatchListener.foundTaxonForTerm(null, term, homoSapiens, NameType.SAME_AS);

                        TaxonImpl anuraPlant = new TaxonImpl("Anura", "FOO:456");
                        anuraPlant.setPath("Plantae | Magnoliophyta | Anura");
                        anuraPlant.setPathIds("FOO:X | FOO:Y | FOO:456");
                        termMatchListener.foundTaxonForTerm(null, term, anuraPlant, NameType.SAME_AS);
                    }
                }
            }
        };

        TermMatcher matcher = new TermMatcherHierarchical(matcherBasic);


        AtomicLong countTotal = new AtomicLong(0);
        AtomicLong countMatches = new AtomicLong(0);
        matcher.match(Arrays.asList(new TermImpl("", "Anura")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, Taxon taxon, NameType nameType) {
                countTotal.incrementAndGet();
                if (!NameType.NONE.equals(nameType)) {
                    countMatches.incrementAndGet();
                }
            }
        });

        assertThat(countTotal.get(), Is.is(2L));
        assertThat(countMatches.get(), Is.is(2L));
    }

    @Test
    public void hierarchySynonym() throws PropertyEnricherException {


        TermMatcher matcherBasic = new TermMatcher() {

            @Override
            public void match(List<Term> list, TermMatchListener termMatchListener) throws PropertyEnricherException {
                for (Term term : list) {
                    if ("Anura".equals(term.getName())) {
                        TaxonImpl someTaxon = new TaxonImpl("Bar", "FOO:123");
                        someTaxon.setPath("Foo | Bar");
                        someTaxon.setPathIds("FOO:1 | FOO:123");
                        termMatchListener.foundTaxonForTerm(null, term, someTaxon, NameType.SAME_AS);
                    }
                }
            }
        };

        TermMatcher matcher = new TermMatcherHierarchical(matcherBasic);


        AtomicLong countTotal = new AtomicLong(0);
        AtomicLong countMatches = new AtomicLong(0);
        matcher.match(Arrays.asList(new TermImpl("", "Anura")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, Taxon taxon, NameType nameType) {
                countTotal.incrementAndGet();
                if (!NameType.NONE.equals(nameType)) {
                    countMatches.incrementAndGet();
                }
            }
        });

        assertThat(countTotal.get(), Is.is(1L));
        assertThat(countMatches.get(), Is.is(1L));
    }

}
