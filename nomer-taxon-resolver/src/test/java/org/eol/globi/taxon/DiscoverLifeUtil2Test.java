package org.eol.globi.taxon;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.TaxonUtil;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public void parseCommonNames() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        Document doc = docForResource("/org/globalbioticinteractions/nomer/match/discoverlife/agapostemon_texanus.xml");

        Node commonNameNode = (Node) XmlUtil.applyXPath(doc, "set/common_name", XPathConstants.NODE);

        Stream<Taxon> relatedNames = Stream
                .of(StringUtils.split(commonNameNode.getTextContent(), ";"))
                .map(StringUtils::trim)
                .map(name -> {
                    Matcher matcher = Pattern.compile("(?<name>.*)(?<authorship>[A-Z].*[,][ ][1-2][0-9]{3}$)").matcher(name);
                    TaxonImpl name1 = new TaxonImpl(name);
                    if (matcher.matches()) {
                        name1.setName(StringUtils.trim(matcher.group("name")));
                        name1.setAuthorship(StringUtils.trim(matcher.group("authorship")));
                    }
                    return name1;
                });
        List<Taxon> names = relatedNames.collect(Collectors.toList());
        assertThat(names.size(), is(14));

        assertThat(names.get(0).getName(), is("Agapostemon texanus subtilior") );
        assertThat(names.get(0).getAuthorship(), is("Cockerell, 1898") );
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

        assertThat(records.size(), is(13));

        assertThat(records.get(0).getLeft().getName(), is("Andrenidae"));
        assertThat(records.get(0).getMiddle(), is(NameType.HAS_ACCEPTED_NAME));
        assertThat(records.get(0).getRight().getName(), is("Andrenidae"));
        assertThat(records.get(0).getRight().getRank(), is("family"));

        assertThat(records.get(12).getLeft().getName(), is("Acamptopoeum melanogaster"));
        assertThat(records.get(12).getMiddle(), is(NameType.HAS_ACCEPTED_NAME));
        assertThat(records.get(12).getRight().getName(), is("Acamptopoeum melanogaster"));
        assertThat(records.get(12).getRight().getRank(), is("species"));
        assertThat(records.get(12).getRight().getPath(), is("Animalia | Arthropoda | Insecta | Hymenoptera | Andrenidae | Panurginae | Calliopsini | None | Acamptopoeum | None | Acamptopoeum melanogaster"));
    }


}