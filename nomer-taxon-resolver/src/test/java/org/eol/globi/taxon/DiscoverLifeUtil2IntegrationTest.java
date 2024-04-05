package org.eol.globi.taxon;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.service.TaxonUtil;
import org.hamcrest.core.Is;
import org.junit.Test;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import static org.hamcrest.MatcherAssert.assertThat;

public class DiscoverLifeUtil2IntegrationTest {

    public static final String BEES_XML_GZIP = "/org/globalbioticinteractions/nomer/match/discoverlife/bees.xml.gz";

    @Test
    public void compareLocalVersionToRemoteVersion() throws IOException {
        DiscoverLifeTestUtil.compareLocalVersionToRemoteVersion(
                BEES_XML_GZIP,
                DiscoverLifeUtil.URL_ENDPOINT_DISCOVER_LIFE + "/nh/id/20q/Apoidea_species.xml"
        );
    }

    @Test
    public void parseNames() throws ParserConfigurationException, IOException {
        AtomicInteger counter = new AtomicInteger(0);
        DiscoverLifeUtil2.parse(new GZIPInputStream(getClass().getResourceAsStream(BEES_XML_GZIP)), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                counter.incrementAndGet();
            }
        });
        assertThat(counter.get(), Is.is(31278));
    }

}