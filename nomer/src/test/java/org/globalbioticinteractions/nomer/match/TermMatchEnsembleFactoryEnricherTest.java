package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.cmd.OutputFormat;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class TermMatchEnsembleFactoryEnricherTest {

    public static final String NODC_RESOURCE = "/org/eol/globi/taxon/nodc/0050418/1.1/data/0-data/NODC_TaxonomicCode_V8_CD-ROM/TAXBRIEF.DAT";
    @Rule
    public TemporaryFolder cacheDir = new TemporaryFolder();

    @Test
    public void nodc() throws PropertyEnricherException, IOException {
        final URL nodcTestArchive = getClass().getResource(NODC_RESOURCE);
        assertNotNull("failed to find [" + NODC_RESOURCE + "]", nodcTestArchive);

        File dir = cacheDir.newFolder();

        TermMatcherContext ctx = new TermMatcherContextCaching() {

            @Override
            public String getCacheDir() {
                return dir.getAbsolutePath();
            }

            @Override
            public String getProperty(String key) {
                return new TreeMap<String, String>() {
                    {
                        put("nomer.itis.taxonomic_units", "/org/globalbioticinteractions/nomer/match/itis/taxonomic_units.psv");
                        put("nomer.itis.synonym_links", "/org/globalbioticinteractions/nomer/match/itis/synonym_links.psv");
                        put("nomer.itis.taxon_unit_types", "/org/globalbioticinteractions/nomer/match/itis/taxon_unit_types.psv");
                        put("nomer.itis.taxon_authors_lkp", "/org/globalbioticinteractions/nomer/match/itis/taxon_authors_lkp.psv");
                        put("nomer.nodc.url", nodcTestArchive.toExternalForm());
                    }
                }.get(key);
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


        };
        TermMatcher termMatcher = new TermMatcherFactoryEnsembleEnricher().createTermMatcher(ctx);
        termMatcher.match(Arrays.asList(new TermImpl("NODC:9227040101", "Mickey")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long id, Term name, NameType nameType, Taxon taxon) {
                assertThat(name.getName(), Is.is("Mickey"));
                assertThat(name.getId(), Is.is("NODC:9227040101"));
                // note that the NODC taxonomy maps to ITIS:180725
                // and itis maps ITIS:180725 to ITIS:552761
                assertThat(taxon.getExternalId(), Is.is("ITIS:552761"));
                assertThat(taxon.getName(), Is.is("Pecari tajacu"));
            }
        });
    }


    @Test
    public void nodcManualPatch() throws PropertyEnricherException, IOException {
        final URL nodcTestArchive = getClass().getResource(NODC_RESOURCE);
        assertNotNull("failed to find [" + NODC_RESOURCE + "]", nodcTestArchive);

        File dir = cacheDir.newFolder();

        TermMatcherContext ctx = new TermMatcherContextCaching() {

            @Override
            public String getCacheDir() {
                return dir.getAbsolutePath();
            }

            @Override
            public String getProperty(String key) {
                return new TreeMap<String, String>() {
                    {
                        put("nomer.itis.taxonomic_units", "/org/globalbioticinteractions/nomer/match/itis/taxonomic_units.psv");
                        put("nomer.itis.synonym_links", "/org/globalbioticinteractions/nomer/match/itis/synonym_links.psv");
                        put("nomer.itis.taxon_unit_types", "/org/globalbioticinteractions/nomer/match/itis/taxon_unit_types.psv");
                        put("nomer.itis.taxon_authors_lkp", "/org/globalbioticinteractions/nomer/match/itis/taxon_authors_lkp.psv");
                        put("nomer.nodc.url", nodcTestArchive.toExternalForm());
                    }
                }.get(key);
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


        };
        TermMatcher termMatcher = new TermMatcherFactoryEnsembleEnricher().createTermMatcher(ctx);
        termMatcher.match(Arrays.asList(new TermImpl("NODC:88430103", "Mickey")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long id, Term name, NameType nameType, Taxon taxon) {
                assertThat(name.getName(), Is.is("Mickey"));
                assertThat(name.getId(), Is.is("NODC:88430103"));
                // note that the NODC taxonomy maps to ITIS:180725
                // and itis maps ITIS:180725 to ITIS:552761
                assertThat(taxon.getExternalId(), Is.is("ITIS:170949"));
                assertThat(taxon.getName(), Is.is("Bathymaster signatus"));
            }
        });
    }
}
