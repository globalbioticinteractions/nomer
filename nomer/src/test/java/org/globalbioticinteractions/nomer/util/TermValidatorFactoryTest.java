package org.globalbioticinteractions.nomer.util;

import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.service.CacheService;
import org.eol.globi.service.PropertyEnricherException;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.assertThat;

public class TermValidatorFactoryTest {

    @Test
    public void validateCache() throws PropertyEnricherException, IOException {
        String termCacheUrl = TermMatcherCacheFactory.getTermCacheUrl(MatchTestUtil.getLocalTermMatcherCache());

        String expectedResult = "OK\tno id\tid\tname\trank\tcommonNames\tpath\tpathIds\tpathNames\texternalUrl\tthumbnailUrl\n" +
                "FAIL\ttoo few\tid\tname\trank\tcommonNames\tpath\tpathIds\tpathNames\texternalUrl\tthumbnailUrl\n" +
                "OK\tno id\tEOL:1276240\tAnas crecca carolinensis\tInfraspecies\tGreen-winged Teal @en | Sarcelle à ailes vertes @fr | Amerikaanse Wintertaling @nl | \tAnimalia | Chordata | Aves | Anseriformes | Anatidae | Anas | Anas crecca | Anas crecca carolinensis\tEOL:1 | EOL:694 | EOL:695 | EOL:8024 | EOL:8027 | EOL:17930 | EOL:1048951 | EOL:1276240\tkingdom | phylum | class | order | family | genus | species | infraspecies\thttp://eol.org/pages/1276240\thttp://media.eol.org/content/2012/11/04/08/35791_98_68.jpg\n" +
                "FAIL\ttoo few\tEOL:1276240\tAnas crecca carolinensis\tInfraspecies\tGreen-winged Teal @en | Sarcelle à ailes vertes @fr | Amerikaanse Wintertaling @nl | \tAnimalia | Chordata | Aves | Anseriformes | Anatidae | Anas | Anas crecca | Anas crecca carolinensis\tEOL:1 | EOL:694 | EOL:695 | EOL:8024 | EOL:8027 | EOL:17930 | EOL:1048951 | EOL:1276240\tkingdom | phylum | class | order | family | genus | species | infraspecies\thttp://eol.org/pages/1276240\thttp://media.eol.org/content/2012/11/04/08/35791_98_68.jpg\n" +
                "OK\tno id\tEOL:455065\tActeocina inculta\tSpecies\trude barrel-bubble @en | \tAnimalia | Mollusca | Gastropoda | Cephalaspidea | Philinoidea | Cylichnidae | Acteocina | Acteocina inculta\tEOL:1 | EOL:2195 | EOL:2366 | EOL:2410 | EOL:10591049 | EOL:2415 | EOL:50321 | EOL:455065\tkingdom | phylum | class | order | superfamily | family | genus | species\thttp://eol.org/pages/455065\t\n" +
                "FAIL\ttoo few\tEOL:455065\tActeocina inculta\tSpecies\trude barrel-bubble @en | \tAnimalia | Mollusca | Gastropoda | Cephalaspidea | Philinoidea | Cylichnidae | Acteocina | Acteocina inculta\tEOL:1 | EOL:2195 | EOL:2366 | EOL:2410 | EOL:10591049 | EOL:2415 | EOL:50321 | EOL:455065\tkingdom | phylum | class | order | superfamily | family | genus | species\thttp://eol.org/pages/455065\t\n";
        assertValidation(termCacheUrl, expectedResult);
    }

    @Test
    public void validateMap() throws PropertyEnricherException, IOException {
        String termCacheUrl = TermMatcherCacheFactory.getTermMapUrl(MatchTestUtil.getLocalTermMatcherCache());

        String expectedResult = "OK\tno id\tprovidedTaxonId\tprovidedTaxonName\tresolvedTaxonId\tresolvedTaxonName\n" +
                "FAIL\ttoo few\tprovidedTaxonId\tprovidedTaxonName\tresolvedTaxonId\tresolvedTaxonName\n" +
                "OK\tno id\tEOL:1276240\tGreen-winged teal\tEOL:1276240\tAnas crecca carolinensis\n" +
                "FAIL\ttoo few\tEOL:1276240\tGreen-winged teal\tEOL:1276240\tAnas crecca carolinensis\n" +
                "OK\tno id\tEOL:1276240\tno name\tEOL:1276240\tAnas crecca carolinensis\n" +
                "FAIL\ttoo few\tEOL:1276240\tno name\tEOL:1276240\tAnas crecca carolinensis\n";
        ;
        assertValidation(termCacheUrl, expectedResult);
    }

    private void assertValidation(String termCacheUrl, String expectedResult) throws IOException {
        TermValidator cacheService = new TermValidatorFactory()
                .createTermValidator(CacheService.createBufferedReader(termCacheUrl).lines());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Predicate<String> one = s -> true;
        Predicate<String> two = s -> false;
        Pair<Predicate<String>, String> predicateReason = Pair.of(one, "no id");
        Pair<Predicate<String>, String> predicateReason1 = Pair.of(two, "too few");
        List<Pair<Predicate<String>, String>> predicates = Arrays.asList(predicateReason, predicateReason1);

        cacheService.setPredicates(predicates);
        cacheService.validate(new PrintStream(out));
        assertThat(out.toString(), Is.is(
                expectedResult));
    }

}