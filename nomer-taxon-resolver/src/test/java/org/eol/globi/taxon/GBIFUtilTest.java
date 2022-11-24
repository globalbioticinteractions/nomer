package org.eol.globi.taxon;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.eol.globi.domain.Taxon;
import org.globalbioticinteractions.nomer.match.GBIFNameRelationType;
import org.globalbioticinteractions.nomer.match.GBIFRank;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class GBIFUtilTest {

    @Test
    public void parseIdNameRank() {
        String line = "3220666\t3220663\t\\N\tf\tACCEPTED\tSPECIES\t{}\ta97f36e5-ded1-49cc-bdec-ac6170fc7b9c\tSOURCE\t172345459\t3\t10719742\t10705277\t1382\t10766405\t3220663\t3220666\t3422152\tDesulfofaba hansenii (Finster et al., 2001) Abildgaard et al., 2004\tDesulfofaba hansenii\tDesulfofaba\thansenii\t\\N\t\\N\tAbildgaard et al.\t2004\tFinster et al.\t2001\tInt. J. Syst. Evol. Microbiol. 54::398\t{}\n";

        Triple<Long, String[], GBIFRank> taxon = GBIFUtil.parseIdNameRank(line);

        assertThat(taxon.getRight(), is(GBIFRank.SPECIES));
        assertThat(taxon.getMiddle()[0], is("Desulfofaba hansenii"));
        assertThat(taxon.getMiddle()[1], is("(Finster et al., 2001)"));
        assertThat(taxon.getLeft(), is(3220666L));

    }

    @Test
    public void parseIdNameRank2() {
        String line = "3220667\t3220666\t\\N\tt\tSYNONYM\tSPECIES\t{}\t52a423d2-0486-4e77-bcee-6350d708d6ff\tSOURCE\t176083328\t3\t10719742\t10705277\t1382\t10766405\t3220663\t3220666\t3423000\tDesulfomusa hansenii Finster et al., 2001\tDesulfomusa hansenii\tDesulfomusa\thansenii\t\\N\t\\N\tFinster et al.\t2001\t\\N\t\\N\tInt. J. Syst. Evol. Microbiol. 51::2060\t{}";

        Triple<Long, String[], GBIFRank> taxon = GBIFUtil.parseIdNameRank(line);

        assertThat(taxon.getRight(), is(GBIFRank.SPECIES));
        assertThat(taxon.getMiddle()[0], is("Desulfomusa hansenii"));
        assertThat(taxon.getMiddle()[1], is("Finster et al., 2001"));
        assertThat(taxon.getLeft(), is(3220667L));
    }

    @Test
    public void parseIdNameRank3() {
        String line = "3220667\t\\N\t\\N\tt\tSYNONYM\tSPECIES\t{}\t52a423d2-0486-4e77-bcee-6350d708d6ff\tSOURCE\t176083328\t3\t10719742\t10705277\t1382\t10766405\t3220663\t3220666\t3423000\tDesulfomusa hansenii Finster et al., 2001\tDesulfomusa hansenii\tDesulfomusa\thansenii\t\\N\t\\N\tFinster et al.\t2001\t\\N\t\\N\tInt. J. Syst. Evol. Microbiol. 51::2060\t{}";

        Triple<Long, String[], GBIFRank> taxon = GBIFUtil.parseIdNameRank(line);

        assertThat(taxon.getRight(), is(GBIFRank.SPECIES));
        assertThat(taxon.getMiddle()[0], is("Desulfomusa hansenii"));
        assertThat(taxon.getMiddle()[1], is("Finster et al., 2001"));
        assertThat(taxon.getLeft(), is(3220667L));
    }

    @Test
    public void parseSynonymMapping() {
        String line = "3220667\t3220666\t\\N\tt\tSYNONYM\tSPECIES\t{}\t52a423d2-0486-4e77-bcee-6350d708d6ff\tSOURCE\t176083328\t3\t10719742\t10705277\t1382\t10766405\t3220663\t3220666\t3423000\tDesulfomusa hansenii Finster et al., 2001\tDesulfomusa hansenii\tDesulfomusa\thansenii\t\\N\t\\N\tFinster et al.\t2001\t\\N\t\\N\tInt. J. Syst. Evol. Microbiol. 51::2060\t{}";

        Pair<Long, Pair<GBIFNameRelationType, Long>> mapping = GBIFUtil.parseIdRelation(line);

        assertNotNull(mapping);
        assertThat(mapping.getLeft(), is(3220667L));
        assertThat(mapping.getRight(), is(Pair.of(GBIFNameRelationType.SYNONYM, 3220666L)));

    }

    @Test
    public void parseChildParentMapping() {
        String line = "3220666\t3220663\t\\N\tf\tACCEPTED\tSPECIES\t{}\ta97f36e5-ded1-49cc-bdec-ac6170fc7b9c\tSOURCE\t172345459\t3\t10719742\t10705277\t1382\t10766405\t3220663\t3220666\t3422152\tDesulfofaba hansenii (Finster et al., 2001) Abildgaard et al., 2004\tDesulfofaba hansenii\tDesulfofaba\thansenii\t\\N\t\\N\tAbildgaard et al.\t2004\tFinster et al.\t2001\tInt. J. Syst. Evol. Microbiol. 54::398\t{}\n";

        Pair<Long, Pair<GBIFNameRelationType, Long>> idRelation = GBIFUtil.parseIdRelation(line);

        assertNotNull(idRelation);
        assertThat(idRelation.getLeft(), is(3220666L));
        assertThat(idRelation.getRight(), is(Pair.of(GBIFNameRelationType.PARENT, 3220663L)));

    }

    @Test
    public void parseRootMapping() {
        String line = "3220666\t\\N\t\\N\tf\tACCEPTED\tSPECIES\t{}\ta97f36e5-ded1-49cc-bdec-ac6170fc7b9c\tSOURCE\t172345459\t3\t10719742\t10705277\t1382\t10766405\t3220663\t3220666\t3422152\tDesulfofaba hansenii (Finster et al., 2001) Abildgaard et al., 2004\tDesulfofaba hansenii\tDesulfofaba\thansenii\t\\N\t\\N\tAbildgaard et al.\t2004\tFinster et al.\t2001\tInt. J. Syst. Evol. Microbiol. 54::398\t{}\n";

        Pair<Long, Pair<GBIFNameRelationType, Long>> idRelation = GBIFUtil.parseIdRelation(line);

        assertNull(idRelation);
    }

    @Test
    public void resolveSynonymTaxonId() {
        Map<Long, Pair<String[], GBIFRank>> idToNameAndRank = new TreeMap<>();
        Map<Long, Pair<GBIFNameRelationType, Long>> idRelation = new TreeMap<>();

        idToNameAndRank.put(12L, Pair.of(new String[]{"Some synonym", "some author"}, GBIFRank.SPECIES));
        idToNameAndRank.put(15L, Pair.of(new String[]{"Some accepted child", "some other author"}, GBIFRank.SPECIES));
        idToNameAndRank.put(1L, Pair.of(new String[]{"Some accepted parent", "some parent author"}, GBIFRank.GENUS));

        idRelation.put(12L, Pair.of(GBIFNameRelationType.SYNONYM, 15L));
        idRelation.put(15L, Pair.of(GBIFNameRelationType.PARENT, 1L));

        Long requestedTaxonId = 12L;

        Taxon taxon = GBIFUtil.resolveTaxonId1(idToNameAndRank, idRelation, requestedTaxonId);

        assertThat(taxon.getName(), is("Some accepted child"));
        assertThat(taxon.getAuthorship(), is("some other author"));
        assertThat(taxon.getPathIds(), is("GBIF:1 | GBIF:15"));
        assertThat(taxon.getPath(), is("Some accepted parent | Some accepted child"));
        assertThat(taxon.getPathNames(), is("genus | species"));


    }

    @Test
    public void resolveExistingRelationButMissingTaxonId() {
        Map<Long, Pair<String[], GBIFRank>> idToNameAndRank = new TreeMap<>();
        Map<Long, Pair<GBIFNameRelationType, Long>> idRelation = new TreeMap<>();

        idRelation.put(12L, Pair.of(GBIFNameRelationType.SYNONYM, 15L));

        Long requestedTaxonId = 12L;

        Taxon taxon = GBIFUtil.resolveTaxonId1(idToNameAndRank, idRelation, requestedTaxonId);

        assertNull(taxon);
    }

    @Test
    public void resolveExistingRelationButMissingSynonymTaxonId() {
        Map<Long, Pair<String[], GBIFRank>> idToNameAndRank = new TreeMap<>();
        idToNameAndRank.put(12L, Pair.of(new String[]{"Some synonym", "some author"}, GBIFRank.SPECIES));

        Map<Long, Pair<GBIFNameRelationType, Long>> idRelation = new TreeMap<>();
        idRelation.put(12L, Pair.of(GBIFNameRelationType.SYNONYM, 15L));

        Long requestedTaxonId = 12L;

        Taxon taxon = GBIFUtil.resolveTaxonId1(idToNameAndRank, idRelation, requestedTaxonId);

        assertNull(taxon);
    }

    @Test
    public void resolveAcceptedTaxonId() {
        Map<Long, Pair<String[], GBIFRank>> idToNameAndRank = new TreeMap<>();
        Map<Long, Pair<GBIFNameRelationType, Long>> idRelation = new TreeMap<>();

        idToNameAndRank.put(15L, Pair.of(new String[]{"Some accepted child", "some author"}, GBIFRank.SPECIES));
        idToNameAndRank.put(1L, Pair.of(new String[] {"Some accepted parent", "some parent author"}, GBIFRank.GENUS));

        idRelation.put(15L, Pair.of(GBIFNameRelationType.PARENT, 1L));

        Long requestedTaxonId = 15L;

        Taxon taxon = GBIFUtil.resolveTaxonId1(idToNameAndRank, idRelation, requestedTaxonId);

        assertThat(taxon.getName(), is("Some accepted child"));
        assertThat(taxon.getAuthorship(), is("some author"));
        assertThat(taxon.getPathIds(), is("GBIF:1 | GBIF:15"));
        assertThat(taxon.getPath(), is("Some accepted parent | Some accepted child"));
        assertThat(taxon.getPathNames(), is("genus | species"));


    }


}