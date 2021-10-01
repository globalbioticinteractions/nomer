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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

public class TaxonParserForDiscoverLifeTest {

    @Test
    public void parseNameRelationsAcamptopoeumVagans() {
        // see https://github.com/globalbioticinteractions/nomer/issues/42
        String xmlSnippet = "<<td>\n" +
                "                 \n" +
                "              <i>\n" +
                "                <a href=\"/mp/20q?search=Acamptopoeum+vagans\" target=\"_self\">\n" +
                "                  Acamptopoeum vagans\n" +
                "                </a>\n" +
                "              </i>\n" +
                "              <font size=\"-1\" face=\"sans-serif\">\n" +
                "                (Cockerell, 1926)\n" +
                "              </font>\n" +
                "               -- \n" +
                "              <i>\n" +
                "                Camptopoeum (Acamptopoeum) vagans \n" +
                "              </i>\n" +
                "              Cockerell, 1926\n" +
                "            </td>\n";
    }

    @Test
    public void parseNameRelationsAndrenaAccepta() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        // see https://github.com/globalbioticinteractions/nomer/issues/42
        String xmlSnippet = "<tr><td>\n" +
                "                 \n" +
                "              <i>\n" +
                "                <a href=\"/mp/20q?search=Andrena+accepta\" target=\"_self\">\n" +
                "                  Andrena accepta\n" +
                "                </a>\n" +
                "              </i>\n" +
                "              <font size=\"-1\" face=\"sans-serif\">\n" +
                "                Viereck, 1916\n" +
                "              </font>\n" +
                "               -- \n" +
                "              <i>\n" +
                "                Andrena pulchella_homonym \n" +
                "              </i>\n" +
                "              Robertson, 1891; \n" +
                "              <i>\n" +
                "                Pterandrena pulchella \n" +
                "              </i>\n" +
                "              (Robertson, 1891); \n" +
                "              <i>\n" +
                "                Andrena accepta \n" +
                "              </i>\n" +
                "              Viereck, 1916, replacement name\n" +
                "            </td></tr>\n";

        NodeList nodes = (NodeList) XmlUtil.applyXPath(
                IOUtils.toInputStream(xmlSnippet, StandardCharsets.UTF_8),
                "//tr/td/b/a | //tr/td/i/a",
                XPathConstants.NODESET
        );

        assertThat(nodes.getLength(), Is.is(1));

        List<Taxon> relatedTaxa = new ArrayList<>();

        TaxonParserForDiscoverLife.parseNames(null, nodes.item(0), new TermMatchListener() {

            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, Taxon resolvedTaxon, NameType nameType) {
                relatedTaxa.add(resolvedTaxon);
            }
        });

        assertThat(relatedTaxa.size(), Is.is(4));

        Taxon firstRelatedName = relatedTaxa.get(0);
        //assertThat(firstRelatedName.get("status"), Is.is("accepted"));
        assertThat(firstRelatedName.getName(), Is.is("Andrena accepta"));
        assertThat(firstRelatedName.getId(), Is.is("https://www.discoverlife.org/mp/20q?search=Andrena+accepta"));
        //assertThat(firstRelatedName.get("authorship"), Is.is("Viereck, 1916"));

        Taxon secondRelatedName = relatedTaxa.get(1);
        //assertThat(secondRelatedName.get("status"), Is.is("homonym"));
        assertThat(secondRelatedName.getName(), Is.is("Andrena pulchella"));
        //assertThat(secondRelatedName.get("authorship"), Is.is("Robertson, 1891"));

        Taxon thirdRelatedName = relatedTaxa.get(2);
        // assertThat(thirdRelatedName.get("status"), Is.is("synonym"));
        assertThat(thirdRelatedName.getName(), Is.is("Pterandrena pulchella"));
        //assertThat(thirdRelatedName.get("authorship"), Is.is("(Robertson, 1891)"));

        Taxon fourthRelatedName = relatedTaxa.get(3);
        //assertThat(fourthRelatedName.getName(), Is.is("synonym"));
        assertThat(fourthRelatedName.getName(), Is.is("Andrena accepta"));
        //assertThat(fourthRelatedName.get("authorship"), Is.is("Viereck, 1916"));


    }


}