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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsNot.not;

public class SynonymizerTest {


    @Test
    public void noneMatchRetry() throws PropertyEnricherException {


        TermMatcher matcherBasic = new TermMatcher() {

            @Override
            public void match(List<Term> list, TermMatchListener termMatchListener) throws PropertyEnricherException {
                for (Term term : list) {
                    if ("Lycalopex vetulus".equals(term.getName())) {
                        TaxonImpl homoSapiens = new TaxonImpl("Lycalopex vetulus", "FOO:123");
                        termMatchListener.foundTaxonForTerm(null, term, NameType.HAS_ACCEPTED_NAME, homoSapiens);
                    } else {
                        termMatchListener.foundTaxonForTerm(null, term, NameType.NONE, new TaxonImpl(term.getName(), term.getId()));
                    }
                }
            }
        };

        TermMatcher matcher = new Synonymizer(matcherBasic);


        AtomicLong countTotal = new AtomicLong(0);
        AtomicLong synonymMatches = new AtomicLong(0);
        TermImpl vetula = new TermImpl(null, "Lycalopex vetula");
        TermImpl vetulus = new TermImpl(null, "Lycalopex vetulus");
        matcher.match(Arrays.asList(vetula, vetulus), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                countTotal.incrementAndGet();
                if (NameType.SYNONYM_OF.equals(nameType)) {
                    synonymMatches.incrementAndGet();
                }
            }
        });

        assertThat(countTotal.get(), Is.is(2L));
        assertThat(synonymMatches.get(), Is.is(1L));
    }

    @Test
    public void proposeNameAlternate() {
        assertThat(
                Synonymizer.proposeSynonymForUpToTwoNonGenusNameParts("Bla blaii"),
                hasItem("Bla blai")
        );
    }

    @Test
    public void proposeNameAlternate2() {
        assertThat(
                Synonymizer.proposeSynonymForUpToTwoNonGenusNameParts("Bla blai"),
                hasItem("Bla blaii")
        );
    }

    @Test
    public void proposeNameAlternate3() {
        assertThat(
                Synonymizer.proposeSynonymForUpToTwoNonGenusNameParts("Donald duckus"),
                hasItem("Donald ducka")
        );
    }

    @Test
    public void proposeNameAlternate4() {
        assertThat(
                Synonymizer.proposeSynonymForUpToTwoNonGenusNameParts("Donald ducka"),
                hasItem("Donald duckus")
        );
    }

    @Test
    public void proposeNameAlternate44() {
        assertThat(
                Synonymizer.proposeSynonymForUpToTwoNonGenusNameParts("Donald ducka var. ducka"),
                hasItems("Donald duckus var. duckus", "Donald duckus var. ducka")
        );
    }

    @Test
    public void proposeNameAlternate45() {
        assertThat(
                Synonymizer.proposeSynonymForUpToTwoNonGenusNameParts("Donald ducka var. ducka (L. 1758)"),
                hasItems("Donald duckus var. ducka (L. 1758)")
        );
    }

    @Test
    public void proposeNameAlternate5() {
        assertThat(
                Synonymizer.proposeSynonymForUpToTwoNonGenusNameParts("Archaeotherium palustre"),
                hasItem("Archaeotherium palustris")
        );
    }

    @Test
    public void proposeNameAlternate6() {
        assertThat(
                Synonymizer.proposeSynonymForUpToTwoNonGenusNameParts("Archaeotherium palustris"),
                hasItem("Archaeotherium palustre")
        );
    }

    @Test
    public void proposeNameAlternate7() {
        assertThat(
                Synonymizer.proposeSynonymForUpToTwoNonGenusNameParts("Donald major"),
                hasItem("Donald majus")
        );
    }

    @Test
    public void proposeNameAlternate9() {
        assertThat(Synonymizer.proposeSynonymForUpToTwoNonGenusNameParts("Carollia brevicauda"), hasItem("Carollia brevicaudum"));
    }

    @Test
    public void proposeNameAlternate10() {
        assertThat(
                Synonymizer.proposeSynonymForUpToTwoNonGenusNameParts("Carollia brevicaudum colombiana"),
                hasItem("Carollia brevicauda colombiana")
        );

    }

    @Test
    public void proposeNameAlternate8() {
        List<String> alternate = Synonymizer.proposeSynonymForUpToTwoNonGenusNameParts("Donald majus");
        assertThat(
                alternate,
                hasItems("Donald major", "Donald maja", "Donald majum")
        );
        assertThat(
                alternate.size(),
                Is.is(3)
        );
    }

    @Test
    public void proposeNameAlternatePeromyscus_boylei() {
        List<String> alternate = Synonymizer.proposeSynonymForUpToTwoNonGenusNameParts("Peromyscus boylei");
        assertThat(
                alternate,
                hasItems("Peromyscus boylii")
        );
    }

    @Test
    public void proposeNameAlternatePlecotus_christii () {
        List<String> alternate = Synonymizer.proposeSynonymForUpToTwoNonGenusNameParts("Plecotus christii");
        assertThat(
                alternate,
                hasItems("Plecotus christiei")
        );
    }

    @Test
    public void proposeNameAlternatePteropus_gilliardi () {
        List<String> alternate = Synonymizer.proposeSynonymForUpToTwoNonGenusNameParts("Pteropus gilliardi");
        assertThat(
                alternate,
                hasItems("Pteropus gilliardorum")
        );
    }

    @Test
    public void proposeNameAlternateCrocidura_greenwoodae () {
        List<String> alternate = Synonymizer.proposeSynonymForUpToTwoNonGenusNameParts("Crocidura greenwoodae");
        assertThat(
                alternate,
                hasItems("Crocidura greenwoodi")
        );
    }

    @Test
    public void proposeNameAlternateCrocidura_greenwoodi () {
        List<String> alternate = Synonymizer.proposeSynonymForUpToTwoNonGenusNameParts("Crocidura greenwoodi");
        assertThat(
                alternate,
                hasItems("Crocidura greenwoodae")
        );
    }

    @Test
    public void superLong() {
        List<String> alternate = Synonymizer.proposeSynonymForUpToTwoNonGenusNameParts("Donald majus bla blaus bla bla bla bla");
        assertThat(
                alternate,
                hasItem("Donald maja blus blaus bla bla bla bla")
        );
        assertThat(
                alternate,
                not(hasItem("Donald maja blus blaus bla bla bla blus"))
        );

        assertThat(
                alternate.size(),
                Is.is(11)
        );
    }

    @Test
    public void noCapitalizedGenus() {
        List<String> alternate = Synonymizer.proposeSynonymForUpToTwoNonGenusNameParts("donald majus bla blaus bla bla bla bla");
        assertThat(
                alternate.size(),
                Is.is(0)
        );
    }

}