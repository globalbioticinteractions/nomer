package org.globalbioticinteractions.nomer.util;

import org.apache.commons.io.IOUtils;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.GlobalNamesService2;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;

public class ReplacingRowHandlerTest {

    @Test
    public void resolveTaxonCache() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("EOL:327955\tHomo sapiens", StandardCharsets.UTF_8);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final TermMatcher matcher = MatchTestUtil.createTaxonCacheService();
        MatchUtil.apply(is, new ReplacingRowHandler(os, matcher, new MatchTestUtil.TermMatcherContextDefault()));
        String[] lines = os.toString().split("\n");
        assertThat(lines.length, Is.is(1));
        assertThat(lines[0], startsWith("EOL:327955\tHomo sapiens"));
    }

    @Test
    public void noReplaceOnMissingSchemas() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("EOL:1276240\tHomo sapiens", StandardCharsets.UTF_8);
        ByteArrayOutputStream os = replace(is, new MatchTestUtil.TermMatcherContextDefault());
        assertThat(os.toString(), Is.is("EOL:1276240\tHomo sapiens\n"));
    }

    @Test
    public void resolveTaxonCacheMatchFirstLineByNameOnly() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("\tGreen-winged teal", StandardCharsets.UTF_8);
        ByteArrayOutputStream os = replace(is, new MatchTestUtil.TermMatcherContextDefault() {
            @Override
            public Map<Integer, String> getOutputSchema() {
                return new TreeMap<Integer, String>() {{
                    put(1, PropertyAndValueDictionary.NAME);
                }};
            }
        });
        assertThat(os.toString(), Is.is("\tAnas crecca carolinensis\n"));
    }

    @Test
    public void replacePipedValues() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("\tGreen-winged teal | Anas crecca carolinensis", StandardCharsets.UTF_8);
        ByteArrayOutputStream os = replace(is, new MatchTestUtil.TermMatcherContextDefault() {
            @Override
            public Map<Integer, String> getOutputSchema() {
                return new TreeMap<Integer, String>() {{
                    put(1, PropertyAndValueDictionary.EXTERNAL_ID);
                }};
            }
        });
        assertThat(os.toString(), Is.is("\tEOL:1276240 | EOL:1276240\n"));
    }

    @Test
    public void replaceOnlyMatchingPipedValues() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("\tJohnny Bravo | Anas crecca carolinensis", StandardCharsets.UTF_8);
        ByteArrayOutputStream os = replace(is, new MatchTestUtil.TermMatcherContextDefault() {
            @Override
            public Map<Integer, String> getOutputSchema() {
                return new TreeMap<Integer, String>() {{
                    put(1, PropertyAndValueDictionary.EXTERNAL_ID);
                }};
            }
        });
        assertThat(os.toString(), Is.is("\t| EOL:1276240\n"));
    }

    @Test
    public void resolveTaxonCacheMatchFirstLineByIdOnly() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("EOL:1276240\tJohnny Bravo", StandardCharsets.UTF_8);
        TermMatcherContext ctx = new MatchTestUtil.TermMatcherContextDefault() {
            @Override
            public Map<Integer, String> getInputSchema() {
                return new TreeMap<Integer, String>() {{
                    put(0, PropertyAndValueDictionary.EXTERNAL_ID);
                }};
            }

            @Override
            public Map<Integer, String> getOutputSchema() {
                return new TreeMap<Integer, String>() {{
                    put(0, PropertyAndValueDictionary.NAME);
                    put(1, PropertyAndValueDictionary.EXTERNAL_ID);
                }};
            }
        };
        ByteArrayOutputStream os = replace(is, ctx);
        assertThat(os.toString(), Is.is("Anas crecca carolinensis\tEOL:1276240\n"));
    }

    @Test
    public void resolveSomeMatchWithNullExternalId() throws IOException, PropertyEnricherException {
        TaxonImpl taxon = new TaxonImpl("some match", null);
        ByteArrayOutputStream os = applyWithMatch(taxon, NameType.SAME_AS, IOUtils.toInputStream("\tJohnny Bravo", StandardCharsets.UTF_8));
        assertThat(os.toString(), Is.is("\tsome match\n"));
    }

    @Test
    public void resolveNoMatch() throws IOException, PropertyEnricherException {
        TaxonImpl taxon = new TaxonImpl("some match", null);
        ByteArrayOutputStream os = applyWithMatch(taxon, NameType.NONE, IOUtils.toInputStream("\tJohnny Bravo", StandardCharsets.UTF_8));
        assertThat(os.toString(), Is.is("\tJohnny Bravo\n"));
    }

    @Test
    public void resolveNoMatchWithNames() throws IOException, PropertyEnricherException {
        TaxonImpl taxon = new TaxonImpl("some match", null);
        TreeMap<Integer, String> outputSchema = new TreeMap<Integer, String>() {{
            put(0, PropertyAndValueDictionary.EXTERNAL_ID);
            put(1, PropertyAndValueDictionary.NAME);
            put(2, "matchType");
        }};
        ByteArrayOutputStream os = applyWithMatch(taxon, NameType.NONE, IOUtils.toInputStream("\tJohnny Bravo\t", StandardCharsets.UTF_8), outputSchema);
        assertThat(os.toString(), Is.is("\tJohnny Bravo\tNONE\n"));
    }

    @Test
    public void resolveMatchWithNameType() throws IOException, PropertyEnricherException {
        TaxonImpl taxon = new TaxonImpl("matching", "some:123");
        TreeMap<Integer, String> outputSchema = new TreeMap<Integer, String>() {{
            put(0, PropertyAndValueDictionary.EXTERNAL_ID);
            put(1, PropertyAndValueDictionary.NAME);
            put(2, "matchType");
        }};
        ByteArrayOutputStream os = applyWithMatch(taxon, NameType.SAME_AS, IOUtils.toInputStream("\tJohnny Bravo | Mickey Mouse\t", StandardCharsets.UTF_8), outputSchema);
        assertThat(os.toString(), Is.is("some:123 | some:123\tmatching | matching\tSAME_AS | SAME_AS\n"));
    }

    private ByteArrayOutputStream applyWithMatch(TaxonImpl taxon, NameType sameAs, InputStream is) throws IOException, PropertyEnricherException {
        TreeMap<Integer, String> outputSchema = new TreeMap<Integer, String>() {{
            put(0, PropertyAndValueDictionary.EXTERNAL_ID);
            put(1, PropertyAndValueDictionary.NAME);
        }};
        return applyWithMatch(taxon, sameAs, is, outputSchema);
    }

    private ByteArrayOutputStream applyWithMatch(Taxon taxon, NameType sameAs, InputStream is, final Map<Integer, String> outputSchema) throws IOException, PropertyEnricherException {
        TermMatcherContext ctx = new MatchTestUtil.TermMatcherContextDefault() {
            @Override
            public Map<Integer, String> getInputSchema() {
                return new TreeMap<Integer, String>() {{
                    put(0, PropertyAndValueDictionary.EXTERNAL_ID);
                    put(1, PropertyAndValueDictionary.NAME);
                }};
            }

            @Override
            public Map<Integer, String> getOutputSchema() {
                return outputSchema;
            }
        };
        return replace(is, ctx, new TermMatcher() {
            @Override
            public void match(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException {
                for (Term term : terms) {
                    termMatchListener.foundTaxonForTerm(null, term, taxon, sameAs);
                }
            }
        });
    }

    private ByteArrayOutputStream replace(InputStream is, TermMatcherContext ctx) throws IOException, PropertyEnricherException {
        final TermMatcher matcher = new TaxonCacheService("classpath:/org/eol/globi/taxon/taxonCache.tsv", "classpath:/org/eol/globi/taxon/taxonMap.tsv");
        return replace(is, ctx, matcher);
    }

    private ByteArrayOutputStream replace(InputStream is, TermMatcherContext ctx, TermMatcher matcher) throws IOException, PropertyEnricherException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MatchUtil.apply(is, new ReplacingRowHandler(os, matcher, ctx));
        return os;
    }

    @Test
    public void resolveGlobalNamesAppendFuzzyMatch() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("\tHomo saliens\tone", StandardCharsets.UTF_8);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MatchUtil.apply(is, new ReplacingRowHandler(os, new GlobalNamesService2(), new MatchTestUtil.TermMatcherContextDefault() {
        }));
        assertThat(os.toString(), Is.is("\tHomo saliens\tone\n"));
    }

}
