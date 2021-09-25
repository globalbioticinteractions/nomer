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
import java.util.zip.GZIPInputStream;

import static org.hamcrest.MatcherAssert.assertThat;

public class DiscoverLifeTest {

    @Test
    public void getBeeNames() throws IOException {
        final WebClient webClient = new WebClient();

        final DomNode page = getBeePage(webClient);

        assertThat(page.asXml(), Is.is(getStringOfBees()));
    }

    private DomNode getBeePage(WebClient webClient) throws IOException {
        String discoverLifeUrl = "https://www.discoverlife.org/" +
                "mp/20q" +
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
        return new GZIPInputStream(getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/discoverlife/bees.xml.gz"));
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
                System.out.println(TaxonUtil.taxonToMap(taxonForNode));
                taxa.add(taxonForNode);
                break;

            }

        }


        assertThat(taxa.size(), Is.is(1));

        Taxon taxon = taxa.get(0);

        assertThat(taxon.getPath(), Is.is("Andrenidae | Acamptopoeum argentinum"));
        assertThat(taxon.getPathIds(), Is.is("/mp/20q?search=Andrenidae | /mp/20q?search=Acamptopoeum+argentinum"));
        assertThat(taxon.getPathNames(), Is.is("family | species"));
        assertThat(taxon.getName(), Is.is("Acamptopoeum argentinum"));
        assertThat(taxon.getId(), Is.is("/mp/20q?search=Acamptopoeum+argentinum"));
        assertThat(taxon.getRank(), Is.is("species"));
    }

    private TaxonImpl getTaxonForNode(Node familyNode, Taxon t) {
        TaxonImpl targetTaxon = new TaxonImpl();
        String familyId = StringUtils.trim(familyNode.getAttributes().getNamedItem("href").getTextContent());
        String familyName = StringUtils.trim(familyNode.getTextContent());
        TaxonUtil.copy(t, targetTaxon);
        targetTaxon.setPath(StringUtils.join(Arrays.asList(familyName, t.getName()), CharsetConstant.SEPARATOR));
        targetTaxon.setPathIds(StringUtils.join(Arrays.asList(familyId, t.getId()), CharsetConstant.SEPARATOR));
        targetTaxon.setPathNames(StringUtils.join(Arrays.asList("family", "species"), CharsetConstant.SEPARATOR));
        return targetTaxon;
    }



}
