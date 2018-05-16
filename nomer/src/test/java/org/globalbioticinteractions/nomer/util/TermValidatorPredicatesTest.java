package org.globalbioticinteractions.nomer.util;

import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.CSVTSVUtil;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.util.function.Predicate;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TermValidatorPredicatesTest {

    @Test
    public void supportedTermCacheId() throws PropertyEnricherException, IOException {
        Predicate<String> unsupportedId = TermValidatorPredicates.SUPPORTED_ID;
        assertThat(unsupportedId.test("EOL:123\tbla\tBoo"), is(true));
        assertThat(unsupportedId.test("BLAH:123\t"), is(false));
    }

    @Test
    public void consistentTermCachePath() throws PropertyEnricherException, IOException {
        assertTrue(TermValidatorPredicates.CONSISTENT_PATH.test("EOL:1276240\tAnas crecca carolinensis\tInfraspecies\tGreen-winged Teal @en | Sarcelle à ailes vertes @fr | Amerikaanse Wintertaling @nl | \tAnimalia | Chordata | Aves | Anseriformes | Anatidae | Anas | Anas crecca | Anas crecca carolinensis\tEOL:1 | EOL:694 | EOL:695 | EOL:8024 | EOL:8027 | EOL:17930 | EOL:1048951 | EOL:1276240\tkingdom | phylum | class | order | family | genus | species | infraspecies\thttp://eol.org/pages/1276240\thttp://media.eol.org/content/2012/11/04/08/35791_98_68.jpg"));
        assertFalse(TermValidatorPredicates.CONSISTENT_PATH.test("EOL:1276240\tAnas crecca carolinensis\tInfraspecies\tGreen-winged Teal @en | Sarcelle à ailes vertes @fr | Amerikaanse Wintertaling @nl | \tAnimalia | Chordata | Aves | Anseriformes | Anatidae | Anas crecca | Anas crecca carolinensis\tEOL:1 | EOL:694 | EOL:695 | EOL:8024 | EOL:8027 | EOL:17930 | EOL:1048951 | EOL:1276240\tkingdom | phylum | class | order | family | genus | species | infraspecies\thttp://eol.org/pages/1276240\thttp://media.eol.org/content/2012/11/04/08/35791_98_68.jpg"));
        assertTrue(TermValidatorPredicates.CONSISTENT_PATH.test("EOL:1276240\tAnas crecca carolinensis\tInfraspecies\tGreen-winged Teal @en | Sarcelle à ailes vertes @fr | Amerikaanse Wintertaling @nl | \tAnimalia | Chordata | Aves | Anseriformes | Anatidae | Anas | Anas crecca | Anas crecca carolinensis\tEOL:1 | EOL:694 | EOL:695 | EOL:8024 | EOL:8027 | EOL:17930 | EOL:1048951 | EOL:1276240\t\thttp://eol.org/pages/1276240\thttp://media.eol.org/content/2012/11/04/08/35791_98_68.jpg"));
        assertFalse(TermValidatorPredicates.CONSISTENT_PATH.test("EOL:1276240\tAnas crecca carolinensis\tInfraspecies\tGreen-winged Teal @en | Sarcelle à ailes vertes @fr | Amerikaanse Wintertaling @nl | \tAnimalia | Chordata | Aves | Anseriformes | Anatidae | Anas | Anas crecca | Anas crecca carolinensis\tEOL:1 | EOL:694 | EOL:695 | EOL:8024 | EOL:8027 | EOL:17930 | EOL:1048951 | EOL:1276240\tkingdom | class | order | family | genus | species | infraspecies\thttp://eol.org/pages/1276240\thttp://media.eol.org/content/2012/11/04/08/35791_98_68.jpg"));
        assertFalse(TermValidatorPredicates.CONSISTENT_PATH.test("EOL:1276240\tAnas crecca carolinensis\tInfraspecies\tGreen-winged Teal @en | Sarcelle à ailes vertes @fr | Amerikaanse Wintertaling @nl | \tAnimalia | Chordata | Aves | Anseriformes | Anatidae | Anas | Anas crecca | Anas crecca carolinensis\tEOL:1 | EOL:695 | EOL:8024 | EOL:8027 | EOL:17930 | EOL:1048951 | EOL:1276240\tkingdom | phylum | class | order | family | genus | species | infraspecies\thttp://eol.org/pages/1276240\thttp://media.eol.org/content/2012/11/04/08/35791_98_68.jpg"));
        assertFalse(TermValidatorPredicates.CONSISTENT_PATH.test("EOL:1276240\tAnas crecca carolinensis\tInfraspecies\tGreen-winged Teal @en | Sarcelle à ailes vertes @fr | Amerikaanse Wintertaling @nl | \t\tEOL:1 | EOL:695 | EOL:8024 | EOL:8027 | EOL:17930 | EOL:1048951 | EOL:1276240\tkingdom | phylum | class | order | family | genus | species | infraspecies\thttp://eol.org/pages/1276240\thttp://media.eol.org/content/2012/11/04/08/35791_98_68.jpg"));
        String t = "NCBI:1970159\tCyprinus carpio\tspecies\t\t| Eukaryota | Opisthokonta | Metazoa | Eumetazoa | Bilateria | Deuterostomia | Chordata | Craniata | Vertebrata | Gnathostomata | Teleostomi | Euteleostomi | Actinopterygii | Actinopteri | Neopterygii | Teleostei | Osteoglossocephalai | Clupeocephala | Otomorpha | Ostariophysi | Otophysi | Cypriniphysae | Cypriniformes | Cyprinoidea | Cyprinidae | Cyprinus | Cyprinus carpio | Cyprinus carpio\tNCBI:131567 | NCBI:2759 | NCBI:33154 | NCBI:33208 | NCBI:6072 | NCBI:33213 | NCBI:33511 | NCBI:7711 | NCBI:89593 | NCBI:7742 | NCBI:7776 | NCBI:117570 | NCBI:117571 | NCBI:7898 | NCBI:186623 | NCBI:41665 | NCBI:32443 | NCBI:1489341 | NCBI:186625 | NCBI:186634 | NCBI:32519 | NCBI:186626 | NCBI:186627 | NCBI:7952 | NCBI:30727 | NCBI:7953 | NCBI:7961 | NCBI:7962 | NCBI:1970159\t| superkingdom |  | kingdom |  |  |  | phylum | subphylum |  |  |  |  | superclass | class | subclass | infraclass |  |  |  |  |  | superorder | order | superfamily | family | genus | species |\thttp://eol.org/pages/985921\t";
        assertTrue(TermValidatorPredicates.CONSISTENT_PATH.test(t));
    }

    @Test
    public void pathExists() throws PropertyEnricherException, IOException {
        assertTrue(TermValidatorPredicates.PATH_EXISTS.test("EOL:1276240\tAnas crecca carolinensis\tInfraspecies\tGreen-winged Teal @en | Sarcelle à ailes vertes @fr | Amerikaanse Wintertaling @nl | \tAnimalia | Chordata | Aves | Anseriformes | Anatidae | Anas | Anas crecca | Anas crecca carolinensis\tEOL:1 | EOL:694 | EOL:695 | EOL:8024 | EOL:8027 | EOL:17930 | EOL:1048951 | EOL:1276240\tkingdom | phylum | class | order | family | genus | species | infraspecies\thttp://eol.org/pages/1276240\thttp://media.eol.org/content/2012/11/04/08/35791_98_68.jpg"));
        assertTrue(TermValidatorPredicates.PATH_EXISTS.test("EOL:1276240\tAnas crecca carolinensis\tInfraspecies\tGreen-winged Teal @en | Sarcelle à ailes vertes @fr | Amerikaanse Wintertaling @nl | \t\tEOL:1 | EOL:694 | EOL:695 | EOL:8024 | EOL:8027 | EOL:17930 | EOL:1048951 | EOL:1276240\t\thttp://eol.org/pages/1276240\thttp://media.eol.org/content/2012/11/04/08/35791_98_68.jpg"));
        assertFalse(TermValidatorPredicates.PATH_EXISTS.test("BLAH:123\t"));
    }

    @Test
    public void wikidataTaxonWithMultipleParentTaxa() {
        // note that  https://www.wikidata.org/wiki/Q7377 has multiple taxa
        String wd = "WD:Q7377\tMammalia\tWD:Q37517\t\t| Mammalia\tWD:Q2082668|Q189069|Q19159|Q181537 | WD:Q7377\t| WD:Q37517\thttp://eol.org/pages/1642\t\n";
        assertFalse(TermValidatorPredicates.CONSISTENT_PATH.test(wd));
    }

    @Test
    public void supportedPathIds() throws PropertyEnricherException, IOException {
        assertTrue(TermValidatorPredicates.SUPPORTED_PATH_IDS.test("EOL:1276240\tAnas crecca carolinensis\tInfraspecies\tGreen-winged Teal @en | Sarcelle à ailes vertes @fr | Amerikaanse Wintertaling @nl | \tAnimalia | Chordata | Aves | Anseriformes | Anatidae | Anas | Anas crecca | Anas crecca carolinensis\tEOL:1 | EOL:694 | EOL:695 | EOL:8024 | EOL:8027 | EOL:17930 | EOL:1048951 | EOL:1276240\tkingdom | phylum | class | order | family | genus | species | infraspecies\thttp://eol.org/pages/1276240\thttp://media.eol.org/content/2012/11/04/08/35791_98_68.jpg"));
        assertTrue(TermValidatorPredicates.SUPPORTED_PATH_IDS.test("EOL:1276240\tAnas crecca carolinensis\tInfraspecies\tGreen-winged Teal @en | Sarcelle à ailes vertes @fr | Amerikaanse Wintertaling @nl | \tAnimalia | Chordata | Aves | Anseriformes | Anatidae | Anas | Anas crecca | Anas crecca carolinensis\tEOL:1 | EOL:694 | EOL:695 | EOL:8024 | EOL:8027 | EOL:17930 | EOL:1048951 | EOL:1276240\tkingdom | phylum | class | order | family | genus | species | infraspecies\thttp://eol.org/pages/1276240\thttp://media.eol.org/content/2012/11/04/08/35791_98_68.jpg"));
        assertTrue(TermValidatorPredicates.SUPPORTED_PATH_IDS.test("GBIF:204\tActinopterygii\tclass\t\tAnimalia | Chordata | Actinopterygii\tGBIF:1 | GBIF:44 | GBIF:204\tkingdom | phylum | class\thttps://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=7898\t"));
        assertFalse(TermValidatorPredicates.SUPPORTED_PATH_IDS.test("EOL:1276240\tAnas crecca carolinensis\tInfraspecies\tGreen-winged Teal @en | Sarcelle à ailes vertes @fr | Amerikaanse Wintertaling @nl | \tAnimalia | Chordata | Aves | Anseriformes | Anatidae | Anas | Anas crecca | Anas crecca carolinensis\tFOO:1 | EOL:694 | EOL:695 | EOL:8024 | EOL:8027 | EOL:17930 | EOL:1048951 | EOL:1276240\tkingdom | phylum | class | order | family | genus | species | infraspecies\thttp://eol.org/pages/1276240\thttp://media.eol.org/content/2012/11/04/08/35791_98_68.jpg"));
    }

    @Test
    public void expectedNumberOfColumns() throws PropertyEnricherException, IOException {
        assertTrue(TermValidatorPredicates.VALID_NUMBER_OF_TERM_COLUMNS.test("EOL:1276240\tAnas crecca carolinensis\tInfraspecies\tGreen-winged Teal @en | Sarcelle à ailes vertes @fr | Amerikaanse Wintertaling @nl | \tAnimalia | Chordata | Aves | Anseriformes | Anatidae | Anas | Anas crecca | Anas crecca carolinensis\tEOL:1 | EOL:694 | EOL:695 | EOL:8024 | EOL:8027 | EOL:17930 | EOL:1048951 | EOL:1276240\tkingdom | phylum | class | order | family | genus | species | infraspecies\thttp://eol.org/pages/1276240\thttp://media.eol.org/content/2012/11/04/08/35791_98_68.jpg"));
        assertFalse(TermValidatorPredicates.VALID_NUMBER_OF_TERM_COLUMNS.test("BLAH:123\t"));
        String t = "\t\t\t\t\t\t\t\t";
        assertThat(t.length(), Is.is(8));
        assertThat(CSVTSVUtil.splitTSV(t).length, Is.is(9));

        assertTrue(TermValidatorPredicates.VALID_NUMBER_OF_TERM_COLUMNS.test(t));
    }

    @Test
    public void expectedNumberOfMapColumns() throws PropertyEnricherException, IOException {
        assertTrue(TermValidatorPredicates.VALID_NUMBER_OF_MAP_COLUMNS.test("EOL:1276240\tAnas crecca carolinensis\tbla:123\tsomething"));
        assertFalse(TermValidatorPredicates.VALID_NUMBER_OF_MAP_COLUMNS.test("BLAH:123\t"));
        assertFalse(TermValidatorPredicates.VALID_NUMBER_OF_MAP_COLUMNS.test("BLAH:123\t\t\t\t\t\t"));
    }

    @Test
    public void supportedMapIdsColumns() throws PropertyEnricherException, IOException {
        assertTrue(TermValidatorPredicates.VALID_NUMBER_OF_MAP_COLUMNS.test("EOL:1276240\tAnas crecca carolinensis\tbla:123\tsomething"));
        assertFalse(TermValidatorPredicates.VALID_NUMBER_OF_MAP_COLUMNS.test("BLAH:123\t"));
        assertFalse(TermValidatorPredicates.VALID_NUMBER_OF_MAP_COLUMNS.test("BLAH:123\t\t\t\t\t\t"));
    }

    @Test
    public void suportedResolvedIds() throws PropertyEnricherException, IOException {
        assertFalse(TermValidatorPredicates.SUPPORTED_RESOLVED_ID.test("EOL:1276240\tAnas crecca carolinensis\tbla:123\tsomething"));
        assertTrue(TermValidatorPredicates.SUPPORTED_RESOLVED_ID.test("BLAH:123\tsome\tEOL:123\tsomething"));
    }

}