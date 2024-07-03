package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CapitalizerTest {

    @Test
    public void undoLowerCase() {
        String corrected = Capitalizer.capitalize("homo sapiens");
        assertThat(corrected, is("Homo sapiens"));
    }

    @Test
    public void undoLowerCase2() {
        String corrected = Capitalizer.capitalize("io");
        assertThat(corrected, is("Io"));
    }

    @Test
    public void doNothing() {
        String corrected = Capitalizer.capitalize("Homo Sapiens");
        assertThat(corrected, is("Homo Sapiens"));
    }

    @Test(timeout = 10)
    public void longName() {
        String aLongName = "7 small acorns or other seed found in cheek pouches during preparation. Seeds are brown, pointed at one end and flattened on the other, 0.75 x 0.6cm.";
        String corrected = Capitalizer.capitalize(aLongName);
        assertThat(corrected, is(aLongName));
    }

    @Ignore("this test fails. see https://github.com/globalbioticinteractions/nomer/issues/182")
    @Test(timeout = 10)
    public void capitalizeLongName() {
        String aLongName = "acorns or   found  cheek pouches during preparation   are brown  pointed at  end  flattened  the   x  cm";
        String corrected = StringUtils.capitalize(aLongName);
        assertThat(corrected, is(aLongName));
    }

    @Test(timeout = 10)
    public void anotherLongName() {
        String anotherLongName = "Acorns or   found  cheek pouches during preparation   are brown  pointed at  end  flattened  the   x  cm";
        String corrected
                = Capitalizer.capitalize(anotherLongName);
        assertThat(corrected, is(anotherLongName));
    }

    @Test(timeout = 10)
    public void yetAnotherLongName() {
        String anotherLongName = "Acorns or found cheek pouches during preparation are brown pointed at end flattened the x cm";
        String corrected
                = Capitalizer.capitalize(anotherLongName);
        assertThat(corrected, is(anotherLongName));
    }


}