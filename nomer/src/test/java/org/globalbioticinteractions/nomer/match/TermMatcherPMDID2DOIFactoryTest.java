package org.globalbioticinteractions.nomer.match;

import org.apache.commons.io.IOUtils;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.doi.MalformedDOIException;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class TermMatcherPMDID2DOIFactoryTest {

    @Test
    public void findDOIforPMID() throws PropertyEnricherException {

        TermMatcher termMatcher = new TermMatcherPMDID2DOIFactory().createTermMatcher(createTestMatchContext());

        AtomicBoolean found = new AtomicBoolean(false);
        termMatcher.match(Collections.singletonList(new TermImpl("11056684", "")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term s, Taxon taxon, NameType nameType) {
                assertThat(nameType, is(NameType.SAME_AS));
                assertThat(taxon.getId(), is("10.1186/bcr29"));
                found.set(true);
            }
        });

        assertTrue(found.get());
    }

    @Test
    public void findDOIforInvalidPMID() throws IOException, MalformedDOIException, PropertyEnricherException {

        TermMatcher termMatcher = new TermMatcherPMDID2DOIFactory().createTermMatcher(createTestMatchContext());

        AtomicBoolean found = new AtomicBoolean(false);
        termMatcher.match(Collections.singletonList(new TermImpl("this is not valid", "")), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long aLong, Term s, Taxon taxon, NameType nameType) {
                assertThat(nameType, is(NameType.NONE));
                found.set(true);
            }
        });

        assertTrue(found.get());
    }

    private TermMatcherContext createTestMatchContext() {
        return new TermMatcherContext() {
            @Override
            public String getCacheDir() {
                return null;
            }

            @Override
            public InputStream getResource(String uri) throws IOException {
                return IOUtils.toInputStream(first10Lines, StandardCharsets.UTF_8);
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
            public String getProperty(String key) {
                return "some:url";
            }
        };
    }


    private String first10Lines = "Journal Title,ISSN,eISSN,Year,Volume,Issue,Page,DOI,PMCID,PMID,Manuscript Id,Release Date\n" +
            "Breast Cancer Res,1465-5411,1465-542X,2000,3,1,55,,PMC13900,11250746,,live\n" +
            "Breast Cancer Res,1465-5411,1465-542X,2000,3,1,61,,PMC13901,11250747,,live\n" +
            "Breast Cancer Res,1465-5411,1465-542X,2000,3,1,66,,PMC13902,11250748,,live\n" +
            "Breast Cancer Res,1465-5411,1465-542X,1999,2,1,59,10.1186/bcr29,PMC13911,11056684,,live\n" +
            "Breast Cancer Res,1465-5411,1465-542X,1999,2,1,64,,PMC13912,11400682,,live\n" +
            "Breast Cancer Res,1465-5411,1465-542X,1999,1,1,73,10.1186/bcr16,PMC13913,11056681,,live\n" +
            "Breast Cancer Res,1465-5411,1465-542X,1999,1,1,81,10.1186/bcr17,PMC13914,11056682,,live\n" +
            "Breast Cancer Res,1465-5411,1465-542X,1999,1,1,88,10.1186/bcr18,PMC13915,11056683,,live\n" +
            "Breast Cancer Res,1465-5411,1465-542X,2000,2,2,139,10.1186/bcr45,PMC13916,11056686,,live\n" +
            "Breast Cancer Res,1465-5411,1465-542X,2000,2,3,222,10.1186/bcr57,PMC13917,11056687,,live\n";


}