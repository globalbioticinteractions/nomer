package org.globalbioticinteractions.nomer.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class TermMatcherFactoryEnricherTest {

    @Test
    public void nodc() throws PropertyEnricherException {
        TermMatcherContext ctx = new TermMatcherContextCaching() {

            @Override
            public String getCacheDir() {
                return new File("target").getAbsolutePath();
            }

            @Override
            public String getProperty(String key) {
                if (StringUtils.equals("nodc.url", key)) {
                    return "zip:" + getClass().getResource("/org/eol/globi/taxon/nodc_archive.zip").toString()
                            + "!/0050418/1.1/data/0-data/NODC_TaxonomicCode_V8_CD-ROM/TAXBRIEF.DAT";
                }
                return null;
            }

        };
        System.out.println(ctx.getCacheDir());
        TermMatcher termMatcher = new TermMatcherFactoryEnricher().createTermMatcher(ctx);
        termMatcher.findTerms(Arrays.asList(new TermImpl("NODC:1", "Mickey")), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long id, String name, Taxon taxon, NameType nameType) {

            }
        });
    }
}