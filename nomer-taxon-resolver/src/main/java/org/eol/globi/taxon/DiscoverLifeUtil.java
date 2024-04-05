package org.eol.globi.taxon;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TaxonUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DiscoverLifeUtil {

    private static final List<String> PATH_STATIC = Arrays.asList("Animalia", "Arthropoda", "Insecta", "Hymenoptera");
    public static final String URL_ENDPOINT_DISCOVER_LIFE = "https://www.discoverlife.org";
    public static final String URL_ENDPOINT_DISCOVER_LIFE_SEARCH = URL_ENDPOINT_DISCOVER_LIFE +
            "/mp/20q?search=";
    private static final List<String> PATH_STATIC_IDS = PATH_STATIC
            .stream()
            .map(x -> StringUtils.prependIfMissing(x, URL_ENDPOINT_DISCOVER_LIFE_SEARCH))
            .collect(Collectors.toList());
    private static final List<String> PATH_NAMES_STATIC = Arrays.asList("kingdom", "phylum", "class", "order", "family");
    public static final String HOMONYM_SUFFIX = "_homonym";
    public static final Pattern PATTERN_GENUS = Pattern.compile("(?<genus>[A-Z][a-z]+)(.*)");

    public static InputStream getBeeNameTable(ResourceService service, String discoverLifeUrl) throws IOException {

        InputStream retrieve = service.retrieve(URI.create(discoverLifeUrl));
        Document document = Jsoup.parse(retrieve, "UTF-8", discoverLifeUrl);
        document.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .charset(StandardCharsets.UTF_8)
                .escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml);
        return new ByteArrayInputStream(document.html().getBytes(StandardCharsets.UTF_8));
    }

    static void parseNames(Node familyNameNode, Node nameNodeCandidate, TermMatchListener listener) throws XPathExpressionException {

        Node currentNode = nameNodeCandidate.getParentNode();

        if (StringUtils.equals("i", currentNode.getNodeName())) {
            Map<String, String> taxonMap = new TreeMap<>();
            String taxonName = trimNameNodeTextContent(nameNodeCandidate.getTextContent());
            taxonMap.put(PropertyAndValueDictionary.NAME, taxonName);

            Node expectedTextNode = currentNode.getNextSibling();
            Node authorshipNode =
                    expectedTextNode == null
                            ? null
                            : expectedTextNode.getNextSibling();

            if (authorshipNode != null) {
                enrichFromAuthorString(StringUtils.trim(authorshipNode.getTextContent()), taxonMap);
                taxonMap.put("status", "accepted");
            }

            String id = nameNodeCandidate.getAttributes().getNamedItem("href") == null
                    ? null
                    : StringUtils.trim(nameNodeCandidate
                    .getAttributes()
                    .getNamedItem("href")
                    .getTextContent());


            Taxon parsedTaxon = toTaxon(taxonMap);
            parsedTaxon.setExternalId(id);

            Taxon focalTaxon = familyNameNode == null
                    ? parsedTaxon
                    : getTaxonForNode(familyNameNode, parsedTaxon);

            currentNode = authorshipNode;

            focalTaxon.setExternalId(StringUtils.prependIfMissing(id, URL_ENDPOINT_DISCOVER_LIFE));

            emitNameRelation(listener, taxonMap, focalTaxon);
            handleRelatedNames(listener, taxonMap, currentNode, focalTaxon);
        }

    }

    public static void emitNameRelation(TermMatchListener listener, Map<String, String> taxonMap, Taxon focalTaxon) {
        if (isHomonym(taxonMap)) {
            listener.foundTaxonForTerm(
                    null,
                    focalTaxon,
                    NameType.HOMONYM_OF,
                    null
            );
        } else {
            listener.foundTaxonForTerm(
                    null,
                    focalTaxon,
                    NameType.HAS_ACCEPTED_NAME,
                    focalTaxon
            );

        }
    }

    private static boolean isHomonym(Map<String, String> taxonMap) {
        String name = taxonMap.get(PropertyAndValueDictionary.NAME);
        return StringUtils.contains(name, HOMONYM_SUFFIX);
    }

    private static void handleRelatedNames(
            TermMatchListener listener,
            Map<String, String> focalTaxonMap,
            Node currentNode,
            Taxon focalTaxon) {
        while ((currentNode = currentNode == null ? null : currentNode.getNextSibling()) != null) {

            if ("i".equals(currentNode.getNodeName())) {

                Map<String, String> relatedTaxonMap = new TreeMap<>();

                Node authorshipNodeCandidate = currentNode.getNextSibling();
                String authorshipString = enrichFromNameString(relatedTaxonMap, currentNode.getTextContent(), authorshipNodeCandidate == null ? null : authorshipNodeCandidate.getTextContent());

                currentNode = currentNode.getNextSibling();

                enrichFromAuthorString(StringUtils.trim(authorshipString), relatedTaxonMap);

                Taxon relatedTaxon = toTaxon(relatedTaxonMap);
                String id = urlForName(relatedTaxon);
                relatedTaxon.setExternalId(id);

                emitNameRelatedToFocalTaxon(listener, focalTaxonMap, focalTaxon, relatedTaxonMap, relatedTaxon);
            }
        }
    }

    public static void emitNameRelatedToFocalTaxon(
            TermMatchListener listener,
            Map<String, String> focalTaxonMap,
            Taxon focalTaxon,
            Map<String, String> relatedTaxonMap,
            Taxon relatedTaxon) {
        boolean relatedNameIsHomonym = isHomonym(relatedTaxonMap);
        if (relatedNameIsHomonym) {
            listener.foundTaxonForTerm(
                    null,
                    relatedTaxon,
                    NameType.HOMONYM_OF,
                    null
            );
        }

        boolean focalTaxonIsHomonym = isHomonym(focalTaxonMap);
        if (focalTaxonIsHomonym) {
            listener.foundTaxonForTerm(
                    null,
                    focalTaxon,
                    NameType.HOMONYM_OF,
                    null
            );
        }

        if (!relatedNameIsHomonym
                && !focalTaxonIsHomonym
                && !isSelfReferential(relatedTaxon, focalTaxon)) {

            listener.foundTaxonForTerm(
                    null,
                    relatedTaxon,
                    NameType.SYNONYM_OF,
                    focalTaxon
            );
        }
    }

    public static boolean isSelfReferential(Taxon relatedTaxon, Taxon focalTaxon) {
        boolean isSelfReference = false;
        if (relatedTaxon != null && focalTaxon != null) {
            isSelfReference =
                    StringUtils.equals(relatedTaxon.getExternalId(), focalTaxon.getExternalId())
                            && StringUtils.equals(relatedTaxon.getName(), focalTaxon.getName())
                            && StringUtils.equals(relatedTaxon.getAuthorship(), focalTaxon.getAuthorship());

        }
        return isSelfReference;
    }

    private static Taxon toTaxon(Map<String, String> relatedName) {
        Taxon relatedTaxon = TaxonUtil.mapToTaxon(relatedName);
        relatedTaxon.setRank(guessRankFromName(relatedTaxon.getName()));
        relatedTaxon.setName(RegExUtils.replacePattern(relatedTaxon.getName(), "_[a-z]+", ""));
        return relatedTaxon;
    }

    public static String guessRankFromName(String name) {
        String trimmedName = trimScientificName(name);
        String rankName = "";
        String[] s = StringUtils.split(trimmedName, " ");
        if (s != null) {
            boolean isVariety = isVariety(trimmedName);
            String[] nameAndVar = StringUtils.splitByWholeSeparator(trimmedName, " var ");
            int wordCount = StringUtils.split(nameAndVar[0], " ").length;
            if (wordCount == 2) {
                if (isVariety) {
                    rankName = "variety";
                } else {
                    rankName = "species";
                }
            } else if (wordCount == 3) {
                if (isVariety) {
                    rankName = "subvariety";
                } else {
                    rankName = "subspecies";
                }
            }
        }
        return rankName;
    }

    private static boolean isVariety(String trimmedName) {
        return StringUtils.contains(trimmedName, " var ");
    }

    public static String urlForName(Taxon relatedTaxon) {
        String name = relatedTaxon.getName();
        return urlForName(name);
    }

    public static String urlForName(String name) {
        return URL_ENDPOINT_DISCOVER_LIFE_SEARCH
                + StringUtils.replace(name, " ", "+");
    }

    private static void enrichFromAuthorString(String authorshipString, Map<String, String> relatedName) {
        String scrubbedName = StringUtils.replace(
                authorshipString, ";", "");

        String[] authorParts = StringUtils.split(scrubbedName, ",");

        if (authorParts != null && authorParts.length > 1) {
            String authorName = StringUtils.trim(authorParts[0]);
            String authorYear = StringUtils.trim(authorParts[1]);
            relatedName.put("authorship", StringUtils.join(Arrays.asList(authorName, authorYear), ", "));
        }
    }

    private static String enrichFromNameString(Map<String, String> relatedName, String altNameText, String authorshipText) {
        String altName = trimNameNodeTextContent(altNameText);

        String authorship = null;
        if (StringUtils.isNotBlank(authorshipText)) {
            authorship = StringUtils.trim(authorshipText);
            if (StringUtils.startsWith(authorship, ",")) {
                authorship = StringUtils.trim(authorship.substring(1));
            }
            if (StringUtils.endsWith(altName, "var")) {
                String[] split = StringUtils.split(authorship);
                if (split.length > 2) {
                    altName = altName + " " + split[0];
                    authorship = StringUtils.trim(authorship.substring(split[0].length()));
                }
            }
        }

        relatedName.put("name", altName);

        return authorship;
    }

    private static String trimNameNodeTextContent(String textContent) {
        return StringUtils.replace(
                StringUtils.trim(textContent),
                "_sic",
                "");
    }

    public static void parse(InputStream is, TermMatchListener termMatchListener) throws IOException {
        NodeList o;
        try {
            o = (NodeList) XmlUtil.applyXPath(is, "//tr/td/b/a | //tr/td/i/a", XPathConstants.NODESET);

            Node currentFamilyNode = null;
            for (int i = 0; i < o.getLength(); i++) {
                Node node = o.item(i);

                boolean isSpeciesNode = StringUtils.equals("i", node.getParentNode().getNodeName());

                if (!isSpeciesNode) {
                    currentFamilyNode = node;
                }

                if (isSpeciesNode && currentFamilyNode != null) {
                    parseNames(
                            currentFamilyNode,
                            node,
                            new SubgenusStrippingListener(termMatchListener));
                }

            }

        } catch (SAXException | ParserConfigurationException | XPathExpressionException e) {
            throw new IOException(e);
        }
    }

    private static Taxon getTaxonForNode(Node familyNode, Taxon t) {
        TaxonImpl targetTaxon = new TaxonImpl();
        String familyId = StringUtils.trim(familyNode.getAttributes().getNamedItem("href").getTextContent());
        String familyName = StringUtils.trim(familyNode.getTextContent());
        TaxonUtil.copy(t, targetTaxon);

        Matcher matcher = PATTERN_GENUS.matcher(t.getName());

        final String genusName = matcher.matches() ? matcher.group("genus") : "";
        List<String> path = new ArrayList<String>(PATH_STATIC) {{
            add(familyName);
            if (StringUtils.isNoneBlank(genusName)) {
                add(genusName);
            }
            add(t.getName());
        }};

        targetTaxon.setPath(StringUtils.join(path, CharsetConstant.SEPARATOR));

        List<String> pathIds = new ArrayList<String>(PATH_STATIC_IDS) {{
            add(URL_ENDPOINT_DISCOVER_LIFE + familyId);
            if (StringUtils.isNoneBlank(genusName)) {
                add(URL_ENDPOINT_DISCOVER_LIFE + "/mp/20q?search=" + genusName);
            }
            add(URL_ENDPOINT_DISCOVER_LIFE + t.getId());
        }};

        targetTaxon.setPathIds(StringUtils.join(pathIds, CharsetConstant.SEPARATOR));

        ArrayList<String> pathList = new ArrayList<String>(PATH_NAMES_STATIC) {{
            if (StringUtils.isNoneBlank(genusName)) {
                add("genus");
            }
            add(targetTaxon.getRank());
        }};

        targetTaxon.setPathNames(StringUtils.join(pathList, CharsetConstant.SEPARATOR));
        return targetTaxon;
    }

    public static String trimScientificName(String actual) {
        return RegExUtils.replacePattern(actual, " \\(.*\\) ", " ");
    }

    public static void parseTaxonPage(InputStream is, TermMatchListener termMatchListener) throws IOException {
        Document document = Jsoup.parse(is, "UTF-8", "https://example.org/");
        document.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .charset(StandardCharsets.UTF_8)
                .escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml);

        Element primary = document.selectXpath("//div/table/tbody/tr/td/b").first();

        Element nameElem = primary == null ? null : primary.select("i").first();
        Taxon accepted = new TaxonImpl();
        if (nameElem != null) {
            setName(accepted, nameElem.text());
        }
        Element authorshipElem = primary == null ? null : primary.select("font").first();

        if (authorshipElem != null) {
            accepted.setAuthorship(authorshipElem.text());
            accepted.setPathNames(accepted.getRank());
            accepted.setPath(accepted.getName());

            Element subgenus = document.selectXpath("//td/p/small/a/i").first();
            if (subgenus != null) {
                accepted.setPathNames("subgenus" + CharsetConstant.SEPARATOR + accepted.getPathNames());
                String subgenusName = subgenus.text();
                accepted.setPath(subgenusName + CharsetConstant.SEPARATOR + accepted.getPath());
            }
            Matcher matcher = PATTERN_GENUS.matcher(accepted.getName());
            if (matcher.matches()) {
                accepted.setPathNames("genus" + CharsetConstant.SEPARATOR + accepted.getPathNames());
                String genusName = matcher.group("genus");
                accepted.setPath(genusName + CharsetConstant.SEPARATOR + accepted.getPath());
            }
            accepted.setPath("Animalia | Arthropoda | Insecta | Hymenoptera | " + accepted.getPath());
            accepted.setPathNames("kingdom | phylum | class | order | " + accepted.getPathNames());
            termMatchListener.foundTaxonForTerm(null, accepted, NameType.HAS_ACCEPTED_NAME, accepted);
        }


        if (authorshipElem != null) {
            Element synonyms = document.selectXpath("//div/table/tbody/tr/td/small").first();

            if (synonyms != null) {
                List<TextNode> textNodes = synonyms.textNodes();
                for (TextNode textNode : textNodes) {
                    org.jsoup.nodes.Node parent = textNode.parent();
                    if (parent != null) {
                        if (StringUtils.equals("small", parent.nodeName())) {
                            Taxon currentTaxon = new TaxonImpl();
                            currentTaxon.setAuthorship(textNode.text());
                            org.jsoup.nodes.Node node = textNode.previousSibling();
                            if (node != null && StringUtils.equals("i", node.nodeName())) {
                                org.jsoup.nodes.Node synonymNameNode = node.firstChild();
                                if (synonymNameNode != null) {
                                    String name = synonymNameNode.toString();
                                    currentTaxon.setName(name);
                                    setName(currentTaxon, name);
                                    termMatchListener.foundTaxonForTerm(null, currentTaxon, NameType.SYNONYM_OF, accepted);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void setName(Taxon accepted, String name) {
        accepted.setName(name);
        accepted.setRank(guessRankFromName(name));
    }
}
