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

public class BOLDServiceIT {

    @Test
    public void existingBinById() throws PropertyEnricherException {
        Taxon bold = new TaxonImpl("", "BOLD:ACM3285");
        Map<String, String> enrich = new BOLDService().enrich(TaxonUtil.taxonToMap(bold));

        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Calanoida"));
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