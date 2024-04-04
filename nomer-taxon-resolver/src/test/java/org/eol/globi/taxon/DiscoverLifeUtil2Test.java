package org.eol.globi.taxon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.TaxonUtil;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
import java.util.Scanner;
import java.util.TreeMap;
import java.util.function.Consumer;

import static org.eol.globi.service.TaxonUtil.generateTaxonPathNames;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DiscoverLifeUtil2Test {

    public static final List<String> RANKS = Arrays.asList("Family", "Subfamily", "Tribe", "Subtribe", "Genus", "Subgenus");

    @Test
    public void compareLocalVersionToRemoteVersion() throws IOException {
        DiscoverLifeTestUtil.compareLocalVersionToRemoteVersion(
                "/org/globalbioticinteractions/nomer/match/discoverlife/bees.xml.gz",
                DiscoverLifeUtil.URL_ENDPOINT_DISCOVER_LIFE + "/nh/id/20q/Apoidea_species.xml"
        );
    }


    @Test
    public void parseXMLRecord() throws ParserConfigurationException, XPathExpressionException, IOException, SAXException {
        InputStream is = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/discoverlife/agapostemon_texanus.xml");

        Map<String, String> nameMap = parseRecord(is);

        Taxon taxon = TaxonUtil.mapToTaxon(nameMap);
        assertThat(taxon.getName(), is("Agapostemon texanus"));
        assertThat(taxon.getRank(), is("species"));
        assertThat(taxon.getPath(), is("Animalia | Arthropoda | Insecta | Hymenoptera | Halictidae | Halictinae | Halictini | Caenohalictina | Agapostemon | Agapostemon | Agapostemon texanus"));
        assertThat(taxon.getPathNames(), is("kingdom | phylum | class | order | family | subfamily | tribe | subtribe | genus | subgenus | species"));
    }

    @Test
    public void parseAcamptopoeum_melanogaster() throws ParserConfigurationException, XPathExpressionException, IOException, SAXException {
        InputStream is = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/discoverlife/record_Acamptopoeum_melanogaster.xml");

        Map<String, String> nameMap = parseRecord(is);

        Taxon taxon = TaxonUtil.mapToTaxon(nameMap);
        assertThat(taxon.getName(), is("Acamptopoeum melanogaster"));
        assertThat(taxon.getRank(), is("species"));
        assertThat(taxon.getPath(), is("Animalia | Arthropoda | Insecta | Hymenoptera | Andrenidae | Panurginae | Calliopsini | None | Acamptopoeum | None | Acamptopoeum melanogaster"));
        assertThat(taxon.getPathNames(), is("kingdom | phylum | class | order | family | subfamily | tribe | subtribe | genus | subgenus | species"));
    }

    @Test
    public void splitRecords() throws IOException {
        InputStream is = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/discoverlife/bees-head.xml");

        List<String> records = new ArrayList<>();
        Consumer<String> recordConsumer = new Consumer<String>() {
            @Override
            public void accept(String s) {
                records.add(s);
            }
        };

        splitRecords(is, recordConsumer);
        assertThat(records.get(0), is(IOUtils.toString(getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/discoverlife/record_Adrenidae.xml"), StandardCharsets.UTF_8)));
        assertThat(records.get(records.size() - 1), is(IOUtils.toString(getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/discoverlife/record_Acamptopoeum_melanogaster.xml"), StandardCharsets.UTF_8)));
    }

    private void splitRecords(InputStream is, Consumer<String> lineConsumer) {
        Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name());
        while (scanner.hasNext()) {
            String record = nextRecord(scanner);
            if (StringUtils.isNotBlank(record)) {
                lineConsumer.accept(record);
            }
        }
    }

    private String nextRecord(Scanner scanner) {
        String record = null;
        scanner.useDelimiter("__START__");
        scanner.next();
        if (scanner.hasNext()) {
            scanner.useDelimiter("\n<set level=");
            scanner.next();
            if (scanner.hasNext()) {
                scanner.useDelimiter("__STOP__");
                record = StringUtils.trim(scanner.next());
            }
        }

        return record;
    }


    private Map<String, String> parseRecord(InputStream is) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
        Map<String, String> nameMap = new TreeMap<String, String>() {{
            put("kingdom", "Animalia");
            put("phylum", "Arthropoda");
            put("class", "Insecta");
            put("order", "Hymenoptera");
            put("superfamily", "Apoidea");
        }};

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().parse(is);

        Node setNode = (Node) XmlUtil.applyXPath(doc, "set", XPathConstants.NODE);


        putTextValueForElement(nameMap, setNode, "name", PropertyAndValueDictionary.NAME);
        putTextValueForElement(nameMap, setNode, "authorship", PropertyAndValueDictionary.AUTHORSHIP);

        Node level = setNode.getAttributes().getNamedItem("level");
        String taxonomicRank = level == null ? null : level.getTextContent();
        nameMap.put(PropertyAndValueDictionary.RANK, taxonomicRank);
        nameMap.put(taxonomicRank, nameMap.get(PropertyAndValueDictionary.NAME));


        NodeList attr = (NodeList) XmlUtil.applyXPath(setNode, "//attributes", XPathConstants.NODESET);

        NodeList childNodes = attr.item(0).getChildNodes();

        String keyCurrent = "";
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        ArrayNode valuesCurrent = new ObjectMapper().createArrayNode();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            String key = childNode.getNodeName();
            if (StringUtils.equals("character", key)) {
                keyCurrent = childNode.getTextContent();
                valuesCurrent = new ObjectMapper().createArrayNode();
            } else if (StringUtils.equals("state", key)) {
                if (StringUtils.isNotBlank(keyCurrent)) {
                    valuesCurrent.add(childNode.getTextContent());
                    if (valuesCurrent.size() > 0 && objectNode != null) {
                        if (RANKS.contains(keyCurrent)) {
                            nameMap.put(StringUtils.lowerCase(keyCurrent), valuesCurrent.get(0).asText());
                        }
                    }
                }
            }
        }

        String pathNames = generateTaxonPathNames(nameMap, Arrays.asList("kingdom", "phylum", "class", "order", "family", "subfamily", "tribe", "subtribe", "genus", "subgenus", "subspecies"), "", "genus", "specificEpithet", "subspecificEpithet", "species");

        nameMap.put(PropertyAndValueDictionary.PATH_NAMES, pathNames);

        String[] ranks = StringUtils.splitByWholeSeparator(pathNames, CharsetConstant.SEPARATOR);
        List<String> path = new ArrayList<>();

        for (String rank : ranks) {
            path.add(nameMap.get(rank));
        }

        String pathString = StringUtils.join(path, CharsetConstant.SEPARATOR);
        nameMap.put(PropertyAndValueDictionary.PATH, pathString);
        return nameMap;
    }

    private void putTextValueForElement(Map<String, String> nameMap, Node setNode, String sourceElementName, String targetName) throws XPathExpressionException {
        Node nameNode = (Node) XmlUtil.applyXPath(setNode, sourceElementName, XPathConstants.NODE);
        if (nameNode != null) {
            nameMap.put(targetName, nameNode.getTextContent());
        }
    }


}