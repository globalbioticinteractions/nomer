package org.globalbioticinteractions.nomer.match;

import org.eol.globi.domain.TaxonomyProvider;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class CommonTaxonServiceTest {

    @Test
    public void isSupportedAlternatePrefix() {
        assertTrue(CommonTaxonService.isIdSupportedBy("tsn:123", TaxonomyProvider.ITIS));
    }

    @Test
    public void isSupportedAlternatePrefix2() {
        assertTrue(CommonTaxonService.isIdSupportedBy("TSN:123", TaxonomyProvider.ITIS));
    }

    @Test
    public void isSupportedDefaultPrefix2() {
        assertTrue(CommonTaxonService.isIdSupportedBy("ITIS:123", TaxonomyProvider.ITIS));
    }

    @Test
    public void isSupportedNCBI() {
        assertTrue(CommonTaxonService.isIdSupportedBy("NCBI:123", TaxonomyProvider.NCBI));
    }

    @Test
    public void isSupportedNCBI2() {
        assertTrue(CommonTaxonService.isIdSupportedBy("NCBI:txid9606", TaxonomyProvider.NCBI));
    }

}