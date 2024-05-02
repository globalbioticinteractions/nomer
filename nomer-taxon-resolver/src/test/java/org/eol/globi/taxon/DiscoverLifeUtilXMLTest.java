package org.eol.globi.taxon;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.globalbioticinteractions.nomer.match.ParserServiceGBIF;
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DiscoverLifeUtilXMLTest {

    @Test
    public void parseXMLRecord() throws ParserConfigurationException, XPathExpressionException, IOException, SAXException {
        Document doc = docForResource("/org/globalbioticinteractions/nomer/match/discoverlife/agapostemon_texanus.xml");

        Map<String, String> nameMap = DiscoverLifeUtilXML.parseFocalTaxon(doc);

        Taxon taxon = TaxonUtil.mapToTaxon(nameMap);
        assertThat(taxon.getName(), is("Agapostemon texanus"));
        assertThat(taxon.getStatus().getName(), is("accepted"));
        assertThat(taxon.getRank(), is("species"));
        assertThat(taxon.getId(), is("https://www.discoverlife.org/mp/20q?guide=Apoidea_species&search=Agapostemon+texanus"));
        assertThat(taxon.getAuthorship(), is("Cresson, 1872"));
        assertThat(taxon.getPath(), is("Animalia | Arthropoda | Insecta | Hymenoptera | Halictidae | Halictinae | Halictini | Caenohalictina | Agapostemon | Agapostemon | Agapostemon texanus"));
        assertThat(taxon.getPathNames(), is("kingdom | phylum | class | order | family | subfamily | tribe | subtribe | genus | subgenus | species"));
    }

    @Test
    public void parseRelatedNames() throws ParserConfigurationException, XPathExpressionException, IOException, SAXException {
        Document doc = docForResource("/org/globalbioticinteractions/nomer/match/discoverlife/andrena_cressonii.xml");

        List<Taxon> taxons = DiscoverLifeUtilXML.parseRelatedNames(doc, new ParserServiceGBIF());

        assertThat(taxons.size(), is(17));

        Taxon lastTaxon = taxons.get(taxons.size() - 1);
        assertThat(lastTaxon.getName(), is("Andrena cressonii"));
        assertThat(lastTaxon.getAuthorship(), is("Robertson, 1891"));
        assertThat(lastTaxon.getStatus().getName(), is(NameType.SYNONYM_OF.name()));
    }

    @Test
    public void parseRelatedNamesWithReplacementName() throws ParserConfigurationException, XPathExpressionException, IOException, SAXException {
        Document doc = docForResource("/org/globalbioticinteractions/nomer/match/discoverlife/osmia_versicolor.xml");

        List<Taxon> taxons = DiscoverLifeUtilXML.parseRelatedNames(doc, new ParserServiceGBIF());

        assertThat(taxons.size(), is(5));

        Taxon lastTaxon = taxons.get(2);
        assertThat(lastTaxon.getName(), is("Megachile algarbiensis"));
        assertThat(lastTaxon.getAuthorship(), is("Strand, 1917"));
        assertThat(lastTaxon.getStatus().getName(), is(NameType.SYNONYM_OF.name()));
    }

    @Test
    public void parseRelatedNamesWithoutRelatedNames() throws ParserConfigurationException, XPathExpressionException, IOException, SAXException {
        Document doc = docForResource("/org/globalbioticinteractions/nomer/match/discoverlife/anthidium_cochimi.xml");

        List<Taxon> taxons = DiscoverLifeUtilXML.parseRelatedNames(doc, new ParserServiceGBIF());

        assertThat(taxons.size(), is(0));

        Map<String, String> focalTaxon = DiscoverLifeUtilXML.parseFocalTaxon(doc);

        Taxon lastTaxon = TaxonUtil.mapToTaxon(focalTaxon);

        assertThat(lastTaxon.getName(), is("Anthidium cochimi"));
        assertThat(lastTaxon.getAuthorship(), is("Snelling, 1992"));
        assertThat(lastTaxon.getStatus().getName(), is("accepted"));
    }

    @Test
    public void parseRelatedNamesDanglingParenthesis() throws ParserConfigurationException, XPathExpressionException, IOException, SAXException {
        Document doc = docForResource("/org/globalbioticinteractions/nomer/match/discoverlife/pseudoanthidium_tropicum.xml");

        List<Taxon> taxa = DiscoverLifeUtilXML.parseRelatedNames(doc, new ParserServiceGBIF());

        assertThat(taxa.size(), is(3));


        Taxon lastTaxon = taxa.get(taxa.size() - 1);

        assertThat(lastTaxon.getName(), is("Pseudoanthidium nanum tropicum"));
        assertThat(lastTaxon.getAuthorship(), is("(Warncke, 1982)"));
    }

    @Test
    public void parseRelatedNamesUnpublishedSynonymy() throws ParserConfigurationException, XPathExpressionException, IOException, SAXException {
        Document doc = docForResource("/org/globalbioticinteractions/nomer/match/discoverlife/nomada_vicina.xml");

        List<Taxon> taxa = DiscoverLifeUtilXML.parseRelatedNames(doc, new ParserServiceGBIF());

        assertThat(taxa.size(), is(3));


        Taxon lastTaxon = taxa.get(taxa.size() - 1);

        assertThat(lastTaxon.getName(), is("Nomada vicina stevensi"));
        assertThat(lastTaxon.getAuthorship(), is("Swenk, 1913"));
    }

    @Test
    public void parseRelatedNamesSubgenericPlacement() throws ParserConfigurationException, XPathExpressionException, IOException, SAXException {
        Document doc = docForResource("/org/globalbioticinteractions/nomer/match/discoverlife/megachile_scheviakovi.xml");

        List<Taxon> taxa = DiscoverLifeUtilXML.parseRelatedNames(doc, new ParserServiceGBIF());

        assertThat(taxa.size(), is(1));


        Taxon lastTaxon = taxa.get(taxa.size() - 1);

        assertThat(lastTaxon.getName(), is("Megachile scheviakovi"));
        assertThat(lastTaxon.getAuthorship(), is("Cockerell, 1928"));
    }


    @Test
    public void parseNamesWithStatus() throws ParserConfigurationException, XPathExpressionException, IOException, SAXException {
        Document doc = docForResource("/org/globalbioticinteractions/nomer/match/discoverlife/andrena_erberi.xml");

        List<Taxon> taxons = DiscoverLifeUtilXML.parseRelatedNames(doc, new ParserServiceGBIF());

        assertThat(taxons.size(), is(7));

        Taxon secondTaxon = taxons.get(1);
        assertThat(secondTaxon.getName(), is("Andrena fulvocrustatus"));
        assertThat(secondTaxon.getAuthorship(), is("Dours, 1873"));
        assertThat(secondTaxon.getPath(), is("Andrena | Campylogaster | fulvocrustatus"));
        assertThat(secondTaxon.getPathNames(), is("genus | infragenericEpithet | specificEpithet"));
        assertThat(secondTaxon.getStatus().getName(), is(NameType.SYNONYM_OF.name()));

        Taxon thirdTaxon = taxons.get(3);
        assertThat(thirdTaxon.getName(), is("Andena squamigera"));
        assertThat(thirdTaxon.getAuthorship(), is("Bramson, 1879"));
        assertThat(thirdTaxon.getStatus().getName(), is(NameType.HOMONYM_OF.name()));

        Taxon fourthTaxon = taxons.get(4);
        assertThat(fourthTaxon.getName(), is("Andrena erberi var. sanguiniventris"));
        assertThat(fourthTaxon.getAuthorship(), is("Friese, 1921"));
        assertThat(fourthTaxon.getStatus().getName(), is(NameType.SYNONYM_OF.name()));


        Taxon lastTaxon = taxons.get(taxons.size() - 1);
        assertThat(lastTaxon.getName(), is("Andrena erberi migrans"));
        assertThat(lastTaxon.getAuthorship(), is("Warncke, 1967"));
        assertThat(lastTaxon.getStatus().getName(), is(NameType.SYNONYM_OF.name()));
    }

    @Test
    public void parseNamesWithValidSubspecies() throws ParserConfigurationException, XPathExpressionException, IOException, SAXException {
        Document doc = docForResource("/org/globalbioticinteractions/nomer/match/discoverlife/melissodes_tepida.xml");

        List<Taxon> taxons = DiscoverLifeUtilXML.parseRelatedNames(doc, new ParserServiceGBIF());

        assertThat(taxons.size(), is(5));

        Taxon firstTaxon = taxons.get(0);
        assertThat(firstTaxon.getName(), is("Melissodes tepida"));
        assertThat(firstTaxon.getRank(), is("species"));
        assertThat(firstTaxon.getAuthorship(), is("Cresson, 1878"));
        assertThat(firstTaxon.getPath(), is("Melissodes | tepida"));
        assertThat(firstTaxon.getPathNames(), is("genus | specificEpithet"));
        assertThat(firstTaxon.getStatus().getName(), is(NameType.SYNONYM_OF.name()));

        Taxon secondTaxon = taxons.get(2);
        assertThat(secondTaxon.getName(), is("Melissodes tepidus timberlakei"));
        assertThat(secondTaxon.getRank(), is("subspecies"));
        assertThat(secondTaxon.getAuthorship(), is("Cockerell, 1926"));
        assertThat(secondTaxon.getStatus().getName(), is(DiscoverLifeUtilXML.VALID_SUBSPECIES_OF));
    }

    @Test
    public void parseCommonNames() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        Document doc = docForResource("/org/globalbioticinteractions/nomer/match/discoverlife/agapostemon_texanus.xml");

        List<Taxon> names = DiscoverLifeUtilXML.parseRelatedNames(doc, new ParserServiceDiscoverLifeCustom());
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

        Map<String, String> nameMap = DiscoverLifeUtilXML.parseFocalTaxon(doc);

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

        DiscoverLifeUtilXML.splitRecords(is, records::add);
        assertThat(records.get(0), is(IOUtils.toString(getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/discoverlife/record_Adrenidae.xml"), StandardCharsets.UTF_8)));
        assertThat(records.get(records.size() - 1), is(IOUtils.toString(getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/discoverlife/record_Acamptopoeum_melanogaster.xml"), StandardCharsets.UTF_8)));
    }

    @Test
    public void parseRecords() throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
        InputStream is = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/discoverlife/bees-head.xml");

        List<Triple<Term, NameType, Taxon>> records = new ArrayList<>();

        DiscoverLifeUtilXML.parse(is, new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                records.add(Triple.of(providedTerm, nameType, resolvedTaxon));
            }
        }, new ParserServiceDiscoverLifeCustom());

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
    public void parseNameAlt7() throws PropertyEnricherException {
        String name = "Andrena erberi var. sanguiniventris Friese, 1922";
        Taxon matched = new ParserServiceGBIF().parse(new TaxonImpl(), name);
        assertThat(matched.getAuthorship(), is("Friese, 1922"));
        assertThat(matched.getName(), is("Andrena erberi var. sanguiniventris"));
    }

    @Test
    public void patchCommonNames() {
        String name = "Protandrena bachue Gonzalez and Ruz, 2007 Rhophitulus bachue (Gonzalez and Ruz, 2007)";
        name = DiscoverLifeUtilXML.ensureDelimiters(name);

        assertThat(name, is("Protandrena bachue Gonzalez and Ruz, 2007 ;Rhophitulus bachue (Gonzalez and Ruz, 2007);"));
    }

    @Test
    public void patchCommonNames2() {
        String name = "Centris (Melanocentris) rhodoprocta Moure and Seabra, 1960, replacement name Centris (Melacentris) rufosuffusa Cockerell, 1900";
        name = DiscoverLifeUtilXML.ensureDelimitersWithNote(name);

        assertThat(name, is("Centris (Melanocentris) rhodoprocta Moure and Seabra, 1960, replacement name ;Centris (Melacentris) rufosuffusa Cockerell, 1900"));
    }

    @Test
    public void patchCommonNames3() {
        String name = "Centris (Melanocentris) rhodoprocta (Moure and Seabra, 1960), replacement name Centris (Melacentris) rufosuffusa Cockerell, 1900";
        name = DiscoverLifeUtilXML.ensureDelimitersWithNote(name);

        assertThat(name, is("Centris (Melanocentris) rhodoprocta (Moure and Seabra, 1960), replacement name ;Centris (Melacentris) rufosuffusa Cockerell, 1900"));
    }


    @Test
    public void scrubNotesFromName() {
        assertThat(
                DiscoverLifeUtilXML.scrubNotesFromName("Donald duckus_homonym"),
                is("Donald duckus")
        );
    }


}