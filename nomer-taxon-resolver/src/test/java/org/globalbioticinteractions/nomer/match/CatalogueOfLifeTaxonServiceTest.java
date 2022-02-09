package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CatalogueOfLifeTaxonServiceTest {

    @Test
    public void enrichById() throws PropertyEnricherException {
        CatalogueOfLifeTaxonService service = createService();

        String externalId = "COL:63MJH";
        Map<String, String> enriched = service.enrich(TaxonUtil.taxonToMap(new TaxonImpl(null, externalId)));

        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), is("Phryganella"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getExternalId(), is("COL:63MJH"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getName(), is("Phryganella"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getRank(), is("genus"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathIds(), is("COL:63MJH"));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPathNames(), is("genus"));
    }

    private CatalogueOfLifeTaxonService createService() {
        return createService("/org/globalbioticinteractions/nomer/match/col/NameUsage.tsv");
    }

    private CatalogueOfLifeTaxonService createService(final String nameUrl) {
        return new CatalogueOfLifeTaxonService(new TermMatcherContextClasspath() {
            @Override
            public String getCacheDir() {
                return new File("target/colCache" + UUID.randomUUID()).getAbsolutePath();
            }

            @Override
            public String getOutputFormat() {
                return null;
            }

            @Override
            public String getProperty(String key) {
                return new TreeMap<String, String>() {
                    {
                        put("nomer.col.name_usage",
                                "/org/globalbioticinteractions/nomer/match/col/NameUsage.tsv"
                        );
                    }
                }.get(key);
            }
        });
    }


}