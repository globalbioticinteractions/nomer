package org.globalbioticinteractions.nomer.util;

import org.eol.globi.domain.TaxonImpl;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class AppenderUtilTest {

    @Test
    public void getKingdomFromPath() {
        TaxonImpl taxon = new TaxonImpl("someName", "someId");
        taxon.setPathNames("kingdom | species");
        taxon.setPath("someKingdom | someSpecies");
        taxon.setPathIds("foo:1 | foo:2");
        String kingdomName = AppenderUtil.valueForTaxonProperty(
                taxon,
                "path.kingdom.name");

        assertThat(kingdomName, is("someKingdom"));
    }

    @Test
    public void getUnknownKingdomFromPath() {
        TaxonImpl taxon = new TaxonImpl("someName", "someId");
        taxon.setPathNames("genus | species");
        taxon.setPath("someGenus | someSpecies");
        taxon.setPathIds("foo:1 | foo:2");
        String kingdomName = AppenderUtil.valueForTaxonProperty(
                taxon,
                "path.kingdom.name");

        assertThat(kingdomName, is(""));
    }

    @Test
    public void getKnownKingdomFromPath() {
        TaxonImpl taxon = new TaxonImpl("someName", "someId");
        taxon.setPathNames("kingdom | genus | species");
        taxon.setPath("someKingdom | someGenus | someSpecies");
        taxon.setPathIds("foo:0 | foo:1 | foo:2");
        taxon.setPathAuthorships("Doe 2013 | Smith 2031 | Johnson 2012");
        String kingdomName = AppenderUtil.valueForTaxonProperty(
                taxon,
                "path.kingdom.name");

        assertThat(kingdomName, is("someKingdom"));

        String authorship = AppenderUtil.valueForTaxonProperty(
                taxon,
                "path.kingdom.authorship");

        assertThat(authorship, is("Doe 2013"));
    }

    @Test   
    public void getUnknownAuthorship() {
        TaxonImpl taxon = new TaxonImpl("someName", "someId");
        taxon.setPathNames("kingdom | species");
        taxon.setPath("someKingdom | someSpecies");
        taxon.setPathIds("foo:1 | foo:2");
        String authorship = AppenderUtil.valueForTaxonProperty(
                taxon,
                "authorship");

        assertThat(authorship, is(nullValue()));
    }

    @Test
    public void getExternalIdFromPath() {
        TaxonImpl taxon = new TaxonImpl("someName", "someId");
        taxon.setPathNames("kingdom | species");
        taxon.setPath("someKingdom | someSpecies");
        taxon.setPathIds("foo:1 | foo:2");
        String kingdomName = AppenderUtil.valueForTaxonPropertyName(
                taxon,
                "externalId");

        assertThat(kingdomName, is("someId"));
    }

    @Test
    public void getPathFromTaxon() {
        TaxonImpl taxon = new TaxonImpl("someName", "someId");
        taxon.setPath("someKingdom | someSpecies");
        String kingdomName = AppenderUtil.valueForTaxonProperty(
                taxon,
                "path.name");

        assertThat(kingdomName, is("someKingdom | someSpecies"));
    }

    @Test
    public void getPathIdsFromTaxon() {
        TaxonImpl taxon = new TaxonImpl("someName", "someId");
        taxon.setPathIds("foo:1 | foo:2");
        String pathIds = AppenderUtil.valueForTaxonProperty(
                taxon,
                "path.id");

        assertThat(pathIds, is("foo:1 | foo:2"));
    }

    @Test
    public void getAuthorshipFromTaxon() {
        TaxonImpl taxon = new TaxonImpl("someName", "someId");
        taxon.setAuthorship("Duck 1931");
        String pathIds = AppenderUtil.valueForTaxonProperty(
                taxon,
                "authorship");

        assertThat(pathIds, is("Duck 1931"));
    }


}