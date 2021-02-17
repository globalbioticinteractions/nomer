package org.eol.globi.taxon;

import org.apache.commons.io.IOUtils;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class BOLDServiceTest {

    @Test
    public void existingBinById() throws PropertyEnricherException {
        Taxon bold = new TaxonImpl("", "BOLD:ACM3285");
        Map<String, String> enrich = new BOLDService().enrich(TaxonUtil.taxonToMap(bold));

        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Calanoida"));
    }

    @Test
    public void parseResult() throws IOException {
        String result = "processid\tsampleid\trecordID\tcatalognum\tfieldnum\tinstitution_storing\tcollection_code\tbin_uri\tphylum_taxID\tphylum_name\tclass_taxID\tclass_name\torder_taxID\torder_name\tfamily_taxID\tfamily_name\tsubfamily_taxID\tsubfamily_name\tgenus_taxID\tgenus_name\tspecies_taxID\tspecies_name\tsubspecies_taxID\tsubspecies_name\tidentification_provided_by\tidentification_method\tidentification_reference\ttax_note\tvoucher_status\ttissue_type\tcollection_event_id\tcollectors\tcollectiondate_start\tcollectiondate_end\tcollectiontime\tcollection_note\tsite_code\tsampling_protocol\tlifestage\tsex\treproduction\thabitat\tassociated_specimens\tassociated_taxa\textrainfo\tnotes\tlat\tlon\tcoord_source\tcoord_accuracy\telev\tdepth\telev_accuracy\tdepth_accuracy\tcountry\tprovince_state\tregion\tsector\texactsite\timage_ids\timage_urls\tmedia_descriptors\tcaptions\tcopyright_holders\tcopyright_years\tcopyright_licenses\tcopyright_institutions\tphotographers\n" +
                "ZPC110-14\tBIOUG01212-B02\t4269823\tBIOUG01212-B02\t\tCentre for Biodiversity Genomics\t\tBOLD:ACM3285\t20\tArthropoda\t979181\tCopepoda\t386\tCalanoida\t\t\t\t\t\t\t\t\t\t\tPeter Bryant\t\t\t\t\t\t\t\t\t\t\t\t\t\tAdult\t\t\t\t\tex: Parasite from Ghost Shrimp,  Neotrypaea californiensis\tNo match 2014\t\t33.6022\t-117.898\t\t\t\t\t\t\tUnited States\tCalifornia\tOrange County\tNewport Beach\tBalboa fun zone\t2373302\thttp://www.boldsystems.org/pics/ZPC/BIOUG01212-B02+1405358528.jpg\tDorsal\t\tCBG Photography Group\t2014\tCreativeCommons - Attribution Non-Commercial Share-Alike\tCentre for Biodiversity Genomics\tCBG Photography Group\n" +
                "ZPC111-14\tBIOUG01212-B03\t4269824\tBIOUG01212-B03\t\tCentre for Biodiversity Genomics\t\tBOLD:ACM3285\t20\tArthropoda\t979181\tCopepoda\t386\tCalanoida\t\t\t\t\t\t\t\t\t\t\tPeter Bryant\t\t\t\t\t\t\t\t\t\t\t\t\t\tAdult\t\t\t\t\tex:Parasite from Ghost Shrimp\tNo match 2014\t\t33.6022\t-117.898\t\t\t\t\t\t\tUnited States\tCalifornia\tOrange County\tNewport Beach\tBalboa fun zone\t2373303\thttp://www.boldsystems.org/pics/ZPC/BIOUG01212-B03+1405358542.jpg\tDorsal\t\tCBG Photography Group\t2014\tCreativeCommons - Attribution Non-Commercial Share-Alike\tCentre for Biodiversity Genomics\tCBG Photography Group\n";

        Taxon taxon = BOLDService.parseTaxon(IOUtils.toInputStream(result, StandardCharsets.UTF_8));

        assertNotNull(taxon);

        assertThat(taxon.getPath(), is("Arthropoda | Copepoda | Calanoida"));
        assertThat(taxon.getPathNames(), is("phylum | class | order"));
        assertThat(taxon.getPathIds(), is("BOLDTaxon:20 | BOLDTaxon:979181 | BOLDTaxon:386"));

        assertThat(taxon.getName(), is("Calanoida"));
        assertThat(taxon.getExternalId(), is("BOLDTaxon:386"));
        assertThat(taxon.getRank(), is("order"));

    }

    @Test
    public void parseTaxonIdLookupResult() throws IOException {
        String result = "{\"88899\":{\"taxid\":88899,\"taxon\":\"Momotus\",\"tax_rank\":\"genus\",\"tax_division\":\"Animalia\",\"parentid\":88898,\"parentname\":\"Momotidae\"},\"88898\":{\"taxid\":88898,\"taxon\":\"Momotidae\",\"tax_rank\":\"family\",\"tax_division\":\"Animalia\",\"parentid\":339,\"parentname\":\"Coraciiformes\"},\"339\":{\"taxid\":339,\"taxon\":\"Coraciiformes\",\"tax_rank\":\"order\",\"tax_division\":\"Animalia\",\"parentid\":51,\"parentname\":\"Aves\"},\"51\":{\"taxid\":51,\"taxon\":\"Aves\",\"tax_rank\":\"class\",\"tax_division\":\"Animalia\",\"parentid\":18,\"parentname\":\"Chordata\",\"taxonrep\":\"Aves\"},\"18\":{\"taxid\":18,\"taxon\":\"Chordata\",\"tax_rank\":\"phylum\",\"tax_division\":\"Animalia\",\"parentid\":1,\"taxonrep\":\"Chordata\"}}";

        Taxon taxon = BOLDService.parseTaxonIdMatch(result);

        assertNotNull(taxon);

        assertThat(taxon.getPath(), is("Chordata | Aves | Coraciiformes | Momotidae | Momotus"));
        assertThat(taxon.getPathNames(), is("phylum | class | order | family | genus"));
        assertThat(taxon.getPathIds(), is("BOLDTaxon:18 | BOLDTaxon:51 | BOLDTaxon:339 | BOLDTaxon:88898 | BOLDTaxon:88899"));

        assertThat(taxon.getName(), is("Momotus"));
        assertThat(taxon.getRank(), is("genus"));
        assertThat(taxon.getExternalId(), is("BOLDTaxon:88899"));

    }

    @Test
    public void parseTaxonIdLookupResultNoMatch() throws IOException {
        Taxon taxon = BOLDService.parseTaxonIdMatch("[]");
        assertNull(taxon);
    }

    @Test
    public void parseIllegalResult() throws IOException {
        Taxon taxon = BOLDService.parseTaxon(
                IOUtils.toInputStream("kaboom!", StandardCharsets.UTF_8)
        );
        assertNull(taxon);
    }

    @Test
    public void existingBinByName() throws PropertyEnricherException {
        new TreeMap<String, String>() {{

        }};
        TaxonImpl bold = new TaxonImpl("BOLD:ACM3285");
        Map<String, String> enrich = new BOLDService().enrich(TaxonUtil.taxonToMap(bold));

        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Calanoida"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("BOLDTaxon:386"));

    }

    @Test
    public void existingTaxonId() throws PropertyEnricherException {
        new TreeMap<String, String>() {{

        }};
        TaxonImpl bold = new TaxonImpl("bla", "BOLDTaxon:88899");
        Map<String, String> enrich = new BOLDService().enrich(TaxonUtil.taxonToMap(bold));

        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Momotus"));
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("BOLDTaxon:88899"));
        assertThat(enrich.get(PropertyAndValueDictionary.RANK), is("genus"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_IDS), is("BOLDTaxon:18 | BOLDTaxon:51 | BOLDTaxon:339 | BOLDTaxon:88898 | BOLDTaxon:88899"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH), is("Chordata | Aves | Coraciiformes | Momotidae | Momotus"));
        assertThat(enrich.get(PropertyAndValueDictionary.PATH_NAMES), is("phylum | class | order | family | genus"));

    }


}