package org.eol.globi.taxon;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class GlobalNamesService2Test {

    @Test
    public void parseNoMatch() throws IOException, PropertyEnricherException {
        String recordedResponse = IOUtils.toString(getClass().getResourceAsStream("globalnames-response-no-match.json"), StandardCharsets.UTF_8);
        List<Triple<Term, NameType, Taxon>> responses = new ArrayList<>();

        TermMatchListener listener = new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                responses.add(Triple.of(term, nameType, taxon));
            }
        };
        GlobalNamesService2.parseResult(
                listener,
                recordedResponse,
                new RequestedTermServiceNOOP()
        );

        assertThat(responses.size(), Is.is(1));
        assertThat(responses.get(0).getLeft().getId(), Is.is(nullValue()));
        assertThat(responses.get(0).getLeft().getName(), Is.is("Donald duck"));
        assertThat(responses.get(0).getMiddle(), Is.is(NameType.NONE));
        assertThat(responses.get(0).getRight().getId(), Is.is(nullValue()));
        assertThat(responses.get(0).getRight().getName(), Is.is("Donald duck"));

    }

    @Test
    public void parseFuzzy() throws IOException, PropertyEnricherException {
        String recordedResponse = IOUtils.toString(getClass().getResourceAsStream("globalnames-response-fuzzy.json"), StandardCharsets.UTF_8);
        List<Triple<Term, NameType, Taxon>> responses = new ArrayList<>();

        TermMatchListener listener = new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                responses.add(Triple.of(term, nameType, taxon));
            }
        };
        GlobalNamesService2.parseResult(
                listener,
                recordedResponse,
                new RequestedTermServiceNOOP()
        );

        assertThat(responses.size(), Is.is(1));
        assertThat(responses.get(0).getLeft().getId(), Is.is(nullValue()));
        assertThat(responses.get(0).getLeft().getName(), Is.is("Homo saliens"));
        assertThat(responses.get(0).getMiddle(), Is.is(NameType.SIMILAR_TO));
        Taxon resolved = responses.get(0).getRight();
        assertHomoSapiens(resolved);

    }

    @Test
    public void parseTwoExactMatches() throws IOException, PropertyEnricherException {
        String recordedResponse = IOUtils.toString(getClass().getResourceAsStream("verifier-globalnames-response.json"), StandardCharsets.UTF_8);
        List<Triple<Term, NameType, Taxon>> responses = new ArrayList<>();

        TermMatchListener listener = new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                responses.add(Triple.of(term, nameType, taxon));
            }
        };
        GlobalNamesService2.parseResult(
                listener,
                recordedResponse,
                new RequestedTermServiceNOOP()
        );

        assertThat(responses.size(), Is.is(2));
        assertThat(responses.get(0).getLeft().getId(), Is.is(nullValue()));
        assertThat(responses.get(0).getLeft().getName(), Is.is("Homo sapiens"));
        assertThat(responses.get(0).getMiddle(), Is.is(NameType.HAS_ACCEPTED_NAME));
        Taxon resolved = responses.get(0).getRight();
        assertHomoSapiens(resolved);

        Term provided = responses.get(1).getLeft();
        assertThat(provided.getId(), Is.is(nullValue()));
        assertThat(provided.getName(), Is.is("Ariopsis felis"));
        assertThat(responses.get(1).getMiddle(), Is.is(NameType.HAS_ACCEPTED_NAME));
        Taxon resolved1 = responses.get(1).getRight();
        asssertAriopsisFelis(resolved1);

    }

    private void assertHomoSapiens(Taxon resolved) {
        assertThat(resolved.getId(), Is.is("ITIS:180092"));
        assertThat(resolved.getName(), Is.is("Homo sapiens"));
        assertThat(resolved.getPath(), Is.is("Bilateria | Deuterostomia | Chordata | Vertebrata | Gnathostomata | Tetrapoda | Mammalia | Theria | Eutheria | Primates | Haplorrhini | Simiiformes | Hominoidea | Hominidae | Homininae | Homo | Homo sapiens"));
        assertThat(resolved.getPathIds(), Is.is("ITIS:914154 | ITIS:914156 | ITIS:158852 | ITIS:331030 | ITIS:914179 | ITIS:914181 | ITIS:179913 | ITIS:179916 | ITIS:179925 | ITIS:180089 | ITIS:943773 | ITIS:943778 | ITIS:943782 | ITIS:180090 | ITIS:943805 | ITIS:180091 | ITIS:180092"));
        assertThat(resolved.getPathNames(), Is.is("subkingdom | infrakingdom | phylum | subphylum | infraphylum | superclass | class | subclass | infraclass | order | suborder | infraorder | superfamily | family | subfamily | genus | species"));
        assertThat(resolved.getAuthorship(), Is.is(nullValue()));
    }

    @Test
    public void parseExact() throws IOException, PropertyEnricherException {
        String recordedResponse = IOUtils.toString(getClass().getResourceAsStream("globalnames-response-exact.json"), StandardCharsets.UTF_8);
        List<Triple<Term, NameType, Taxon>> responses = new ArrayList<>();

        TermMatchListener listener = new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                responses.add(Triple.of(term, nameType, taxon));
            }
        };
        GlobalNamesService2.parseResult(
                listener,
                recordedResponse,
                new RequestedTermServiceNOOP()
        );

        assertThat(responses.size(), Is.is(1));
        assertThat(responses.get(0).getLeft().getId(), Is.is(nullValue()));
        assertThat(responses.get(0).getLeft().getName(), Is.is("Homo sapiens"));
        assertThat(responses.get(0).getMiddle(), Is.is(NameType.HAS_ACCEPTED_NAME));
        Taxon resolved = responses.get(0).getRight();
        assertHomoSapiens(resolved);

    }

    @Test
    public void parseExactNotCuratedINaturalist() throws IOException, PropertyEnricherException {
        String recordedResponse = IOUtils.toString(getClass().getResourceAsStream("verifier-globalnames-response-inaturalist.json"), StandardCharsets.UTF_8);
        List<Triple<Term, NameType, Taxon>> responses = new ArrayList<>();

        TermMatchListener listener = new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                responses.add(Triple.of(term, nameType, taxon));
            }
        };
        GlobalNamesService2.parseResult(
                listener,
                recordedResponse,
                new RequestedTermServiceNOOP()
        );

        assertThat(responses.size(), Is.is(1));
        assertThat(responses.get(0).getLeft().getId(), Is.is(nullValue()));
        assertThat(responses.get(0).getLeft().getName(), Is.is("Ariopsis felis"));
        assertThat(responses.get(0).getMiddle(), Is.is(NameType.SAME_AS));
        Taxon resolved = responses.get(0).getRight();
        assertThat(resolved.getId(), Is.is("INAT_TAXON:94635"));
        assertThat(resolved.getName(), Is.is("Ariopsis felis"));
        assertThat(resolved.getAuthorship(), Is.is(nullValue()));

    }

    @Test
    public void parseSynonym() throws IOException, PropertyEnricherException {
        String recordedResponse = IOUtils.toString(getClass().getResourceAsStream("globalnames-response-synonym.json"), StandardCharsets.UTF_8);
        List<Triple<Term, NameType, Taxon>> responses = new ArrayList<>();

        TermMatchListener listener = new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                responses.add(Triple.of(term, nameType, taxon));
            }
        };
        GlobalNamesService2.parseResult(
                listener,
                recordedResponse,
                new RequestedTermServiceNOOP()
        );

        assertThat(responses.size(), Is.is(1));
        assertThat(responses.get(0).getLeft().getId(), Is.is(nullValue()));
        assertThat(responses.get(0).getLeft().getName(), Is.is("Arius felis"));
        assertThat(responses.get(0).getMiddle(), Is.is(NameType.SYNONYM_OF));
        Taxon resolved = responses.get(0).getRight();
        asssertAriopsisFelis(resolved);

    }

    private void asssertAriopsisFelis(Taxon resolved) {
        assertThat(resolved.getId(), Is.is("ITIS:680665"));
        assertThat(resolved.getRank(), Is.is("species"));
        assertThat(resolved.getName(), Is.is("Ariopsis felis"));
        assertThat(resolved.getPath(), Is.is("Bilateria | Deuterostomia | Chordata | Vertebrata | Gnathostomata | Actinopterygii | Teleostei | Ostariophysi | Siluriformes | Ariidae | Ariopsis | Ariopsis felis"));
        assertThat(resolved.getPathIds(), Is.is("ITIS:914154 | ITIS:914156 | ITIS:158852 | ITIS:331030 | ITIS:914179 | ITIS:161061 | ITIS:161105 | ITIS:162845 | ITIS:163992 | ITIS:164157 | ITIS:639019 | ITIS:680665"));
        assertThat(resolved.getPathNames(), Is.is("subkingdom | infrakingdom | phylum | subphylum | infraphylum | superclass | class | superorder | order | family | genus | species"));
        assertThat(resolved.getAuthorship(), Is.is(nullValue()));
    }

    private static class RequestedTermServiceNOOP implements GlobalNamesService2.RequestedTermService {
        @Override
        public Term termForRequestId(Long requestId) {
            return null;
        }
    }
}