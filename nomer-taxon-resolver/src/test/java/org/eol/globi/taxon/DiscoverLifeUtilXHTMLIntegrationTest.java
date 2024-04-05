package org.eol.globi.taxon;

import org.eol.globi.domain.Taxon;
import org.eol.globi.service.ResourceService;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

public class DiscoverLifeUtilXHTMLIntegrationTest {

    private static final String DISCOVER_LIFE_URL
            = DiscoverLifeUtilXHTML.URL_ENDPOINT_DISCOVER_LIFE +
            "/mp/20q" +
            "?act=x_checklist" +
            "&guide=Apoidea_species" +
            "&flags=HAS";
    private static final String BEE_NAMES_CACHED = "/org/globalbioticinteractions/nomer/match/discoverlife/bees.html.gz";


    @Test
    public void parseBees() throws IOException {

        final AtomicReference<Taxon> firstTaxon = new AtomicReference<>();

        AtomicInteger counter = new AtomicInteger(0);

        TermMatchListener listener = (requestId, providedTerm, nameType, resolvedTaxon) -> {
            int index = counter.getAndIncrement();
            if (index == 0) {
                firstTaxon.set(resolvedTaxon);
            }
        };

        DiscoverLifeUtilXHTML.parse(DiscoverLifeUtilXHTML.getBeeNameTable(new ResourceService() {
            @Override
            public InputStream retrieve(URI uri) throws IOException {
                return DiscoverLifeTestUtil.getStreamOfBees(BEE_NAMES_CACHED);
            }
        }, "https://example.org"), listener);

        assertThat(counter.get(), Is.is(58310));

        Taxon taxon = firstTaxon.get();

        assertThat(taxon.getPath(), Is.is("Animalia | Arthropoda | Insecta | Hymenoptera | Andrenidae | Acamptopoeum | Acamptopoeum argentinum"));
        assertThat(taxon.getPathIds(), Is.is("https://www.discoverlife.org/mp/20q?search=Animalia | https://www.discoverlife.org/mp/20q?search=Arthropoda | https://www.discoverlife.org/mp/20q?search=Insecta | https://www.discoverlife.org/mp/20q?search=Hymenoptera | https://www.discoverlife.org/mp/20q?search=Andrenidae | https://www.discoverlife.org/mp/20q?search=Acamptopoeum | https://www.discoverlife.org/mp/20q?search=Acamptopoeum+argentinum"));
        assertThat(taxon.getPathNames(), Is.is("kingdom | phylum | class | order | family | genus | species"));
        assertThat(taxon.getName(), Is.is("Acamptopoeum argentinum"));
        assertThat(taxon.getRank(), Is.is("species"));
        assertThat(taxon.getId(), Is.is("https://www.discoverlife.org/mp/20q?search=Acamptopoeum+argentinum"));
    }


    @Test
    public void getCurrentBeeNames() throws IOException {
        DiscoverLifeTestUtil.compareLocalVersionToRemoteVersion(BEE_NAMES_CACHED, DISCOVER_LIFE_URL);
    }

}