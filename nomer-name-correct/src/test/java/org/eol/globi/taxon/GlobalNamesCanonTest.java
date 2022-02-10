package org.eol.globi.taxon;

import org.eol.globi.service.NameSuggester;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class GlobalNamesCanonTest extends NameParserTestBase {

    @Override
    public NameSuggester getNameSuggester() {
        return new GlobalNamesCanon();
    }

    @Test
    public void removeWhitespacesGNBehavior() {
        assertThat(getNameSuggester().suggest("Xanthorhoë"), is("Xanthorhoe"));
        assertThat(getNameSuggester().suggest("Leptochela cf bermudensis"), is("Leptochela bermudensis"));
        assertThat(getNameSuggester().suggest("Archips podana/operana"), is("Archips"));
    }

    @Test
    public void subspeciesGNBehavior() {
        assertThat(getNameSuggester().suggest("Aegathoa oculata ssp. fred"), is("Aegathoa oculata ssp. fred"));
        assertThat(getNameSuggester().suggest("Aegathoa oculata subsp fred"), is("Aegathoa oculata ssp. fred"));
        assertThat(getNameSuggester().suggest("Aegathoa oculata ssp fred"), is("Aegathoa oculata ssp. fred"));
        assertThat(getNameSuggester().suggest("Aegathoa oculata subsp. fred"), is("Aegathoa oculata ssp. fred"));
        assertThat(getNameSuggester().suggest("Aegathoa oculata spp. Aegathoa oculata spp. Aegathoa oculata spp."), is("Aegathoa oculata"));
        assertThat(getNameSuggester().suggest("Acrididae spp. "), is("Acrididae"));
        assertThat(getNameSuggester().suggest("Pleocyemata spp."), is("Pleocyemata"));
    }

    @Test
    public void replaceMultiplyOrXByLowerCaseXGNBehavior() {
        final String expected = "Arthopyrenia hyalospora × Hydnellum scrobiculatum";
        assertThat(getNameSuggester().suggest("Arthopyrenia hyalospora X Hydnellum scrobiculatum"), is(expected));
        assertThat(getNameSuggester().suggest("Lobelia cardinalis x siphilitica"), is("Lobelia cardinalis × siphilitica"));
        assertThat(getNameSuggester().suggest("Arthopyrenia hyalospora x Hydnellum scrobiculatum"), is(expected));
    }

    @Test
    public void repetition() {
        assertThat(getNameSuggester().suggest("Bacteriastrum spp.          Bacteriastrum spp.          Bacteriastrum spp.          Bacteriastrum spp."), is("Bacteriastrum"));
    }

}
