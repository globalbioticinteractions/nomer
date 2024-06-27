package org.globalbioticinteractions.nomer.match;

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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

public class WikidataTaxonServiceIT {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void indexAndFindBySpiderName() throws IOException, PropertyEnricherException {
        WikidataTaxonService service = createService(new File("/home/jorrit/.cache/nomer"));

        assertMatch(service, "Xysticus logunovorum");
        assertMatch(service, "Xysticus logunovi");
    }

    private void assertMatch(WikidataTaxonService service, String taxonName) throws PropertyEnricherException {
        List<Triple<Term, NameType, Taxon>> found = new ArrayList<>();
        service.match(Arrays.asList(new TermImpl(null, taxonName)), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term term, NameType nameType, Taxon taxon) {
                found.add(Triple.of(term, nameType, taxon));

            }
        });
        assertThat(found.size(), Is.is(1));

        Term provided = found.get(0).getLeft();
        assertThat(provided.getName(), Is.is(taxonName));
        assertThat(provided.getId(), Is.is(nullValue()));

        assertThat(found.get(0).getMiddle(), is(NameType.HAS_ACCEPTED_NAME));

        Taxon resolved = found.get(0).getRight();
        assertThat(resolved.getName(), is(taxonName));
        assertThat(resolved.getId(), startsWith("WD:"));
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
                return new TreeMap<String, String>().get(key);
            }
        });
    }



}