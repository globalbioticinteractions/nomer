package org.globalbioticinteractions.nomer.match;

import org.apache.commons.io.IOUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.ResourceServiceHTTP;
import org.eol.globi.util.ResourceServiceLocal;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class WikidataTaxonRankLoaderTest {

    @Test
    public void importFromWikidataInternet() throws IOException, URISyntaxException {
        AtomicInteger counter = new AtomicInteger(0);
        URI req = WikidataTaxonRankLoader.createWikidataTaxonRankQuery();

        WikidataTaxonRankLoader.importTaxonRanks(taxon -> counter.incrementAndGet(), new ResourceServiceHTTP(is -> is), req);
        assertTrue(counter.get() > 10);
    }

    @Test
    public void importFromWikidataContentBased() throws IOException, URISyntaxException {
        AtomicInteger counter = new AtomicInteger(0);
        URI req = WikidataTaxonRankLoader.createWikidataTaxonRankQuery();

        ResourceService resourceService = new ResourceService() {

            @Override
            public InputStream retrieve(URI uri) throws IOException {
                return getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/util/wikidata-ranks.json");
            }
        };

        WikidataTaxonRankLoader.importTaxonRanks(
                taxon -> counter.incrementAndGet(),
                resourceService,
                req
        );
        assertTrue(counter.get() > 10);
    }

    @Test
    public void importStatic() throws IOException {
        ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        final PrintStream printer1 = new PrintStream(out1, true, CharsetConstant.UTF8);

        WikidataTaxonRankLoader.TermListener cacheWriter = WikidataTaxonRankLoader.createCacheWriter(printer1);

        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        final PrintStream printer2 = new PrintStream(out2, true, CharsetConstant.UTF8);
        WikidataTaxonRankLoader.TermListener mapWriter = WikidataTaxonRankLoader.createMapWriter(printer2);

        WikidataTaxonRankLoader.TermListener proxy = new WikidataTaxonRankLoader.TermListener() {
            @Override
            public void onTerm(Taxon taxon) {
                cacheWriter.onTerm(taxon);
                mapWriter.onTerm(taxon);
            }
        };
        WikidataTaxonRankLoader.handleWikidataTaxonRanks(proxy, IOUtils.toString(getClass().getResourceAsStream("wikidata_taxon_ranks.json"), StandardCharsets.UTF_8));

        assertThat(out1.toString(CharsetConstant.UTF8), startsWith("WD:Q2455704\tsubfamily\t\tonderfamilie @nl"));
        assertThat(out2.toString(CharsetConstant.UTF8), startsWith("\tonderfamilie\tWD:Q2455704\tsubfamily"));
    }

}
