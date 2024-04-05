package org.eol.globi.taxon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.TaxonUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
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

public class DiscoverLifeUtil2 {

    public static final List<String> RANKS = Arrays.asList("Family", "Subfamily", "Tribe", "Subtribe", "Genus", "Subgenus");

    public static void splitRecords(InputStream is, Consumer<String> lineConsumer) {
        Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name());
        while (scanner.hasNext()) {
            String record = nextRecord(scanner);
            if (StringUtils.isNotBlank(record)) {
                lineConsumer.accept(record);
            }
        }
    }

    public static String nextRecord(Scanner scanner) {
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

    public static void parse(InputStream is, final TermMatchListener listener) throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();

        splitRecords(is, new Consumer<String>() {
            @Override
            public void accept(String recordXml) {
                try {
                    InputStream recordInputStream = IOUtils.toInputStream(
                            escapeUnescapedAmpersands(recordXml), StandardCharsets.UTF_8
                    );

                    Document doc = builder.parse(recordInputStream
                    );
                    Map<String, String> nameMap = parseFocalTaxon(doc);

                    DiscoverLifeUtil.emitNameRelation(listener, nameMap, TaxonUtil.mapToTaxon(nameMap));


                } catch (SAXException | IOException | XPathExpressionException e) {
                    try {
                        IOUtils.copy(IOUtils.toInputStream(recordXml, StandardCharsets.UTF_8), System.err);
                    } catch (IOException e1) {
                        //
                    }
                    throw new RuntimeException("failed to parse DiscoverLife record [" + recordXml + "]", e);
                }

            }

            private String escapeUnescapedAmpersands(String recordXml) {
                return StringUtils.replace(recordXml, " & ", " &amp; ");
            }
        });
    }

    static Map<String, String> parseFocalTaxon(Document doc) throws XPathExpressionException {
        Map<String, String> nameMap = new TreeMap<String, String>() {{
            put("kingdom", "Animalia");
            put("phylum", "Arthropoda");
            put("class", "Insecta");
            put("order", "Hymenoptera");
            put("superfamily", "Apoidea");
        }};

        Node setNode = (Node) XmlUtil.applyXPath(doc, "set", XPathConstants.NODE);


        putTextValueForElement(nameMap, setNode, "name", PropertyAndValueDictionary.NAME);
        putTextValueForElement(nameMap, setNode, "authority", PropertyAndValueDictionary.AUTHORSHIP);
        if (StringUtils.isNotBlank(nameMap.get(PropertyAndValueDictionary.AUTHORSHIP))) {
            nameMap.put(PropertyAndValueDictionary.STATUS_ID, "discoverlife:accepted");
            nameMap.put(PropertyAndValueDictionary.STATUS_LABEL, "accepted");
        }

        Node level = setNode.getAttributes().getNamedItem("level");
        String taxonomicRank = level == null ? null : level.getTextContent();
        nameMap.put(PropertyAndValueDictionary.RANK, taxonomicRank);
        String name = nameMap.get(PropertyAndValueDictionary.NAME);
        nameMap.put(taxonomicRank, name);
        nameMap.put(PropertyAndValueDictionary.EXTERNAL_ID, DiscoverLifeUtil.URL_ENDPOINT_DISCOVER_LIFE_SEARCH + StringUtils.replace(name, " ", "+"));


        NodeList attr = (NodeList) XmlUtil.applyXPath(setNode, "//attributes", XPathConstants.NODESET);

        if (attr != null && attr.getLength() > 0) {

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
        }
        return nameMap;
    }

    private static void putTextValueForElement(Map<String, String> nameMap, Node setNode, String sourceElementName, String targetName) throws XPathExpressionException {
        Node nameNode = (Node) XmlUtil.applyXPath(setNode, sourceElementName, XPathConstants.NODE);
        if (nameNode != null) {
            nameMap.put(targetName, nameNode.getTextContent());
        }
    }
}
