package org.eol.globi.taxon;


import org.eol.globi.domain.Taxon;
import org.eol.globi.service.PropertyEnricherException;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ParserServiceDiscoverLifeCustomTest {

    private static Taxon parse(String name) {
        try {
            return new ParserServiceDiscoverLifeCustom().parse(null, name);
        } catch (PropertyEnricherException e) {
            return null;
        }
    }

    @Test
    public void parseName() {
        Taxon matched = parse("Pterandrena aliciae (Robertson, 1891)");

        assertNotNull(matched);
        assertThat(matched.getName(), is("Pterandrena aliciae"));
        assertThat(matched.getAuthorship(), is("(Robertson, 1891)"));
    }

    @Test
    public void parseNameAlt1() {
        String name = "Acamptopoeum colombiensis_sic Shinn, 1965";

        Taxon matched = parse(name);
        assertThat(matched.getName(), is("Acamptopoeum colombiensis"));
        assertThat(matched.getAuthorship(), is("Shinn, 1965"));
    }

    @Test
    public void parseNameAlt2() {
        Taxon matched = parse("Camptopoeum (Acamptopoeum) nigritarse Vachal, 1909");
        assertThat(matched.getName(), is("Camptopoeum (Acamptopoeum) nigritarse"));
        assertThat(matched.getAuthorship(), is("Vachal, 1909"));

    }

    @Test
    public void parseNameAlt3() {
        Taxon matched = parse("Allodapula minor Michener and Syed, 1962");
        assertThat(matched.getName(), is("Allodapula minor"));
        assertThat(matched.getAuthorship(), is("Michener and Syed, 1962"));
    }

    @Test
    public void parseNameAlt4() {
        Taxon matched = parse("Zadontomerus metallica (H. S. Smith, 1907)");
        assertThat(matched.getName(), is("Zadontomerus metallica"));
        assertThat(matched.getAuthorship(), is("(H. S. Smith, 1907)"));

    }

    @Test
    public void parseNameAlt5() {
        Taxon matched = parse("Agapostemon texanus subtilior Cockerell, 1898");
        assertThat(matched.getName(), is("Agapostemon texanus subtilior"));
        assertThat(matched.getAuthorship(), is("Cockerell, 1898"));

    }

    @Test
    public void parseNameAlt6() {
        Taxon matched = parse("Andena squamigera_homonym Bramson, 1879");
        assertThat(matched.getName(), is("Andena squamigera"));
        assertThat(matched.getAuthorship(), is("Bramson, 1879"));

    }


}