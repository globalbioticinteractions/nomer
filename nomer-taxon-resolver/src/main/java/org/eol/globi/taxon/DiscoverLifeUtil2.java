package org.eol.globi.taxon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
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
import java.util.Objects;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eol.globi.service.TaxonUtil.generateTaxonPathNames;
import static org.eol.globi.taxon.DiscoverLifeUtil.emitNameRelatedToFocalTaxon;

public class DiscoverLifeUtil2 {

    public static final List<String> RANKS = Arrays.asList("Family", "Subfamily", "Tribe", "Subtribe", "Genus", "Subgenus");
    public static final String NAME_PATTERN_AUTHORSHIP_PARENTHESES = "(?<name>[A-Z][a-z]+[ ][a-z]+)[ ]+(?<authorship>[(][^,]+[,][ ][0-9]{4}[)])";
    public static final String NAME_PATTERN_WITH_NOTE = "(?<name>[A-Z][a-z]+[ ][a-z]+)(?<note>[_][_a-z]+)[ ]+(?<authorship>[^,]+[,][ ][0-9]{4})";
    public static final String NAME_PATTERN_WITH_PARENTHESIS = "(?<name>[A-Z][a-z]+[ ][(][A-Z][a-z]+[)][ ][a-z]+)[ ]+(?<authorship>[^,]+[,][ ][0-9]{4})";
    public static final String NAME_PATTERN_AUTHORSHIP_MULTIPLE_AUTHORS = "(?<name>[A-Z][a-z]+[ ][a-z]+)[ ]+(?<authorship>([A-Z][a-z]+)([ ]and[ ])([A-Z][a-z]+)[,][ ][0-9]{4})";
    public static final List<String> NAME_PATTERNS = Arrays.asList(NAME_PATTERN_AUTHORSHIP_PARENTHESES, NAME_PATTERN_WITH_NOTE, NAME_PATTERN_WITH_PARENTHESIS, NAME_PATTERN_AUTHORSHIP_MULTIPLE_AUTHORS);

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

                    Document doc = builder.parse(recordInputStream);
                    Map<String, String> nameMap = parseFocalTaxon(doc);
                    DiscoverLifeUtil.emitNameRelation(listener, nameMap, TaxonUtil.mapToTaxon(nameMap));

                    List<Taxon> relatedTaxa = parseRelatedNames(doc);

                    for (Taxon relatedTaxon : relatedTaxa) {
                        emitNameRelatedToFocalTaxon(listener, nameMap, TaxonUtil.mapToTaxon(nameMap), TaxonUtil.taxonToMap(relatedTaxon), relatedTaxon);
                    }


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

    public static List<Taxon> parseRelatedNames(Document doc) throws XPathExpressionException {
        Stream<Taxon> relatedNames = Stream.empty();
        Node commonNameNode = (Node) XmlUtil.applyXPath(doc, "set/common_name", XPathConstants.NODE);

        if (commonNameNode != null) {
            String textContent = ensureDelimitersWithNote(ensureDelimiters(commonNameNode.getTextContent()));
            relatedNames = Stream
                    .of(StringUtils.split(textContent, ";"))
                    .map(StringUtils::trim)
                    .map(name -> {
                        String[] nameParts = StringUtils.split(name, ",");
                        List<String> namesWithoutRemarks = Arrays.asList(nameParts).subList(0, nameParts.length > 2 ? nameParts.length - 1 : nameParts.length);
                        return StringUtils.join(namesWithoutRemarks, ",");
                    })
                    .map(DiscoverLifeUtil2::parse)
                    .filter(Objects::nonNull);
        }
        return relatedNames.collect(Collectors.toList());
    }

    public static String ensureDelimiters(String name) {
        Pattern compile = Pattern.compile("(?<year>[0-9]{4})(?<parenthesis>[ )]+)");
        Matcher matcher = compile.matcher(name);
        while (matcher.find()) {
            String target = matcher.group("year") + matcher.group("parenthesis");
            name = StringUtils.replace(name, target, target + ";");
        }
        return name;
    }

    public static String ensureDelimitersWithNote(String name) {
        Pattern compile = Pattern.compile("(?<year>[0-9]{4})(?<parenthesis>[ ),]+)(?<note>[^A-Z]+)");
        Matcher matcher = compile.matcher(name);
        while (matcher.find()) {
            String target = matcher.group("year") + matcher.group("parenthesis") + matcher.group("note");
            name = StringUtils.replace(name, target, target + ";");
        }
        return name;
    }

    public static Taxon parse(String name) {
        Taxon matched = null;

        for (String namePattern : NAME_PATTERNS) {
            Pattern compile = Pattern.compile(namePattern);
            Matcher matcher = compile.matcher(name);
            if (matcher.matches()) {
                matched = new TaxonImpl(matcher.group("name"));
                matched.setAuthorship(matcher.group("authorship"));
                break;
            }

        }
        return matched;
    }
}
