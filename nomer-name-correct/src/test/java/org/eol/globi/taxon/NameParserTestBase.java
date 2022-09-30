package org.eol.globi.taxon;

import org.eol.globi.service.NameSuggester;
import org.hamcrest.core.Is;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public abstract class NameParserTestBase {

    abstract public NameSuggester getNameSuggester();

    @Test
    public void parseHumans() {
        assertThat(getNameSuggester().suggest("Homo sapiens Linneaus, 1758"),
                Is.is("Homo sapiens"));
    }

    @Test
    public void parseBigString() {
        // https://github.com/globalbioticinteractions/nomer/issues/89
        assertThat(getNameSuggester().suggest("Reithrodontomys FULVESCENS GRISEOFLAVUS"),
                Is.is("Reithrodontomys"));
    }

    @Test
    public void subspecies() {
        assertThat(getNameSuggester().suggest("Aegathoa oculata spp"), is("Aegathoa oculata"));
        assertThat(getNameSuggester().suggest("Aegathoa oculata spp."), is("Aegathoa oculata"));
    }

    @Test
    public void species() {
        assertThat(getNameSuggester().suggest("Aegathoa oculata sp."), is("Aegathoa oculata"));
    }


    @Test
    public void varieties() {
        assertThat(getNameSuggester().suggest("Puccinia dioicae var. dioicae"), is("Puccinia dioicae var. dioicae"));
        assertThat(getNameSuggester().suggest("Puccinia dioicae var dioicae"), is("Puccinia dioicae var. dioicae"));
    }

    @Test
    public void removeWhitespaces() {
        assertThat(getNameSuggester().suggest("Aegathoa oculata "), is("Aegathoa oculata"));
        assertThat(getNameSuggester().suggest("Blbua blas "), is("Blbua blas"));
        assertThat(getNameSuggester().suggest("Blbua  blas  "), is("Blbua blas"));
        assertThat(getNameSuggester().suggest("Blbua  blas (akjhaskdjhf)"), is("Blbua blas"));
        assertThat(getNameSuggester().suggest("  Blbua  blas (akjhaskdjhf)"), is("Blbua blas"));
        assertThat(getNameSuggester().suggest("Taraxacum leptaleum"), is("Taraxacum leptaleum"));

        assertThat(getNameSuggester().suggest("Mycosphaerella filipendulae-denudatae"), is("Mycosphaerella filipendulae-denudatae"));


        //assertThat(normalizer.normalize("Armeria 'Bees Ruby'"), is("Armeria 'Bees Ruby'"));


        assertThat(getNameSuggester().suggest("Limonia (Dicranomyia) chorea"), is("Limonia chorea"));

    }

    @Test
    @Ignore
    public void dubiousAssertions() {
        assertThat(getNameSuggester().suggest("Rubus fruticosus agg."), is("Rubus fruticosus agg."));
        // Malcolm Storey technique to distinguish duplicate genus names in taxonomies.
        assertThat(getNameSuggester().suggest("Ammophila (Bot.)"), is("Ammophila (Bot.)"));
        assertThat(getNameSuggester().suggest("Ammophila (Zool.)"), is("Ammophila (Zool.)"));
        assertThat(getNameSuggester().suggest("Ammophila (zoo)"), is("Ammophila (zoo)"));
        assertThat(getNameSuggester().suggest("Ammophila (bot)"), is("Ammophila (bot)"));
        assertThat(getNameSuggester().suggest("Ammophila (bot.)"), is("Ammophila (bot.)"));
        assertThat(getNameSuggester().suggest("Ammophila (Bot)"), is("Ammophila (Bot)"));
        assertThat(getNameSuggester().suggest("Ammophila (blah)"), is("Ammophila"));

    }

    /**
     * In scientific names of hybrids (e.g. Erysimum decumbens x perofskianum, http://eol.org/pages/5145889),
     * the \u00D7 or × (multiply) symbol should be used. However, most data sources simply use lower case "x",
     * and don't support the × symbol is their name matching methods.
     */

    @Test
    public void replaceMultiplyOrXByLowerCaseX() {
        final String expected = "Arthopyrenia hyalospora × Hydnellum scrobiculatum";
        assertThat(getNameSuggester().suggest("Arthopyrenia hyalospora \u00D7 Hydnellum scrobiculatum"), is(expected));
    }

    @Test
    public void batVirusName() {
        // see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/672#issuecomment-867149488
        assertThat(getNameSuggester().suggest("Bat SARS CoV"), is("Bat SARS CoV"));
    }

    @Test
    public void batVirusName2() {
        // see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/672#issuecomment-867149488
        assertThat(getNameSuggester().suggest("Homo sapiensvirus Linneaus, 1758"), is("Homo sapiensvirus Linneaus, 1758"));
    }

}
