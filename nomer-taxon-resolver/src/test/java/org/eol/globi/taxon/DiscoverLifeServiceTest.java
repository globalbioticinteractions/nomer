package org.eol.globi.taxon;

import org.apache.commons.io.IOUtils;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;

public class DiscoverLifeServiceTest {

    @Test
    public void parseBees() throws IOException {

        final AtomicReference<Taxon> firstTaxon = new AtomicReference<>();

        AtomicInteger counter = new AtomicInteger(0);

        TaxonImportListener listener = new TaxonImportListener() {

            @Override
            public void addTerm(Taxon term) {
                setFirstOnly(term);
            }

            private void setFirstOnly(Taxon term) {
                int index = counter.getAndIncrement();
                if (index == 0) {
                    firstTaxon.set(term);
                }
            }

            @Override
            public void addTerm(String key, Taxon term) {
                setFirstOnly(term);
            }

            @Override
            public void start() {

            }

            @Override
            public void finish() {

            }
        };


        TaxonParser parser = new TaxonParserForDiscoverLife();

        parser.parse(DiscoverLifeService.getStreamOfBees(), listener);

        assertThat(counter.get(), Is.is(50590));

        Taxon taxon = firstTaxon.get();

        assertThat(taxon.getPath(), Is.is("Animalia | Arthropoda | Insecta | Hymenoptera | Andrenidae | Acamptopoeum argentinum"));
        assertThat(taxon.getPathIds(), Is.is("https://www.discoverlife.org/mp/20q?search=Animalia | https://www.discoverlife.org/mp/20q?search=Arthropoda | https://www.discoverlife.org/mp/20q?search=Insecta | https://www.discoverlife.org/mp/20q?search=Hymenoptera | https://www.discoverlife.org/mp/20q?search=Andrenidae | https://www.discoverlife.org/mp/20q?search=Acamptopoeum+argentinum"));
        assertThat(taxon.getPathNames(), Is.is("kingdom | phylum | class | order | family | species"));
        assertThat(taxon.getName(), Is.is("Acamptopoeum argentinum"));
        assertThat(taxon.getRank(), Is.is("species"));
        assertThat(taxon.getId(), Is.is("https://www.discoverlife.org/mp/20q?search=Acamptopoeum+argentinum"));
    }


    @Test
    public void getCurrentBeeNames() throws IOException {
        String actual = DiscoverLifeService.getBeeNamesAsXmlString();

        String localCopy = IOUtils.toString(DiscoverLifeService.getStreamOfBees(), StandardCharsets.UTF_8);
        assertThat(actual, Is.is(localCopy));
    }




}
