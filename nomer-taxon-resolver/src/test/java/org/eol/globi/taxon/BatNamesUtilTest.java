package org.eol.globi.taxon;

import org.apache.commons.io.IOUtils;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

public class BatNamesUtilTest {


    @Test
    public void getExplorePageSaved() throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
        String htmlAsXmlString = IOUtils.toString(
                getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/batnames/explore.xml"),
                StandardCharsets.UTF_8
        );


        NodeList linkElem = (NodeList) XmlUtil.applyXPath(IOUtils.toInputStream(htmlAsXmlString, StandardCharsets.UTF_8)
                , "//a", XPathConstants.NODESET);

        assertThat(linkElem.getLength(), Is.is(1808));

    }

    @Test
    public void extractGenera() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        InputStream is = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/batnames/explore.xml");


        Collection<String> genera = BatNamesUtil.extractGenera(is);

        assertThat(genera.size(), Is.is(234));
        assertThat(genera, hasItem("Rhinolophus"));

    }

    @Test
    public void extractTaxonInfoRhinolophus() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        InputStream is = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/batnames/Rhinolophus.xml");


        List<Taxon> taxa = new ArrayList<>();
        TermMatchListener listener = new TermMatchListener() {

            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                taxa.add(resolvedTaxon);
            }
        };
        BatNamesUtil.parseTaxaForGenus(is, listener);

        assertThat(taxa.size(), Is.is(110));

        Taxon firstTaxon = taxa.get(0);
        assertThat(firstTaxon.getExternalId(), Is.is("https://batnames.org/species/Rhinolophus+acuminatus"));
        assertThat(firstTaxon.getExternalUrl(), Is.is("https://batnames.org/species/Rhinolophus+acuminatus"));
        assertThat(firstTaxon.getName(), Is.is("Rhinolophus acuminatus"));
        assertThat(firstTaxon.getAuthorship(), Is.is("Peters, 1871"));
        assertThat(firstTaxon.getCommonNames(), Is.is("Accuminate Horseshoe Bat @en"));
        assertThat(firstTaxon.getNameSource(), Is.is("Simmons, N.B. and A.L. Cirranello. 2022B. Bat Species of the World: A taxonomic and geographic database . Accessed on 07/21/2022."));
        assertThat(firstTaxon.getNameSourceURL(), Is.is("https://batnames.org/species/Rhinolophus+acuminatus"));
        assertThat(firstTaxon.getNameSourceAccessedAt(), Is.is(notNullValue()));

    }


    @Test
    public void extractTaxonInfoCistugo() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        InputStream is = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/batnames/Cistugo.xml");


        List<Taxon> taxa = new ArrayList<>();
        TermMatchListener listener = new TermMatchListener() {

            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                taxa.add(resolvedTaxon);
            }
        };
        BatNamesUtil.parseTaxaForGenus(is, listener);

        assertThat(taxa.size(), Is.is(2));

        Taxon firstTaxon = taxa.get(0);
        assertThat(firstTaxon.getExternalId(), Is.is("https://batnames.org/species/Cistugo+lesueuri"));
        assertThat(firstTaxon.getExternalUrl(), Is.is("https://batnames.org/species/Cistugo+lesueuri"));
        assertThat(firstTaxon.getName(), Is.is("Cistugo lesueuri"));
        assertThat(firstTaxon.getAuthorship(), Is.is("Roberts, 1919"));
        assertThat(firstTaxon.getCommonNames(), Is.is("Lesueur's Wing-gland Bat @en"));
        assertThat(firstTaxon.getNameSource(), startsWith("Simmons, N.B. and A.L. Cirranello. 2023. Bat Species of the World: A taxonomic and geographic database."));
        assertThat(firstTaxon.getNameSourceURL(), Is.is("https://batnames.org/species/Cistugo+lesueuri"));
        assertThat(firstTaxon.getNameSourceAccessedAt(), Is.is(notNullValue()));

    }
    @Test
    public void extractTaxonInfoMiniopterus() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        InputStream is = getClass()
                .getResourceAsStream("/org/globalbioticinteractions/nomer/match/batnames/Miniopterus.xml");


        List<Taxon> taxa = new ArrayList<>();
        TermMatchListener listener = new TermMatchListener() {

            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                taxa.add(resolvedTaxon);
            }
        };
        BatNamesUtil.parseTaxaForGenus(is, listener);

        assertThat(taxa.size(), Is.is(39));

        Taxon firstTaxon = taxa.get(0);
        assertThat(firstTaxon.getExternalId(), Is.is("https://batnames.org/species/Miniopterus+aelleni"));
        assertThat(firstTaxon.getExternalUrl(), Is.is("https://batnames.org/species/Miniopterus+aelleni"));
        assertThat(firstTaxon.getName(), Is.is("Miniopterus aelleni"));
        assertThat(firstTaxon.getAuthorship(), Is.is("Goodman, Maminirina, Weyeneth, Bradman, Christidis, Ruedi, &amp; Appleton, 2009"));
        assertThat(firstTaxon.getCommonNames(), Is.is("Aellen's Long-fingered Bat @en"));
        assertThat(firstTaxon.getNameSource(), startsWith("Simmons, N.B. and A.L. Cirranello. 2023. Bat Species of the World: A taxonomic and geographic database."));
        assertThat(firstTaxon.getNameSourceURL(), Is.is("https://batnames.org/species/Miniopterus+aelleni"));
        assertThat(firstTaxon.getNameSourceAccessedAt(), Is.is(notNullValue()));

    }

}

