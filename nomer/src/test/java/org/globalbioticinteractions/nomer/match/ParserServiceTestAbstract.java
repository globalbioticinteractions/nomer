package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatchListener;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class ParserServiceTestAbstract {


    @Test
    public void nameWithAuthorship() throws PropertyEnricherException {

        AtomicBoolean foundMatch = new AtomicBoolean(false);
        getParserService().match(Arrays.asList(new TermImpl("someId", "Homo sapiens L.")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                assertThat(nameType, Is.is(NameType.SAME_AS));
                assertThat(term.getName(), Is.is("Homo sapiens L."));
                assertThat(taxon.getName(), Is.is("Homo sapiens"));
                assertThat(taxon.getAuthorship(), Is.is("L."));
                foundMatch.set(true);
            }
        });

        assertTrue(foundMatch.get());
    }

    protected abstract ParserServiceAbstract getParserService();

    @Test
    public void nameWithoutAuthorship() throws PropertyEnricherException {

        AtomicBoolean foundMatch = new AtomicBoolean(true);
        getParserService().match(Arrays.asList(new TermImpl("someId", "Homo sapiens")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                assertThat(nameType, Is.is(NameType.SAME_AS));
                assertThat(term.getName(), Is.is("Homo sapiens"));
                assertThat(taxon.getName(), Is.is("Homo sapiens"));
                assertThat(taxon.getAuthorship(), Is.is(nullValue()));
                foundMatch.set(true);
            }
        });

        assertTrue(foundMatch.get());
    }

    @Test
    public void ignoreViralName() throws PropertyEnricherException {

        AtomicBoolean foundMatch = new AtomicBoolean(false);
        getParserService().match(Arrays.asList(new TermImpl("someId", "Homo sapiens crazy corona virus")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                assertThat(nameType, Is.is(NameType.NONE));
                assertThat(term.getName(), Is.is("Homo sapiens crazy corona virus"));
                assertThat(taxon.getName(), Is.is("Homo sapiens crazy corona virus"));
                assertThat(taxon.getAuthorship(), Is.is(nullValue()));
                foundMatch.set(true);
            }
        });

        assertTrue(foundMatch.get());
    }

}