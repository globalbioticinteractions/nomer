package org.eol.globi.taxon;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.service.TaxonUtil;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DiscoverLifeUtil2Test {

    @Test
    public void parseXMLRecord() throws ParserConfigurationException, XPathExpressionException, IOException, SAXException {
        Document doc = docForResource("/org/globalbioticinteractions/nomer/match/discoverlife/agapostemon_texanus.xml");

        Map<String, String> nameMap = DiscoverLifeUtil2.parseFocalTaxon(doc);

        Taxon taxon = TaxonUtil.mapToTaxon(nameMap);
        assertThat(taxon.getName(), is("Agapostemon texanus"));
        assertThat(taxon.getStatus().getName(), is("accepted"));
        assertThat(taxon.getRank(), is("species"));
        assertThat(taxon.getId(), is("https://www.discoverlife.org/mp/20q?search=Agapostemon+texanus"));
        assertThat(taxon.getAuthorship(), is("Cresson, 1872"));
        assertThat(taxon.getPath(), is("Animalia | Arthropoda | Insecta | Hymenoptera | Halictidae | Halictinae | Halictini | Caenohalictina | Agapostemon | Agapostemon | Agapostemon texanus"));
        assertThat(taxon.getPathNames(), is("kingdom | phylum | class | order | family | subfamily | tribe | subtribe | genus | subgenus | species"));
    }

    @Test
    public void parseRelatedNames() throws ParserConfigurationException, XPathExpressionException, IOException, SAXException {
        Document doc = docForResource("/org/globalbioticinteractions/nomer/match/discoverlife/andrena_cressonii.xml");

        List<Taxon> taxons = DiscoverLifeUtil2.parseRelatedNames(doc);

        assertThat(taxons.size(), is(13));

        Taxon lastTaxon = taxons.get(12);
        assertThat(lastTaxon.getName(), is("Andrena (Holandrena) cressonii"));
        assertThat(lastTaxon.getAuthorship(), is("Robertson, 1891"));
    }

    @Test
    public void parseCommonNames() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        Document doc = docForResource("/org/globalbioticinteractions/nomer/match/discoverlife/agapostemon_texanus.xml");

        List<Taxon> names = DiscoverLifeUtil2.parseRelatedNames(doc);
        assertThat(names.size(), is(14));

        assertThat(names.get(0).getName(), is("Agapostemon texanus subtilior"));
        assertThat(names.get(0).getAuthorship(), is("Cockerell, 1898"));
    }

    private Document docForResource(String resourcePath) throws SAXException, IOException, ParserConfigurationException {
        InputStream is = getClass().getResourceAsStream(resourcePath);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        return factory.newDocumentBuilder().parse(is);
    }

    @Test
    public void parseAcamptopoeum_melanogaster() throws ParserConfigurationException, XPathExpressionException, IOException, SAXException {
        Document doc = docForResource("/org/globalbioticinteractions/nomer/match/discoverlife/record_Acamptopoeum_melanogaster.xml");

        Map<String, String> nameMap = DiscoverLifeUtil2.parseFocalTaxon(doc);

        Taxon taxon = TaxonUtil.mapToTaxon(nameMap);
        assertThat(taxon.getName(), is("Acamptopoeum melanogaster"));
        assertThat(taxon.getRank(), is("species"));
        assertThat(taxon.getAuthorship(), is("Compagnucci, 2004"));
        assertThat(taxon.getPath(), is("Animalia | Arthropoda | Insecta | Hymenoptera | Andrenidae | Panurginae | Calliopsini | None | Acamptopoeum | None | Acamptopoeum melanogaster"));
        assertThat(taxon.getPathNames(), is("kingdom | phylum | class | order | family | subfamily | tribe | subtribe | genus | subgenus | species"));
    }

    @Test
    public void splitRecords() throws IOException {
        InputStream is = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/discoverlife/bees-head.xml");

        List<String> records = new ArrayList<>();

        DiscoverLifeUtil2.splitRecords(is, records::add);
        assertThat(records.get(0), is(IOUtils.toString(getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/discoverlife/record_Adrenidae.xml"), StandardCharsets.UTF_8)));
        assertThat(records.get(records.size() - 1), is(IOUtils.toString(getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/discoverlife/record_Acamptopoeum_melanogaster.xml"), StandardCharsets.UTF_8)));
    }

    @Test
    public void parseRecords() throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
        InputStream is = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/discoverlife/bees-head.xml");

        List<Triple<Term, NameType, Taxon>> records = new ArrayList<>();

        DiscoverLifeUtil2.parse(is, new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                records.add(Triple.of(providedTerm, nameType, resolvedTaxon));
            }
        });

        assertThat(records.size(), is(16));

        Triple<Term, NameType, Taxon> first = records.get(0);
        assertThat(first.getLeft().getName(), is("Andrenidae"));
        assertThat(first.getMiddle(), is(NameType.HAS_ACCEPTED_NAME));
        assertThat(first.getRight().getName(), is("Andrenidae"));
        assertThat(first.getRight().getRank(), is("family"));

        Triple<Term, NameType, Taxon> last = records.get(records.size() - 1);
        assertThat(last.getLeft().getName(), is("Acamptopoeum melanogaster"));
        assertThat(last.getMiddle(), is(NameType.HAS_ACCEPTED_NAME));
        assertThat(last.getRight().getName(), is("Acamptopoeum melanogaster"));
        assertThat(last.getRight().getRank(), is("species"));
        assertThat(last.getRight().getPath(), is("Animalia | Arthropoda | Insecta | Hymenoptera | Andrenidae | Panurginae | Calliopsini | None | Acamptopoeum | None | Acamptopoeum melanogaster"));
    }

    @Test
    public void parseName() {
        Taxon matched = DiscoverLifeUtil2.parse("Pterandrena aliciae (Robertson, 1891)");

        assertNotNull(matched);
        assertThat(matched.getName(), is("Pterandrena aliciae"));
        assertThat(matched.getAuthorship(), is("(Robertson, 1891)"));
    }

    @Test
    public void parseNameAlt1() {
        String name = "Acamptopoeum colombiensis_sic Shinn, 1965";

        Taxon matched = DiscoverLifeUtil2.parse(name);
        assertThat(matched.getName(), is("Acamptopoeum colombiensis"));
        assertThat(matched.getAuthorship(), is("Shinn, 1965"));
    }

    @Test
    public void parseNameAlt2() {
        Taxon matched = DiscoverLifeUtil2.parse("Camptopoeum (Acamptopoeum) nigritarse Vachal, 1909");
        assertThat(matched.getName(), is("Camptopoeum (Acamptopoeum) nigritarse"));
        assertThat(matched.getAuthorship(), is("Vachal, 1909"));

    }

    @Test
    public void parseNameAlt3() {
        Taxon matched = DiscoverLifeUtil2.parse("Allodapula minor Michener and Syed, 1962");
        assertThat(matched.getName(), is("Allodapula minor"));
        assertThat(matched.getAuthorship(), is("Michener and Syed, 1962"));
    }

    @Test
    public void parseNameAlt4() {
        Taxon matched = DiscoverLifeUtil2.parse("Zadontomerus metallica (H. S. Smith, 1907)");
        assertThat(matched.getName(), is("Zadontomerus metallica"));
        assertThat(matched.getAuthorship(), is("(H. S. Smith, 1907)"));

    }

    @Test
    public void patchCommonNames() {
        String name = "Protandrena bachue Gonzalez and Ruz, 2007 Rhophitulus bachue (Gonzalez and Ruz, 2007)";
        name = DiscoverLifeUtil2.ensureDelimiters(name);

        assertThat(name, is("Protandrena bachue Gonzalez and Ruz, 2007 ;Rhophitulus bachue (Gonzalez and Ruz, 2007);"));
    }

    @Test
    public void patchCommonNames2() {
        String name = "Centris (Melanocentris) rhodoprocta Moure and Seabra, 1960, replacement name Centris (Melacentris) rufosuffusa Cockerell, 1900";
        name = DiscoverLifeUtil2.ensureDelimitersWithNote(name);

        assertThat(name, is("Centris (Melanocentris) rhodoprocta Moure and Seabra, 1960, replacement name ;Centris (Melacentris) rufosuffusa Cockerell, 1900"));
    }

    @Test
    public void patchCommonNames3() {
        String name = "Centris (Melanocentris) rhodoprocta (Moure and Seabra, 1960), replacement name Centris (Melacentris) rufosuffusa Cockerell, 1900";
        name = DiscoverLifeUtil2.ensureDelimitersWithNote(name);

        assertThat(name, is("Centris (Melanocentris) rhodoprocta (Moure and Seabra, 1960), replacement name ;Centris (Melacentris) rufosuffusa Cockerell, 1900"));
    }


}