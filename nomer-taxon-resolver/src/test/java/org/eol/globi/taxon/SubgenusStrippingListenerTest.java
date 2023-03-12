package org.eol.globi.taxon;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

public class SubgenusStrippingListenerTest {

    @Test
    public void nameWithSubgenus() {
        List<Term> terms = new ArrayList<>();
        SubgenusStrippingListener listener = new SubgenusStrippingListener(new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                terms.add(term);
            }
        });

        listener.foundTaxonForTerm(null,
                new TaxonImpl("Pseudopanurgus (Heterosarus) parvus"),
                NameType.SYNONYM_OF,
                new TaxonImpl("Donaldus ducki"));

        assertThat(terms.size(), Is.is(2));

        assertThat(terms.get(0).getName(), Is.is("Pseudopanurgus parvus"));
        assertThat(terms.get(1).getName(), Is.is("Pseudopanurgus (Heterosarus) parvus"));
    }

    @Test
    public void nameWithoutSubgenus() {
        List<Term> terms = new ArrayList<>();
        SubgenusStrippingListener listener = new SubgenusStrippingListener(new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                terms.add(term);
            }
        });

        listener.foundTaxonForTerm(null,
                new TaxonImpl("Pseudopanurgus parvus"),
                NameType.SYNONYM_OF,
                new TaxonImpl("Donaldus ducki"));

        assertThat(terms.size(), Is.is(1));

        assertThat(terms.get(0).getName(), Is.is("Pseudopanurgus parvus"));
    }

}