package org.globalbioticinteractions.nomer.util;

import org.apache.commons.io.IOUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.Taxon;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class WikidataTaxonRankLoaderTest {

    @Test
    public void importFromWikidata() throws IOException, URISyntaxException {
        AtomicInteger counter = new AtomicInteger(0);
        WikidataTaxonRankLoader.importTaxonRanks(taxon -> counter.incrementAndGet());
        assertTrue(counter.get() > 10);
    }

    @Test
    public void importStatic() throws URISyntaxException, IOException {
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
        WikidataTaxonRankLoader.handleWikidataTaxonRanks(proxy, IOUtils.toString(getClass().getResourceAsStream("wikidata_taxon_ranks.json")));

        assertThat(out1.toString(CharsetConstant.UTF8), startsWith("WD:Q2455704\tsubfamily\t\tonderfamilie @nl"));
        assertThat(out2.toString(CharsetConstant.UTF8), startsWith("\tonderfamilie\tWD:Q2455704\tsubfamily"));
    }

}
