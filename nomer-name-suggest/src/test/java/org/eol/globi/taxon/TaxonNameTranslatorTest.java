package org.eol.globi.taxon;

import org.apache.commons.lang3.tuple.Triple;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class TaxonNameTranslatorTest {

    List<Triple<Term, NameType, Taxon>> matches = new ArrayList<>();


    @Test
    public void genderVariationsUsA() throws PropertyEnricherException {
        TaxonNameTranslator translator = new TaxonNameTranslator();
        translator.match(Arrays.asList(new TermImpl("someId", "Some namus")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                matches.add(Triple.of(term, nameType, taxon));
            }
        });

        assertThat(matches.size(), is(2));
        assertThat(matches.get(0).getLeft().getName(), is("Some namus"));
        assertThat(matches.get(0).getMiddle(), is(NameType.SAME_AS));
        assertThat(matches.get(0).getRight().getName(), is("Some namus"));

        assertThat(matches.get(1).getLeft().getName(), is("Some namus"));
        assertThat(matches.get(1).getMiddle(), is(NameType.SAME_AS));
        assertThat(matches.get(1).getRight().getName(), is("Some nama"));

    }

    @Test
    public void genderVariationsA_to_US() throws PropertyEnricherException {
        TaxonNameTranslator translator = new TaxonNameTranslator();
        translator.match(Arrays.asList(new TermImpl("someId", "Some nama")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                matches.add(Triple.of(term, nameType, taxon));
            }
        });

        assertThat(matches.size(), is(2));
        assertThat(matches.get(0).getLeft().getName(), is("Some nama"));
        assertThat(matches.get(0).getMiddle(), is(NameType.SAME_AS));
        assertThat(matches.get(0).getRight().getName(), is("Some nama"));

        assertThat(matches.get(1).getLeft().getName(), is("Some nama"));
        assertThat(matches.get(1).getMiddle(), is(NameType.SAME_AS));
        assertThat(matches.get(1).getRight().getName(), is("Some namus"));

    }


    @Test
    public void iSuffixVariations() throws PropertyEnricherException {
        TaxonNameTranslator translator = new TaxonNameTranslator();
        translator.match(Arrays.asList(new TermImpl("someId", "Some nami")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                matches.add(Triple.of(term, nameType, taxon));
            }
        });

        assertThat(matches.size(), is(2));
        assertThat(matches.get(0).getLeft().getName(), is("Some nami"));
        assertThat(matches.get(0).getMiddle(), is(NameType.SAME_AS));
        assertThat(matches.get(0).getRight().getName(), is("Some nami"));

        assertThat(matches.get(1).getLeft().getName(), is("Some nami"));
        assertThat(matches.get(1).getMiddle(), is(NameType.SAME_AS));
        assertThat(matches.get(1).getRight().getName(), is("Some namii"));

    }


    @Test
    public void iiSuffixVariations() throws PropertyEnricherException {
        TaxonNameTranslator translator = new TaxonNameTranslator();
        translator.match(Arrays.asList(new TermImpl("someId", "Some namii")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                matches.add(Triple.of(term, nameType, taxon));
            }
        });

        assertThat(matches.size(), is(2));
        assertThat(matches.get(0).getLeft().getName(), is("Some namii"));
        assertThat(matches.get(0).getMiddle(), is(NameType.SAME_AS));
        assertThat(matches.get(0).getRight().getName(), is("Some namii"));

        assertThat(matches.get(1).getLeft().getName(), is("Some namii"));
        assertThat(matches.get(1).getMiddle(), is(NameType.SAME_AS));
        assertThat(matches.get(1).getRight().getName(), is("Some nami"));

    }

}
