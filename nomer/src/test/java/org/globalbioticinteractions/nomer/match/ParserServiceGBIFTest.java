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
import static org.hamcrest.MatcherAssert.assertThat;

public class ParserServiceGBIFTest extends ParserServiceTestAbstract {

    @Override
    protected ParserServiceAbstract getParserService() {
        return new ParserServiceGBIF();
    }


    @Test
    public void nameWithSubgenusAndAuthorship() throws PropertyEnricherException {

        AtomicBoolean foundMatch = new AtomicBoolean(false);
        getParserService().match(Arrays.asList(new TermImpl("someId", "Andrena (Aenandrena) aeneiventris Morawitz, 1872")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                assertThat(nameType, Is.is(NameType.SAME_AS));
                assertThat(term.getName(), Is.is("Andrena (Aenandrena) aeneiventris Morawitz, 1872"));
                assertThat(taxon.getName(), Is.is("Andrena aeneiventris"));
                assertThat(taxon.getAuthorship(), Is.is("Morawitz, 1872"));
                assertThat(taxon.getRank(), Is.is("species"));
                assertThat(taxon.getPath(), Is.is("Andrena | Aenandrena | aeneiventris"));
                assertThat(taxon.getPathNames(), Is.is("genus | infragenericEpithet | specificEpithet"));
                foundMatch.set(true);
            }
        });

        assertTrue(foundMatch.get());
    }



}