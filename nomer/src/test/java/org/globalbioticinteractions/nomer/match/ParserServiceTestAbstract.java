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
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;

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

    @Test
    public void unparsableName() throws PropertyEnricherException {

        AtomicBoolean foundMatch = new AtomicBoolean(false);
        getParserService().match(Arrays.asList(new TermImpl("someId", "ad64ad57 a281 e b398 dddc7e953")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                assertThat(nameType, Is.is(NameType.SAME_AS));
                assertThat(term.getName(), Is.is("ad64ad57 a281 e b398 dddc7e953"));
                assertThat(term.getId(), Is.is("someId"));
                assertThat(taxon.getName(), Is.is("ad64ad57 a281 e b398 dddc7e953"));
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

    @Test
    public void ignoreNulName() throws PropertyEnricherException {

        AtomicBoolean foundMatch = new AtomicBoolean(false);
        getParserService().match(Collections.singletonList(new TermImpl("someId", null)), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                foundMatch.set(true);
            }
        });

        assertFalse(foundMatch.get());
    }

    @Test
    public void nameWithSubgenus() throws PropertyEnricherException {

        AtomicBoolean foundMatch = new AtomicBoolean(false);
        getParserService().match(Arrays.asList(new TermImpl("someId", "Pteronotus (Chilonycteris) macleayii")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                assertThat(nameType, Is.is(NameType.SAME_AS));
                assertThat(term.getName(), Is.is("Pteronotus (Chilonycteris) macleayii"));
                assertThat(taxon.getName(), Is.is("Pteronotus macleayii"));
                foundMatch.set(true);
            }
        });

        assertTrue(foundMatch.get());
    }


}