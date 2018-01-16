package org.globalbioticinteractions.nomer.util;

import org.apache.commons.io.IOUtils;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.PropertyEnricherFactory;
import org.eol.globi.taxon.GlobalNamesService;
import org.eol.globi.taxon.GlobalNamesSources;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.taxon.TermMatcher;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;

public class TermMatchingRowHandlerTest {

    @Test
    public void resolveWithEnricher() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("NCBI:9606\tHomo sapiens");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final TermMatcher matcher = PropertyEnricherFactory.createTaxonMatcher(null);
        MatchUtil.resolve(is, new TermMatchingRowHandler(os, matcher, new MatchTestUtil.TermMatcherContextDefault() {
        }));
        assertThat(os.toString(), startsWith("NCBI:9606\tHomo sapiens\tSAME_AS\tNCBI:9606\tHomo sapiens\tspecies\tman @en | human @en\t"));
    }

    @Test
    public void resolveTaxonCache() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("EOL:327955\tHomo sapiens");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final TermMatcher matcher = MatchTestUtil.createTaxonCacheService();
        MatchUtil.resolve(is, new TermMatchingRowHandler(os, matcher, new MatchTestUtil.TermMatcherContextDefault() {
        }));
        String[] lines = os.toString().split("\n");
        assertThat(lines[0], startsWith("EOL:327955\tHomo sapiens\tSAME_AS\tEOL:327955\tHomo sapiens\tSpecies\tإنسان @ar | Insan @az | човешки @bg | মানবীয় @bn | Ljudsko biće @bs | Humà @ca | Muž @cs | Menneske @da | Mensch @de | ανθρώπινο ον @el | Humans @en | Humano @es | Gizakiaren @eu | Ihminen @fi | Homme @fr | Mutum @ha | אנושי @he | մարդու @hy | Umano @it | ადამიანის @ka | Homo @la | žmogaus @lt | Om @mo | Mens @nl | Òme @oc | Om @ro | Человек разумный современный @ru | Qenie Njerëzore @sq | மனிதன் @ta | మానవుడు @te | Aadmi @ur | umuntu @zu |\tAnimalia | Bilateria | Deuterostomia | Chordata | Vertebrata | Gnathostomata | Tetrapoda | Mammalia | Theria | Eutheria | Primates | Haplorrhini | Simiiformes | Hominoidea | Hominidae | Homininae | Homo | Homo sapiens\tEOL:1 | EOL:3014411 | EOL:8814528 | EOL:694 | EOL:2774383 | EOL:12094272 | EOL:4712200 | EOL:1642 | EOL:57446 | EOL:2844801 | EOL:1645 | EOL:10487985 | EOL:10509493 | EOL:4529848 | EOL:1653 | EOL:10551052 | EOL:42268 | EOL:327955\tkingdom | subkingdom | infrakingdom | division | subdivision | infraphylum | superclass | class | subclass | infraclass | order | suborder | infraorder | superfamily | family | subfamily | genus | species\thttp://eol.org/pages/327955\thttp://media.eol.org/content/2014/08/07/23/02836_98_68.jpg"));
        assertThat(lines.length, Is.is(2));
        assertThat(lines[1], startsWith("EOL:327955\tHomo sapiens\tSAME_AS\tNCBI:9606\tHomo sapiens\tspecies\t"));
    }


    @Test
    public void resolveTaxonCacheMatchFirstLine() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("EOL:1276240\tHomo sapiens");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final TermMatcher matcher = new TaxonCacheService("classpath:/org/eol/globi/taxon/taxonCache.tsv.gz", "classpath:/org/eol/globi/taxon/taxonMap.tsv.gz");
        MatchUtil.resolve(is, new TermMatchingRowHandler(os, matcher, new MatchTestUtil.TermMatcherContextDefault() {
        }));
        String[] lines = os.toString().split("\n");
        assertThat(lines.length, Is.is(1));
        assertThat(lines[0], startsWith("EOL:1276240\tHomo sapiens\tSAME_AS\tEOL:1276240\tAnas crecca carolinensis"));
    }

    @Test
    public void resolveTaxonCacheMatchFirstLineWithNonDefaultSchema() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("a scrub\ta tree\tEOL:1276240\tHomo sapiens\ta bone");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final TermMatcher matcher = new TaxonCacheService("classpath:/org/eol/globi/taxon/taxonCache.tsv.gz", "classpath:/org/eol/globi/taxon/taxonMap.tsv.gz");
        MatchUtil.resolve(is, new TermMatchingRowHandler(os, matcher, new MatchTestUtil.TermMatcherContextDefault() {
            @Override
            public Map<Integer, String> getInputSchema() {
                return new TreeMap<Integer, String>() {{
                    put(2, "externalId");
                    put(3, "name");
                }};
            }
        }));
        String[] lines = os.toString().split("\n");
        assertThat(lines.length, Is.is(1));
        assertThat(lines[0], startsWith("a scrub\ta tree\tEOL:1276240\tHomo sapiens\ta bone\tSAME_AS\tEOL:1276240\tAnas crecca carolinensis"));
    }

    @Test
    public void resolveGlobalNamesAppendFuzzyMatch() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("\tHomo saliens\tone");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MatchUtil.resolve(is, new TermMatchingRowHandler(os, new GlobalNamesService(), new MatchTestUtil.TermMatcherContextDefault() {
        }));
        assertThat(os.toString(), containsString("\tHomo saliens\tone\tSIMILAR_TO\t"));
    }

    @Test
    public void resolveGlobalNamesBatchAppend() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("NCBI:9606\tHomo sapiens\tone");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MatchUtil.resolve(is, new TermMatchingRowHandler(os, new GlobalNamesService(GlobalNamesSources.NCBI), new MatchTestUtil.TermMatcherContextDefault() {
        }));
        assertThat(os.toString(), containsString("Mammalia"));
        assertThat(os.toString(), containsString("nih.gov"));
    }

    @Test
    public void resolveGlobalNamesBatchAppendNoMatchName() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("NCBI:9606\tDonald duck\tone");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MatchUtil.resolve(is, new TermMatchingRowHandler(os, new GlobalNamesService(GlobalNamesSources.NCBI), new MatchTestUtil.TermMatcherContextDefault() {
        }));
        assertThat(os.toString(), startsWith("NCBI:9606\tDonald duck\tone\tNONE\t\tDonald duck"));
    }

}
