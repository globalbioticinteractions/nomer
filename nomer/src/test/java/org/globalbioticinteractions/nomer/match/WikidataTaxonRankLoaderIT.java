package org.globalbioticinteractions.nomer.match;

import org.apache.commons.io.IOUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.ResourceServiceHTTP;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
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

public class WikidataTaxonRankLoaderIT {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();


    @Test
    public void importFromWikidataInternet() throws IOException, URISyntaxException {
        AtomicInteger counter = new AtomicInteger(0);
        URI req = WikidataTaxonRankLoader.createWikidataTaxonRankQuery();

        File tmpCacheDir = folder.newFolder("tmpCacheDir");

        WikidataTaxonRankLoader.importTaxonRanks(
                taxon -> counter.incrementAndGet(),
                new ResourceServiceHTTP(is -> is, tmpCacheDir),
                req
        );
        assertTrue(counter.get() > 10);
    }

}
