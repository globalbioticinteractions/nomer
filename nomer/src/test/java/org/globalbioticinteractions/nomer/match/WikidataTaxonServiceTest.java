package org.globalbioticinteractions.nomer.match;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Triple;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatchListener;
import org.globalbioticinteractions.nomer.cmd.OutputFormat;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

public class WikidataTaxonServiceTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void parseTaxon() throws IOException {
        InputStream is = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/wikidata/lion.json");
        JsonNode jsonNode = new ObjectMapper().readTree(is);

        assertNotNull(jsonNode);

        Taxon taxon = WikidataTaxonService.parseTaxon(jsonNode);

        assertThat(taxon.getId(), Is.is("WD:Q140"));
        assertThat(taxon.getName(), Is.is("Panthera leo"));
        assertThat(taxon.getCommonNames(), containsString("Leeuw @nl"));
        assertThat(taxon.getCommonNames(), containsString("Lion @en"));
        assertThat(taxon.getThumbnailUrl(), Is.is(nullValue()));
    }

    @Test
    public void relatedIdentifiers() throws IOException {
        InputStream is = getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/wikidata/lion.json");
        JsonNode jsonNode = new ObjectMapper().readTree(is);

        assertNotNull(jsonNode);


        List<String> relatedIds = WikidataTaxonService.parseRelatedIds(jsonNode);

        assertThat(relatedIds, hasItem("EOL:328672"));
        assertThat(relatedIds, hasItem("ITIS:183803"));
        assertThat(relatedIds.size(), Is.is(9));

    }

    @Test
    public void indexAndFindById() throws IOException, PropertyEnricherException {
        assertFindById(createService(folder.newFolder()));

    }

    private void assertFindById(WikidataTaxonService service) throws PropertyEnricherException {
        List<Triple<Term, NameType, Taxon>> found = new ArrayList<>();

        service.match(Arrays.asList(new TermImpl("WD:Q140", null)), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                found.add(Triple.of(term, nameType, taxon));
            }
        });

        assertThat(found.size(), Is.is(1));
        Term provided = found.get(0).getLeft();
        assertThat(provided.getName(), Is.is(nullValue()));
        assertThat(provided.getId(), Is.is("WD:Q140"));

        assertThat(found.get(0).getMiddle(), is(NameType.HAS_ACCEPTED_NAME));

        Taxon resolved = found.get(0).getRight();
        assertThat(resolved.getName(), is("Panthera leo"));
        assertThat(resolved.getId(), is("WD:Q140"));
        assertThat(resolved.getCommonNames(), containsString("Leeuw @nl"));
    }

    @Test
    public void findByNonWikidataId() throws IOException, PropertyEnricherException {
        WikidataTaxonService service = createService(folder.newFolder());

        assertNonWikidataTaxonId(service);

    }

    private void assertNonWikidataTaxonId(WikidataTaxonService service) throws PropertyEnricherException {
        List<Triple<Term, NameType, Taxon>> found = new ArrayList<>();

        service.match(Arrays.asList(new TermImpl("ITIS:183803", null)), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                found.add(Triple.of(term, nameType, taxon));
            }
        });

        assertThat(found.size(), Is.is(1));
        Term provided = found.get(0).getLeft();
        assertThat(provided.getName(), Is.is(nullValue()));
        assertThat(provided.getId(), Is.is("ITIS:183803"));

        assertThat(found.get(0).getMiddle(), is(NameType.SYNONYM_OF));

        Taxon resolved = found.get(0).getRight();
        assertThat(resolved.getName(), is("Panthera leo"));
        assertThat(resolved.getId(), is("WD:Q140"));
        assertThat(resolved.getCommonNames(), containsString("Leeuw @nl"));
    }

    @Test
    public void findByNonWikidataIdTwice() throws IOException, PropertyEnricherException {
        File file1 = folder.newFolder();
        assertNonWikidataTaxonId(file1);
        assertNonWikidataTaxonId(file1);

    }

    private void assertNonWikidataTaxonId(File file1) throws IOException, PropertyEnricherException {
        WikidataTaxonService service = createService(file1);
        assertNonWikidataTaxonId(service);
        service.shutdown();
    }

    @Test
    public void indexAndFindByName() throws IOException, PropertyEnricherException {
        WikidataTaxonService service = createService(folder.newFolder());
        List<Triple<Term, NameType, Taxon>> found = new ArrayList<>();

        service.match(Arrays.asList(new TermImpl(null, "Panthera leo")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                found.add(Triple.of(term, nameType, taxon));

            }
        });
        assertThat(found.size(), Is.is(1));

        Term provided = found.get(0).getLeft();
        assertThat(provided.getName(), Is.is("Panthera leo"));
        assertThat(provided.getId(), Is.is(nullValue()));

        assertThat(found.get(0).getMiddle(), is(NameType.HAS_ACCEPTED_NAME));

        Taxon resolved = found.get(0).getRight();
        assertThat(resolved.getName(), is("Panthera leo"));
        assertThat(resolved.getId(), is("WD:Q140"));
        assertThat(resolved.getCommonNames(), containsString("Leeuw @nl"));

    }

    private WikidataTaxonService createService(File file1) throws IOException {
        File file = file1;
        return new WikidataTaxonService(new TermMatcherContext() {
            @Override
            public String getCacheDir() {
                return file.getAbsolutePath();
            }

            @Override
            public InputStream retrieve(URI uri) throws IOException {
                return getClass().getResourceAsStream(uri.toString());
            }

            @Override
            public List<String> getMatchers() {
                return null;
            }

            @Override
            public Map<Integer, String> getInputSchema() {
                return null;
            }

            @Override
            public Map<Integer, String> getOutputSchema() {
                return null;
            }

            @Override
            public OutputFormat getOutputFormat() {
                return null;
            }

            @Override
            public String getProperty(String key) {
                return new TreeMap<String, String>() {
                    {
                        put("nomer.wikidata.url", "/org/globalbioticinteractions/nomer/match/wikidata/items.json");
                    }
                }.get(key);
            }
        });
    }



}