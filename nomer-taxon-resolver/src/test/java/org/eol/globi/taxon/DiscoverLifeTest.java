package org.eol.globi.taxon;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.TaxonUtil;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static org.hamcrest.MatcherAssert.assertThat;

public class DiscoverLifeTest {

    private static final String URL_ENDPOINT_DISCOVER_LIFE = "https://www.discoverlife.org";

    private static final String URL_ENDPOINT_DISCOVER_LIFE_SEARCH = URL_ENDPOINT_DISCOVER_LIFE +
            "/mp/20q?search=";

    private static final List<String> PATH_STATIC = Arrays.asList("Animalia", "Arthropoda", "Insecta", "Hymenoptera");

    private static final List<String> PATH_STATIC_IDS = PATH_STATIC
            .stream()
            .map(x -> StringUtils.prependIfMissing(x, URL_ENDPOINT_DISCOVER_LIFE_SEARCH))
            .collect(Collectors.toList());

    private static final List<String> PATH_NAMES_STATIC = Arrays.asList("kingdom", "phylum", "class", "order", "family", "species");

    @Test
    public void getBeeNames() throws IOException {
        final WebClient webClient = new WebClient();

        final DomNode page = getBeePage(webClient);

        assertThat(page.asXml(), Is.is(getStringOfBees()));
    }

    private DomNode getBeePage(WebClient webClient) throws IOException {
        String discoverLifeUrl = URL_ENDPOINT_DISCOVER_LIFE +
                "?act=x_checklist" +
                "&guide=Apoidea_species" +
                "&flags=HAS";
        webClient
                .getOptions()
                .setUseInsecureSSL(true);

        return webClient.getPage(discoverLifeUrl);
    }

    private String getStringOfBees() throws IOException {
        return IOUtils.toString(getStreamOfBees(), StandardCharsets.UTF_8);
    }

    private InputStream getStreamOfBees() throws IOException {
        return new GZIPInputStream(getClass()
                .getResourceAsStream("/org/globalbioticinteractions/nomer/match/discoverlife/bees.xml.gz")
        );
    }

    @Test
    public void parseBees() throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {

        NodeList o = (NodeList) XmlUtil.applyXPath(getStreamOfBees(), "//tr/td/b/a | //tr/td/i/a", XPathConstants.NODESET);

        assertThat(o.getLength(), Is.is(20520));

        Node currentFamilyNode = null;
        ArrayList<Taxon> taxa = new ArrayList<>();
        for (int i = 0; i < o.getLength(); i++) {
            Node row = o.item(i);

            Node speciesNode = (Node) XmlUtil.applyXPath(row, "//td/i/a", XPathConstants.NODE);
            Node familyNode = (Node) XmlUtil.applyXPath(row, "//td/b/a", XPathConstants.NODE);

            if (familyNode != null) {
                currentFamilyNode = familyNode;
            }

            if (currentFamilyNode != null && speciesNode != null) {
                String name = StringUtils.trim(speciesNode.getTextContent());

                String id = speciesNode.getAttributes().getNamedItem("href") == null
                        ? null
                        : StringUtils.trim(speciesNode
                        .getAttributes()
                        .getNamedItem("href")
                        .getTextContent());

                TaxonImpl taxon = new TaxonImpl(
                        name,
                        id);

                taxon.setRank("species");
                TaxonImpl taxonForNode = getTaxonForNode(currentFamilyNode, taxon);
                taxonForNode.setExternalId(StringUtils.prependIfMissing(id, URL_ENDPOINT_DISCOVER_LIFE));
                taxa.add(taxonForNode);
                break;

            }

        }


        assertThat(taxa.size(), Is.is(1));

        Taxon taxon = taxa.get(0);

        assertThat(taxon.getPath(), Is.is("Animalia | Arthropoda | Insecta | Hymenoptera | Andrenidae | Acamptopoeum argentinum"));
        assertThat(taxon.getPathIds(), Is.is("https://www.discoverlife.org/mp/20q?search=Animalia | https://www.discoverlife.org/mp/20q?search=Arthropoda | https://www.discoverlife.org/mp/20q?search=Insecta | https://www.discoverlife.org/mp/20q?search=Hymenoptera | https://www.discoverlife.org/mp/20q?search=Andrenidae | https://www.discoverlife.org/mp/20q?search=Acamptopoeum+argentinum"));
        assertThat(taxon.getPathNames(), Is.is("kingdom | phylum | class | order | family | species"));
        assertThat(taxon.getName(), Is.is("Acamptopoeum argentinum"));
        assertThat(taxon.getId(), Is.is("https://www.discoverlife.org/mp/20q?search=Acamptopoeum+argentinum"));
        assertThat(taxon.getRank(), Is.is("species"));
    }

    private TaxonImpl getTaxonForNode(Node familyNode, Taxon t) {
        TaxonImpl targetTaxon = new TaxonImpl();
        String familyId = StringUtils.trim(familyNode.getAttributes().getNamedItem("href").getTextContent());
        String familyName = StringUtils.trim(familyNode.getTextContent());
        TaxonUtil.copy(t, targetTaxon);

        List<String> path = new ArrayList<String>(PATH_STATIC) {{
            addAll(Arrays.asList(familyName, t.getName()));
        }};

        targetTaxon.setPath(StringUtils.join(path, CharsetConstant.SEPARATOR));

        List<String> pathIds = new ArrayList<String>(PATH_STATIC_IDS) {{
            addAll(Arrays.asList(URL_ENDPOINT_DISCOVER_LIFE + familyId, URL_ENDPOINT_DISCOVER_LIFE + t.getId()));
        }};

        targetTaxon.setPathIds(StringUtils.join(pathIds, CharsetConstant.SEPARATOR));

        targetTaxon.setPathNames(StringUtils.join(PATH_NAMES_STATIC, CharsetConstant.SEPARATOR));
        return targetTaxon;
    }


}
