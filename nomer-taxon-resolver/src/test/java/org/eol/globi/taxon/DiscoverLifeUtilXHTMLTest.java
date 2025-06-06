package org.eol.globi.taxon;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

public class DiscoverLifeUtilXHTMLTest {

    @Test
    public void parseNameAncylandrenaAtoposoma() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        // see https://github.com/globalbioticinteractions/nomer/issues/149
        String xmlSnippet = "          <tr bgcolor=\"#f0f0f0\">\n" +
                "            <td>\n" +
                "                 \n" +
                "              <i>\n" +
                "                <a href=\"/mp/20q?search=Ancylandrena+atoposoma\" target=\"_self\">\n" +
                "                  Ancylandrena atoposoma\n" +
                "                </a>\n" +
                "              </i>\n" +
                "              <font size=\"-1\" face=\"sans-serif\">\n" +
                "                (Cockerell, 1934)\n" +
                "              </font>\n" +
                "               -- \n" +
                "              <i>\n" +
                "                Andrena (Ancylandrena) heterodoxa_homonym \n" +
                "              </i>\n" +
                "              Cockerell, 1930; \n" +
                "              <i>\n" +
                "                Andrena atoposoma \n" +
                "              </i>\n" +
                "              Cockerell, 1934, replacement name\n" +
                "            </td>\n" +
                "          </tr>\n";

        NodeList nodes = selectNameRelations(xmlSnippet);

        assertThat(nodes.getLength(), Is.is(1));

        List<Triple<Term, NameType, Taxon>> relatedTaxa = new ArrayList<>();

        DiscoverLifeUtilXHTML.parseNames(null, nodes.item(0), new TermMatchListener() {

            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                relatedTaxa.add(Triple.of(providedTerm, nameType, resolvedTaxon));
            }
        });

        assertThat(relatedTaxa.size(), Is.is(3));

        Triple<Term, NameType, Taxon> firstNameRelation = relatedTaxa.get(0);
        assertThat(firstNameRelation.getLeft().getName(), Is.is("Ancylandrena atoposoma"));
        assertThat(firstNameRelation.getLeft().getId(), Is.is("https://www.discoverlife.org/mp/20q?guide=Apoidea_species&search=Ancylandrena+atoposoma"));
        assertThat(((Taxon) firstNameRelation.getLeft()).getAuthorship(), Is.is("(Cockerell, 1934)"));
        assertThat(((Taxon) firstNameRelation.getLeft()).getRank(), Is.is("species"));
        assertThat(firstNameRelation.getMiddle(), Is.is(NameType.HAS_ACCEPTED_NAME));
        assertThat(firstNameRelation.getRight().getName(), Is.is("Ancylandrena atoposoma"));
        assertThat(firstNameRelation.getRight().getId(), Is.is("https://www.discoverlife.org/mp/20q?guide=Apoidea_species&search=Ancylandrena+atoposoma"));
        assertThat(firstNameRelation.getRight().getAuthorship(), Is.is("(Cockerell, 1934)"));
        assertThat(firstNameRelation.getRight().getRank(), Is.is("species"));

    }

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
        String xmlSnippet = "<table>\n" +

                "<tr><td>\n" +
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
                "            </td></tr>\n" +
                "<tr bgcolor=\"#f0f0f0\">\n" +
                "            <td>\n" +
                "                 \n" +
                "              <i>\n" +
                "                <a href=\"/mp/20q?search=Ceylalictus+variegatus\" target=\"_self\">\n" +
                "                  Ceylalictus variegatus\n" +
                "                </a>\n" +
                "              </i>\n" +
                "              <font size=\"-1\" face=\"sans-serif\">\n" +
                "                (Olivier, 1789)\n" +
                "              </font>\n" +
                "               -- \n" +
                "              <i>\n" +
                "                Andrena variegata \n" +
                "              </i>\n" +
                "              Olivier, 1789; \n" +
                "              <i>\n" +
                "                Andrena pulchella \n" +
                "              </i>\n" +
                "              Jurine, 1807; \n" +
                "              <i>\n" +
                "                Allodape syrphoides \n" +
                "              </i>\n" +
                "              Walker, 1871; \n" +
                "              <i>\n" +
                "                Andrena flavo-picta \n" +
                "              </i>\n" +
                "              Dours, 1873; \n" +
                "              <i>\n" +
                "                Andrena flavopicta \n" +
                "              </i>\n" +
                "              Dours, 1873; \n" +
                "              <i>\n" +
                "                Nomioides jucunda \n" +
                "              </i>\n" +
                "              Morawitz, 1874; \n" +
                "              <i>\n" +
                "                Nomioides fasciatus var intermedius \n" +
                "              </i>\n" +
                "              Alfken, 1924; \n" +
                "              <i>\n" +
                "                Nomioides variegata var simplex \n" +
                "              </i>\n" +
                "              Blüthgen, 1925; \n" +
                "              <i>\n" +
                "                Nomioides variegata var unifasciata \n" +
                "              </i>\n" +
                "              Blüthgen, 1925; \n" +
                "              <i>\n" +
                "                Nomioides labiatarum \n" +
                "              </i>\n" +
                "              Cockerell, 1931; \n" +
                "              <i>\n" +
                "                Nomioides variegata var nigrita \n" +
                "              </i>\n" +
                "              Blüthgen, 1934; \n" +
                "              <i>\n" +
                "                Nomioides variegata var pseudocerea \n" +
                "              </i>\n" +
                "              Blüthgen, 1934; \n" +
                "              <i>\n" +
                "                Nomioides variegata var nigriventris \n" +
                "              </i>\n" +
                "              Blüthgen, 1934\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "</table>";

        NodeList nodes = selectNameRelations(xmlSnippet);

        assertThat(nodes.getLength(), Is.is(2));

        List<Triple<Term, NameType, Taxon>> relatedTaxa = new ArrayList<>();

        DiscoverLifeUtilXHTML.parseNames(null, nodes.item(0), new TermMatchListener() {

            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                relatedTaxa.add(Triple.of(providedTerm, nameType, resolvedTaxon));
            }
        });

        assertThat(relatedTaxa.size(), Is.is(3));

        Triple<Term, NameType, Taxon> firstNameRelation = relatedTaxa.get(0);
        assertThat(firstNameRelation.getLeft().getName(), Is.is("Andrena accepta"));
        assertThat(firstNameRelation.getLeft().getId(), Is.is("https://www.discoverlife.org/mp/20q?guide=Apoidea_species&search=Andrena+accepta"));
        assertThat(((Taxon) firstNameRelation.getLeft()).getAuthorship(), Is.is("Viereck, 1916"));
        assertThat(((Taxon) firstNameRelation.getLeft()).getRank(), Is.is("species"));
        assertThat(firstNameRelation.getMiddle(), Is.is(NameType.HAS_ACCEPTED_NAME));

        Triple<Term, NameType, Taxon> secondNameRelation = relatedTaxa.get(1);
        assertThat(secondNameRelation.getLeft().getName(), Is.is("Andrena pulchella"));
        assertThat(secondNameRelation.getLeft().getId(), Is.is("https://www.discoverlife.org/mp/20q?guide=Apoidea_species&search=Andrena+pulchella"));
        assertThat(((Taxon) secondNameRelation.getLeft()).getAuthorship(), Is.is("Robertson, 1891"));
        assertThat(secondNameRelation.getMiddle(), Is.is(NameType.HOMONYM_OF));
        assertNull(secondNameRelation.getRight());

        Triple<Term, NameType, Taxon> thirdNameRelation = relatedTaxa.get(2);
        assertThat(thirdNameRelation.getLeft().getName(), Is.is("Pterandrena pulchella"));
        assertThat(((Taxon) thirdNameRelation.getLeft()).getAuthorship(), Is.is("(Robertson, 1891)"));
        assertThat(thirdNameRelation.getMiddle(), Is.is(NameType.SYNONYM_OF));
        assertThat(thirdNameRelation.getRight().getName(), Is.is("Andrena accepta"));

    }

    @Test
    public void parseHomonymAllodapeClypeata() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        // see https://github.com/globalbioticinteractions/nomer/issues/53
        String xmlSnippet = "<tr bgcolor=\"#f0f0f0\">\n" +
                "            <td>\n" +
                "                 \n" +
                "              <i>\n" +
                "                <a href=\"/mp/20q?search=Allodape+obscuripennis\" target=\"_self\">\n" +
                "                  Allodape obscuripennis\n" +
                "                </a>\n" +
                "              </i>\n" +
                "              <font size=\"-1\" face=\"sans-serif\">\n" +
                "                Strand, 1915\n" +
                "              </font>\n" +
                "               -- \n" +
                "              <i>\n" +
                "                Allodape clypeata \n" +
                "              </i>\n" +
                "              Strand, 1915; \n" +
                "              <i>\n" +
                "                Allodape clypeata_homonym \n" +
                "              </i>\n" +
                "              Friese, 1924\n" +
                "            </td>\n" +
                "          </tr>\n";

        NodeList nodes = selectNameRelations(xmlSnippet);

        assertThat(nodes.getLength(), Is.is(1));

        List<Triple<Term, NameType, Taxon>> relatedTaxa = new ArrayList<>();

        DiscoverLifeUtilXHTML.parseNames(null, nodes.item(0), new TermMatchListener() {

            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                relatedTaxa.add(Triple.of(providedTerm, nameType, resolvedTaxon));
            }
        });

        assertThat(relatedTaxa.size(), Is.is(3));

        Triple<Term, NameType, Taxon> firstNameRelation = relatedTaxa.get(0);
        assertThat(firstNameRelation.getLeft().getName(), Is.is("Allodape obscuripennis"));
        assertThat(firstNameRelation.getMiddle(), Is.is(NameType.HAS_ACCEPTED_NAME));
        assertThat(((Taxon) firstNameRelation.getLeft()).getAuthorship(), Is.is("Strand, 1915"));

        Triple<Term, NameType, Taxon> secondNameRelation = relatedTaxa.get(1);
        assertThat(secondNameRelation.getLeft().getName(), Is.is("Allodape clypeata"));
        assertThat(((Taxon) secondNameRelation.getLeft()).getAuthorship(), Is.is("Strand, 1915"));
        assertThat(secondNameRelation.getMiddle(), Is.is(NameType.SYNONYM_OF));
        assertThat(secondNameRelation.getRight().getName(), Is.is("Allodape obscuripennis"));
        assertThat(secondNameRelation.getRight().getAuthorship(), Is.is("Strand, 1915"));

        Triple<Term, NameType, Taxon> thirdNameRelation = relatedTaxa.get(2);
        assertThat(thirdNameRelation.getLeft().getName(), Is.is("Allodape clypeata"));
        assertThat(((Taxon) thirdNameRelation.getLeft()).getAuthorship(), Is.is("Friese, 1924"));
        assertThat(thirdNameRelation.getMiddle(), Is.is(NameType.HOMONYM_OF));
        assertNull(thirdNameRelation.getRight());


    }

    @Test
    public void parseNameRelationsDanglingVar() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        // see https://github.com/globalbioticinteractions/nomer/issues/52
        String xmlSnippet = "<tr bgcolor=\"#f0f0f0\">\n" +
                "            <td>\n" +
                "                 \n" +
                "              <i>\n" +
                "                <a href=\"/mp/20q?search=Xylocopa+inconspicua\" target=\"_self\">\n" +
                "                  Xylocopa inconspicua\n" +
                "                </a>\n" +
                "              </i>\n" +
                "              <font size=\"-1\" face=\"sans-serif\">\n" +
                "                Maa, 1937\n" +
                "              </font>\n" +
                "               -- \n" +
                "              <i>\n" +
                "                Xylocopa (Xylocopa) rufipes var \n" +
                "              </i>\n" +
                "              inconspicua Maa, 1937; \n" +
                "              <i>\n" +
                "                Xylocopa (Mimoxylocopa) inconspicua \n" +
                "              </i>\n" +
                "              Maa, 1937; \n" +
                "              <i>\n" +
                "                Xylocopa (Bomboixylocopa) inconspicua \n" +
                "              </i>\n" +
                "              Maa, 1937\n" +
                "            </td>\n" +
                "          </tr>";

        NodeList nodes = selectNameRelations(xmlSnippet);

        assertThat(nodes.getLength(), Is.is(1));

        List<Triple<Term, NameType, Taxon>> relatedTaxa = new ArrayList<>();

        DiscoverLifeUtilXHTML.parseNames(null, nodes.item(0), new TermMatchListener() {

            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                relatedTaxa.add(Triple.of(providedTerm, nameType, resolvedTaxon));
            }
        });

        assertThat(relatedTaxa.size(), Is.is(4));

        Triple<Term, NameType, Taxon> firstNameRelation = relatedTaxa.get(0);
        //assertThat(firstRelatedName.get("status"), Is.is("accepted"));
        assertThat(firstNameRelation.getLeft().getName(), Is.is("Xylocopa inconspicua"));
        assertThat(firstNameRelation.getLeft().getId(), Is.is("https://www.discoverlife.org/mp/20q?guide=Apoidea_species&search=Xylocopa+inconspicua"));
        assertThat(firstNameRelation.getMiddle(), Is.is(NameType.HAS_ACCEPTED_NAME));
        assertThat(((Taxon) firstNameRelation.getLeft()).getAuthorship(), Is.is("Maa, 1937"));

        Triple<Term, NameType, Taxon> secondNameRelation = relatedTaxa.get(1);
        assertThat(secondNameRelation.getLeft().getName(), Is.is("Xylocopa (Xylocopa) rufipes var inconspicua"));
        assertThat(secondNameRelation.getLeft().getId(), Is.is("https://www.discoverlife.org/mp/20q?guide=Apoidea_species&search=Xylocopa+(Xylocopa)+rufipes+var+inconspicua"));
        assertThat(((Taxon) secondNameRelation.getLeft()).getAuthorship(), Is.is("Maa, 1937"));
        assertThat(secondNameRelation.getMiddle(), Is.is(NameType.SYNONYM_OF));
        assertThat(secondNameRelation.getRight().getName(), Is.is("Xylocopa inconspicua"));

    }

    @Test
    public void parseNameRelationsDanglingVarWithLeadingComma() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        // see https://github.com/globalbioticinteractions/nomer/issues/52
        String xmlSnippet = "<tr bgcolor=\"#f0f0f0\">\n" +
                "            <td>\n" +
                "                 \n" +
                "              <i>\n" +
                "                <a href=\"/mp/20q?search=Ceratina+moricei\" target=\"_self\">\n" +
                "                  Ceratina moricei\n" +
                "                </a>\n" +
                "              </i>\n" +
                "              <font size=\"-1\" face=\"sans-serif\">\n" +
                "                Friese, 1899\n" +
                "              </font>\n" +
                "               -- \n" +
                "              <i>\n" +
                "                Ceratina laevifrons var\n" +
                "              </i>\n" +
                "              , moricei Friese, 1899\n" +
                "            </td>\n" +
                "          </tr>\n";

        NodeList nodes = selectNameRelations(xmlSnippet);

        assertThat(nodes.getLength(), Is.is(1));

        List<Triple<Term, NameType, Taxon>> relatedTaxa = new ArrayList<>();

        DiscoverLifeUtilXHTML.parseNames(null, nodes.item(0), new TermMatchListener() {

            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                relatedTaxa.add(Triple.of(providedTerm, nameType, resolvedTaxon));
            }
        });

        assertThat(relatedTaxa.size(), Is.is(2));

        Triple<Term, NameType, Taxon> firstNameRelation = relatedTaxa.get(0);
        //assertThat(firstRelatedName.get("status"), Is.is("accepted"));
        assertThat(firstNameRelation.getLeft().getName(), Is.is("Ceratina moricei"));
        assertThat(firstNameRelation.getMiddle(), Is.is(NameType.HAS_ACCEPTED_NAME));
        assertThat(((Taxon) firstNameRelation.getLeft()).getAuthorship(), Is.is("Friese, 1899"));

        Triple<Term, NameType, Taxon> secondNameRelation = relatedTaxa.get(1);
        assertThat(secondNameRelation.getLeft().getName(), Is.is("Ceratina laevifrons var moricei"));
        assertThat(((Taxon) secondNameRelation.getLeft()).getAuthorship(), Is.is("Friese, 1899"));
        assertThat(secondNameRelation.getMiddle(), Is.is(NameType.SYNONYM_OF));
        assertThat(secondNameRelation.getRight().getName(), Is.is("Ceratina moricei"));

    }

    @Test
    public void parseNameAcceptedHomonym() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        // see https://github.com/globalbioticinteractions/nomer/issues/63
        String xmlSnippet = "<tr bgcolor=\"#f0f0f0\">\n" +
                "            <td>\n" +
                "                 \n" +
                "              <i>\n" +
                "                <a href=\"/mp/20q?search=Anthidiellum+boreale_homonym\" target=\"_self\">\n" +
                "                  Anthidiellum boreale_homonym\n" +
                "                </a>\n" +
                "              </i>\n" +
                "              <font size=\"-1\" face=\"sans-serif\">\n" +
                "                Wu, 2004\n" +
                "              </font>\n" +
                "               -- \n" +
                "              <i>\n" +
                "                Anthidiellum (Anthidiellum) borealis_homonym \n" +
                "              </i>\n" +
                "              Wu, 2004\n" +
                "            </td>\n" +
                "          </tr>\n";

        NodeList nodes = selectNameRelations(xmlSnippet);

        assertThat(nodes.getLength(), Is.is(1));

        List<Triple<Term, NameType, Taxon>> relatedTaxa = new ArrayList<>();

        DiscoverLifeUtilXHTML.parseNames(null, nodes.item(0), new TermMatchListener() {

            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                relatedTaxa.add(Triple.of(providedTerm, nameType, resolvedTaxon));
            }
        });

        assertThat(relatedTaxa.size(), Is.is(3));

        Triple<Term, NameType, Taxon> firstNameRelation = relatedTaxa.get(0);
        assertThat(((Taxon) firstNameRelation.getLeft()).getAuthorship(), Is.is("Wu, 2004"));
        assertThat(firstNameRelation.getLeft().getName(), Is.is("Anthidiellum boreale"));
        assertThat(firstNameRelation.getMiddle(), Is.is(NameType.HOMONYM_OF));
        assertNull(firstNameRelation.getRight());

        Triple<Term, NameType, Taxon> secondNameRelation = relatedTaxa.get(1);
        assertThat(secondNameRelation.getLeft().getName(), Is.is("Anthidiellum (Anthidiellum) borealis"));
        assertThat(secondNameRelation.getMiddle(), Is.is(NameType.HOMONYM_OF));
        assertNull(secondNameRelation.getRight());

        Triple<Term, NameType, Taxon> thirdNameRelation = relatedTaxa.get(2);
        assertThat(thirdNameRelation.getLeft().getName(), Is.is("Anthidiellum boreale"));
        assertThat(((Taxon) thirdNameRelation.getLeft()).getAuthorship(), Is.is("Wu, 2004"));
        assertThat(thirdNameRelation.getMiddle(), Is.is(NameType.HOMONYM_OF));
        assertNull(thirdNameRelation.getRight());
    }

    private NodeList selectNameRelations(String xmlSnippet) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
        return (NodeList) XmlUtil.applyXPath(
                IOUtils.toInputStream(xmlSnippet, StandardCharsets.UTF_8),
                "//tr/td/b/a | //tr/td/i/a",
                XPathConstants.NODESET
        );
    }

    @Test
    public void parseNameRelationsWithHomonym() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        // see https://github.com/globalbioticinteractions/nomer/issues/52
        String xmlSnippet = "<tr bgcolor=\"#f0f0f0\">\n" +
                "            <td>\n" +
                "                 \n" +
                "              <i>\n" +
                "                <a href=\"/mp/20q?search=Pseudopanurgus+aestivalis\" target=\"_self\">\n" +
                "                  Pseudopanurgus aestivalis\n" +
                "                </a>\n" +
                "              </i>\n" +
                "              <font size=\"-1\" face=\"sans-serif\">\n" +
                "                (Provancher, 1882)\n" +
                "              </font>\n" +
                "               -- \n" +
                "              <i>\n" +
                "                Panurgus aestivalis \n" +
                "              </i>\n" +
                "              Provancher, 1882; \n" +
                "              <i>\n" +
                "                Panurginus nebrascensis \n" +
                "              </i>\n" +
                "              Crawford, 1903; \n" +
                "              <i>\n" +
                "                Pseudopanurgus nebrascensis timberlakei_homonym \n" +
                "              </i>\n" +
                "              Michener, 1947; \n" +
                "              <i>\n" +
                "                Pseudopanurgus (Heterosarus) nebrascensis muesebecki \n" +
                "              </i>\n" +
                "              Michener, 1951, replacement name; \n" +
                "              <i>\n" +
                "                Pterosarus nebrascensis \n" +
                "              </i>\n" +
                "              (Crawford, 1903); \n" +
                "              <i>\n" +
                "                Pterosarus nebrascensis muesbecki \n" +
                "              </i>\n" +
                "              (Michener, 1951); \n" +
                "              <i>\n" +
                "                Pterosarus aestivalis \n" +
                "              </i>\n" +
                "              (Provancher, 1882); \n" +
                "              <i>\n" +
                "                Heterosarus (Pterosarus) nebrascensis \n" +
                "              </i>\n" +
                "              (Crawford, 1903); \n" +
                "              <i>\n" +
                "                Heterosarus (Pterosarus) nebrascensis muesbecki \n" +
                "              </i>\n" +
                "              (Michener, 1951); \n" +
                "              <i>\n" +
                "                Protandrena (Pterosarus) nebrascensis \n" +
                "              </i>\n" +
                "              (Crawford, 1903); \n" +
                "              <i>\n" +
                "                Pseudopanurgus nebrascensis nebrascensis \n" +
                "              </i>\n" +
                "              (Crawford, 1903); \n" +
                "              <i>\n" +
                "                Protandrena (Pterosarus) nebrascensis muesbecki \n" +
                "              </i>\n" +
                "              (Michener, 1951); \n" +
                "              <i>\n" +
                "                Pseudopanurgus aestivalis muesbecki \n" +
                "              </i>\n" +
                "              (Michener, 1951), valid subspecies\n" +
                "            </td>\n" +
                "          </tr>\n";

        NodeList nodes = selectNameRelations(xmlSnippet);

        assertThat(nodes.getLength(), Is.is(1));

        List<Triple<Term, NameType, Taxon>> relatedTaxa = new ArrayList<>();

        DiscoverLifeUtilXHTML.parseNames(null, nodes.item(0), new TermMatchListener() {

            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                if (StringUtils.equals("Pseudopanurgus nebrascensis timberlakei", providedTerm.getName())) {
                    relatedTaxa.add(Triple.of(providedTerm, nameType, resolvedTaxon));
                }
            }
        });

        assertThat(relatedTaxa.size(), Is.is(1));

        Triple<Term, NameType, Taxon> firstNameRelation = relatedTaxa.get(0);
        assertThat(firstNameRelation.getMiddle(), Is.is(NameType.HOMONYM_OF));
        Taxon providedTaxon = (Taxon) firstNameRelation.getLeft();
        assertThat(providedTaxon.getName(), Is.is("Pseudopanurgus nebrascensis timberlakei"));
        assertThat(providedTaxon.getAuthorship(), Is.is("Michener, 1947"));
        assertThat(providedTaxon.getRank(), Is.is("subspecies"));

    }

    @Test
    public void parseNameRelationsWithSicSuffix() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        // see https://github.com/globalbioticinteractions/nomer/issues/51
        String xmlSnippet = " <tr bgcolor=\"#f0f0f0\">\n" +
                "            <td>\n" +
                "                 \n" +
                "              <i>\n" +
                "                <a href=\"/mp/20q?search=Andrena+apicata\" target=\"_self\">\n" +
                "                  Andrena apicata\n" +
                "                </a>\n" +
                "              </i>\n" +
                "              <font size=\"-1\" face=\"sans-serif\">\n" +
                "                Smith, 1847\n" +
                "              </font>\n" +
                "               -- \n" +
                "              <i>\n" +
                "                Andrena apicatus_sic \n" +
                "              </i>\n" +
                "              Smith, 1847; \n" +
                "              <i>\n" +
                "                Andrena apicata var tristis \n" +
                "              </i>\n" +
                "              Alfken, 1905; \n" +
                "              <i>\n" +
                "                Andrena apicata var kamtschatica \n" +
                "              </i>\n" +
                "              Alfken, 1929\n" +
                "            </td>\n" +
                "          </tr>\n";

        NodeList nodes = selectNameRelations(xmlSnippet);

        assertThat(nodes.getLength(), Is.is(1));

        List<Triple<Term, NameType, Taxon>> relatedTaxa = new ArrayList<>();

        DiscoverLifeUtilXHTML.parseNames(null, nodes.item(0), new TermMatchListener() {

            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                relatedTaxa.add(Triple.of(providedTerm, nameType, resolvedTaxon));
            }
        });

        assertThat(relatedTaxa.size(), Is.is(4));

        Triple<Term, NameType, Taxon> firstNameRelation = relatedTaxa.get(0);
        assertThat(firstNameRelation.getLeft().getName(), Is.is("Andrena apicata"));
        assertThat(firstNameRelation.getMiddle(), Is.is(NameType.HAS_ACCEPTED_NAME));
        assertThat(((Taxon) firstNameRelation.getLeft()).getAuthorship(), Is.is("Smith, 1847"));

        Triple<Term, NameType, Taxon> secondNameRelation = relatedTaxa.get(1);
        assertThat(secondNameRelation.getLeft().getName(), Is.is("Andrena apicatus"));
        assertThat(((Taxon) secondNameRelation.getLeft()).getAuthorship(), Is.is("Smith, 1847"));
        assertThat(secondNameRelation.getMiddle(), Is.is(NameType.SYNONYM_OF));
        assertThat(secondNameRelation.getRight().getName(), Is.is("Andrena apicata"));

    }

    @Test
    public void parseNameRelationsWithSicSuffix2() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        // see https://github.com/globalbioticinteractions/nomer/issues/51
        String xmlSnippet = "<tr bgcolor=\"#f0f0f0\">\n" +
                "            <td>\n" +
                "                 \n" +
                "              <i>\n" +
                "                <a href=\"/mp/20q?search=Coelioxys+pasteeli_sic\" target=\"_self\">\n" +
                "                  Coelioxys pasteeli_sic\n" +
                "                </a>\n" +
                "              </i>\n" +
                "              <font size=\"-1\" face=\"sans-serif\">\n" +
                "                Gupta, 1992\n" +
                "              </font>\n" +
                "               -- \n" +
                "              <i>\n" +
                "                Coelioxys (Coelioxys) pasteeli_sic \n" +
                "              </i>\n" +
                "              Gupta, 1992\n" +
                "            </td>\n" +
                "          </tr>\n";

        NodeList nodes = selectNameRelations(xmlSnippet);

        assertThat(nodes.getLength(), Is.is(1));

        List<Triple<Term, NameType, Taxon>> relatedTaxa = new ArrayList<>();

        DiscoverLifeUtilXHTML.parseNames(null, nodes.item(0), new TermMatchListener() {

            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                relatedTaxa.add(Triple.of(providedTerm, nameType, resolvedTaxon));
            }
        });

        assertThat(relatedTaxa.size(), Is.is(2));

        Triple<Term, NameType, Taxon> firstNameRelation = relatedTaxa.get(0);
        Taxon firstTaxonLeft = (Taxon) firstNameRelation.getLeft();
        assertThat(firstTaxonLeft.getName(), Is.is("Coelioxys pasteeli"));
        assertThat(firstTaxonLeft.getRank(), Is.is("species"));
        assertThat(firstTaxonLeft.getAuthorship(), Is.is("Gupta, 1992"));
        assertThat(firstNameRelation.getMiddle(), Is.is(NameType.HAS_ACCEPTED_NAME));

        Triple<Term, NameType, Taxon> secondNameRelation = relatedTaxa.get(1);
        assertThat(secondNameRelation.getLeft().getName(), Is.is("Coelioxys (Coelioxys) pasteeli"));
        assertThat(((Taxon) secondNameRelation.getLeft()).getAuthorship(), Is.is("Gupta, 1992"));
        assertThat(secondNameRelation.getMiddle(), Is.is(NameType.SYNONYM_OF));
        assertThat(secondNameRelation.getRight().getName(), Is.is("Coelioxys pasteeli"));

    }

    @Test
    public void parseNamePseudopanurgus_parvus() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        // see https://github.com/globalbioticinteractions/nomer/issues/149
        String xmlSnippet = "<tr bgcolor=\"#f0f0f0\"><td>" +
                "<i><a href=\"/mp/20q?search=Protandrena+parva\" target=\"_self\">Protandrena parva</a></i> <font size=\"-1\" face=\"sans-serif\">(Robertson, 1892)</font> -- <i>Calliopsis parvus </i>Robertson, 1892; <i>Pseudopanurgus gerardiae </i>Crawford, 1932; <i>Pseudopanurgus stevensi </i>Crawford, 1932; <i>Panurginus borealis </i>Cockerell, 1937; <i>Pseudopanurgus (Heterosarus) parvus </i>(Robertson, 1892); <i>Heterosarus (Heterosarus) parvus </i>(Robertson, 1892)</td></tr>";

        NodeList nodes = selectNameRelations(xmlSnippet);

        assertThat(nodes.getLength(), Is.is(1));

        List<Triple<Term, NameType, Taxon>> relatedTaxa = new ArrayList<>();

        DiscoverLifeUtilXHTML.parseNames(null, nodes.item(0), new TermMatchListener() {

            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                relatedTaxa.add(Triple.of(providedTerm, nameType, resolvedTaxon));
            }
        });

        assertThat(relatedTaxa.size(), Is.is(7));

        Triple<Term, NameType, Taxon> firstNameRelation = relatedTaxa.get(0);
        Taxon firstTaxonLeft = (Taxon) firstNameRelation.getLeft();
        assertThat(firstTaxonLeft.getName(), Is.is("Protandrena parva"));
        assertThat(firstTaxonLeft.getRank(), Is.is("species"));
        assertThat(firstTaxonLeft.getAuthorship(), Is.is("(Robertson, 1892)"));
        assertThat(firstNameRelation.getMiddle(), Is.is(NameType.HAS_ACCEPTED_NAME));

        Triple<Term, NameType, Taxon> secondNameRelation = relatedTaxa.get(5);
        assertThat(secondNameRelation.getLeft().getName(), Is.is("Pseudopanurgus (Heterosarus) parvus"));
        assertThat(((Taxon) secondNameRelation.getLeft()).getAuthorship(), Is.is("(Robertson, 1892)"));
        assertThat(secondNameRelation.getMiddle(), Is.is(NameType.SYNONYM_OF));
        assertThat(secondNameRelation.getRight().getName(), Is.is("Protandrena parva"));

    }

    @Test
    public void guessRank() throws IOException {
        assertThat(DiscoverLifeUtilXHTML.guessRankFromName("Bla bla"), Is.is("species"));
    }

    @Test
    public void guessRankFamily() throws IOException {
        assertThat(DiscoverLifeUtilXHTML.guessRankFromName("Bla (Bla) bla"), Is.is("species"));
    }

    @Test
    public void guessRankSubspecies() throws IOException {
        assertThat(DiscoverLifeUtilXHTML.guessRankFromName("Bla bla bla"), Is.is("subspecies"));
    }

    @Test
    public void guessRankVariant() throws IOException {
        assertThat(DiscoverLifeUtilXHTML.guessRankFromName("Bla bla var bla"), Is.is("variety"));
    }

    @Test
    public void guessRankSubvariant() throws IOException {
        assertThat(DiscoverLifeUtilXHTML.guessRankFromName("Bla bla bla var bla"), Is.is("subvariety"));
    }

    @Test
    public void parseSingleBeePage() throws IOException {
        InputStream is = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/discoverlife/agapostemon_texanus.html");

        AtomicReference<Taxon> acceptedFound = new AtomicReference<>(null);
        List<Taxon> knownSynonyms = new ArrayList<>();

        TermMatchListener termMatchListener = new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
                if (NameType.HAS_ACCEPTED_NAME.equals(nameType)) {
                    acceptedFound.set(resolvedTaxon);
                } else if (NameType.SYNONYM_OF.equals(nameType)) {
                    knownSynonyms.add((Taxon) providedTerm);
                }
            }
        };

        DiscoverLifeUtilXHTML.parseTaxonPage(is, termMatchListener);


        assertThat(acceptedFound.get().getName(), Is.is("Agapostemon texanus"));
        assertThat(acceptedFound.get().getAuthorship(), Is.is("Cresson, 1872"));
        assertThat(acceptedFound.get().getRank(), Is.is("species"));
        assertThat(acceptedFound.get().getPath(), Is.is("Animalia | Arthropoda | Insecta | Hymenoptera | Agapostemon | Agapostemon | Agapostemon texanus"));
        assertThat(acceptedFound.get().getPathNames(), Is.is("kingdom | phylum | class | order | genus | subgenus | species"));

        assertThat(knownSynonyms.size(), Is.is(14));

        assertThat(knownSynonyms.get(0).getAuthorship(), Is.is("Cockerell, 1898; "));
        assertThat(knownSynonyms.get(0).getName(), Is.is("Agapostemon texanus subtilior "));
        assertThat(knownSynonyms.get(0).getRank(), Is.is("subspecies"));

        assertThat(knownSynonyms.get(knownSynonyms.size() - 1).getAuthorship(), Is.is("Cockerell, 1939"));
        assertThat(knownSynonyms.get(knownSynonyms.size() - 1).getName(), Is.is("Agapostemon californicus clementinus "));
        assertThat(knownSynonyms.get(knownSynonyms.size() - 1).getRank(), Is.is("subspecies"));


    }


}