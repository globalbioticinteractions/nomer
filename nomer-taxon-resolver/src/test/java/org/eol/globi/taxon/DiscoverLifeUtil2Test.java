package org.eol.globi.taxon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.eol.globi.service.TaxonUtil.generateTaxonPathNames;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class DiscoverLifeUtil2Test {

    @Test
    public void compareLocalVersionToRemoteVersion() throws IOException {
        DiscoverLifeTestUtil.compareLocalVersionToRemoteVersion(
                "/org/globalbioticinteractions/nomer/match/discoverlife/bees.xml.gz",
                DiscoverLifeUtil.URL_ENDPOINT_DISCOVER_LIFE + "/nh/id/20q/Apoidea_species.xml"
        );
    }


    @Test
    public void parseXMLRecord() throws ParserConfigurationException, XPathExpressionException, IOException, SAXException {
        Map<String, String> nameMap = new TreeMap<String, String>() {{
            put("kingdom", "Animalia");
            put("phylum", "Arthropoda");
            put("class", "Insecta");
            put("order", "Hymenoptera");
            put("superfamily", "Apoidea");
        }};

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().parse(getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/discoverlife/agapostemon_texanus.xml"));

        Node setNode = (Node) XmlUtil.applyXPath(doc, "set", XPathConstants.NODE);


        putTextValueForElement(nameMap, setNode, "name", PropertyAndValueDictionary.NAME);
        putTextValueForElement(nameMap, setNode, "authorship", PropertyAndValueDictionary.AUTHORSHIP);

        Node level = setNode.getAttributes().getNamedItem("level");
        String taxonomicRank = level == null ? null : level.getTextContent();
        nameMap.put(PropertyAndValueDictionary.RANK, taxonomicRank);
        nameMap.put(taxonomicRank, nameMap.get(PropertyAndValueDictionary.NAME));


        NodeList attr = (NodeList) XmlUtil.applyXPath(setNode, "//attributes", XPathConstants.NODESET);

        assertThat(attr.getLength(), is(1));

        NodeList childNodes = attr.item(0).getChildNodes();

        assertThat(childNodes.getLength(), is(69));

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
                        List<String> ranks = Arrays.asList("Family", "Subfamily", "Tribe", "Subtribe", "Genus", "Subgenus");
                        if (ranks.contains(keyCurrent)) {
                            nameMap.put(StringUtils.lowerCase(keyCurrent), valuesCurrent.get(0).asText());
                        }
                    }
                }
            }
        }

        assertNotNull(objectNode);



        String pathNames = generateTaxonPathNames(nameMap, Arrays.asList("kingdom", "phylum", "class", "order", "family", "subfamily", "tribe", "subtribe", "genus", "subgenus", "subspecies"), "", "genus", "specificEpithet", "subspecificEpithet", "species");

        nameMap.put(PropertyAndValueDictionary.PATH_NAMES, pathNames);

        String[] ranks = StringUtils.splitByWholeSeparator(pathNames, CharsetConstant.SEPARATOR);
        List<String> path = new ArrayList<>();

        for (String rank : ranks) {
            path.add(nameMap.get(rank));
        }

        String pathString = StringUtils.join(path, CharsetConstant.SEPARATOR);
        nameMap.put(PropertyAndValueDictionary.PATH, pathString);

        Taxon taxon = TaxonUtil.mapToTaxon(nameMap);
        assertThat(taxon.getName(), is("Agapostemon texanus"));
        assertThat(taxon.getRank(), is("species"));
        assertThat(taxon.getPath(), is("Animalia | Arthropoda | Insecta | Hymenoptera | Halictidae | Halictinae | Halictini | Caenohalictina | Agapostemon | Agapostemon | Agapostemon texanus"));
        assertThat(taxon.getPathNames(), is("kingdom | phylum | class | order | family | subfamily | tribe | subtribe | genus | subgenus | species"));


    }

    private void putTextValueForElement(Map<String, String> nameMap, Node setNode, String sourceElementName, String targetName) throws XPathExpressionException {
        Node nameNode = (Node) XmlUtil.applyXPath(setNode, sourceElementName, XPathConstants.NODE);
        if (nameNode != null) {
            nameMap.put(targetName, nameNode.getTextContent());
        }
    }


}