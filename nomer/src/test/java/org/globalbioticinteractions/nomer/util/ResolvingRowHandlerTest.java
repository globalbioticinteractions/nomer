package org.globalbioticinteractions.nomer.util;

import org.apache.commons.io.IOUtils;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.globalbioticinteractions.nomer.util.MatchTestUtil.*;
import static org.globalbioticinteractions.nomer.util.MatchTestUtil.contextAppendTerms;
import static org.globalbioticinteractions.nomer.util.MatchTestUtil.createTaxonCacheService;
import static org.junit.Assert.assertThat;

public class ResolvingRowHandlerTest {

    @Test
    public void resolve() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("NCBI:9606\tHomo sapiens");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final PropertyEnricher enricher = new PropertyEnricherMatch();
        MatchUtil.resolve(is, new ResolvingRowHandler(os, enricher, contextAppendTerms()));
        assertThat(os.toString(), Is.is("NCBI:9606\tHomo sapiens\tSAME_AS\tNCBI:9606\tHomo sapiens\t\t\tone | two\t\t\thttps://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=9606\t\tA name source\thttp://example.org\t1970-01-01T00:00:00Z\n"));
    }


    @Test
    public void resolveTaxonCacheNoId() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("\tHomo sapiens");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final PropertyEnricher enricher = createTaxonCacheService();
        MatchUtil.resolve(is, new ResolvingRowHandler(os, enricher, contextAppendTerms()));
        assertThat(os.toString(), Is.is("\tHomo sapiens\tSAME_AS\tEOL:327955\tHomo sapiens\tSpecies\tإنسان @ar | Insan @az | човешки @bg | মানবীয় @bn | Ljudsko biće @bs | Humà @ca | Muž @cs | Menneske @da | Mensch @de | ανθρώπινο ον @el | Humans @en | Humano @es | Gizakiaren @eu | Ihminen @fi | Homme @fr | Mutum @ha | אנושי @he | մարդու @hy | Umano @it | ადამიანის @ka | Homo @la | žmogaus @lt | Om @mo | Mens @nl | Òme @oc | Om @ro | Человек разумный современный @ru | Qenie Njerëzore @sq | மனிதன் @ta | మానవుడు @te | Aadmi @ur | umuntu @zu |\tAnimalia | Bilateria | Deuterostomia | Chordata | Vertebrata | Gnathostomata | Tetrapoda | Mammalia | Theria | Eutheria | Primates | Haplorrhini | Simiiformes | Hominoidea | Hominidae | Homininae | Homo | Homo sapiens\tEOL:1 | EOL:3014411 | EOL:8814528 | EOL:694 | EOL:2774383 | EOL:12094272 | EOL:4712200 | EOL:1642 | EOL:57446 | EOL:2844801 | EOL:1645 | EOL:10487985 | EOL:10509493 | EOL:4529848 | EOL:1653 | EOL:10551052 | EOL:42268 | EOL:327955\tkingdom | subkingdom | infrakingdom | division | subdivision | infraphylum | superclass | class | subclass | infraclass | order | suborder | infraorder | superfamily | family | subfamily | genus | species\thttp://eol.org/pages/327955\thttp://media.eol.org/content/2014/08/07/23/02836_98_68.jpg\t\t\t\n"));
    }


    @Test
    public void resolveAppend() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("NCBI:9606\tHomo sapiens\tone");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final PropertyEnricher enricher = new PropertyEnricherMatch();
        MatchUtil.resolve(is, new ResolvingRowHandler(os, enricher, contextAppendTerms()));
        assertThat(os.toString(), Is.is("NCBI:9606\tHomo sapiens\tone\tSAME_AS\tNCBI:9606\tHomo sapiens\t\t\tone | two\t\t\thttps://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=9606\t\tA name source\thttp://example.org\t1970-01-01T00:00:00Z\n"));
    }

    @Test
    public void resolveReplace() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("NCBI:9606\tHomo sapiens\tone");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final PropertyEnricherPassThrough enricher = new PropertyEnricherPassThrough();
        MatchUtil.resolve(is, new ResolvingRowHandler(os, enricher, contextReplaceTerms()));
        assertThat(os.toString(), Is.is("NCBI:9606\tHomo sapiens\tone\n"));
    }



}
