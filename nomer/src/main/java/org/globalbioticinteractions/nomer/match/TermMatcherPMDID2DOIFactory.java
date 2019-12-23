package org.globalbioticinteractions.nomer.match;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.eol.globi.util.CSVTSVUtil;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TermMatcherPMDID2DOIFactory implements TermMatcherFactory {

    private static final String NOMER_PMID_CACHE = "nomer.pmid2doi.cache.url";
    private static final Log LOG = LogFactory.getLog(TermMatcherPMDID2DOIFactory.class);

    @Override
    public String getName() {
        return "pmid-doi";
    }

    @Override
    public String getDescription() {
        return "resolves pubmed id to doi using https://www.ncbi.nlm.nih.gov/pmc/pmctopmid/";
    }

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        if (null != ctx) {
            String pmidCache = ctx.getProperty(NOMER_PMID_CACHE);
            try {
                InputStream resource = ctx.getResource(pmidCache);

                LabeledCSVParser labeledCSVParser = CSVTSVUtil.createLabeledCSVParser(new InputStreamReader(new BufferedInputStream(resource), StandardCharsets.UTF_8));

                Map<Long, DOI> pmidToDOI = new TreeMap<>();

                while (labeledCSVParser.getLine() != null) {
                    String pmid = labeledCSVParser.getValueByLabel("PMID");
                    String doiString = labeledCSVParser.getValueByLabel("DOI");
                    if (StringUtils.isNotBlank(doiString) && NumberUtils.isDigits(pmid)) {
                        try {
                            pmidToDOI.put(Long.parseLong(pmid), DOI.create(doiString));
                        } catch (MalformedDOIException e) {
                            LOG.warn("skipping invalid doi [" + doiString + "]", e);
                        }
                    }

                }
                return new TermMatcher() {

                    @Override
                    public void match(List<Term> list, TermMatchListener termMatchListener) throws PropertyEnricherException {
                        for (Term term : list) {
                            String id = term.getId();
                            DOI doi = null;
                            if (NumberUtils.isDigits(id)) {
                                doi = pmidToDOI.get(Long.parseLong(id));
                            }
                            Taxon found = doi == null
                                    ? new TaxonImpl(term.getName(), term.getId())
                                    : new TaxonImpl(null, doi.toString());
                            termMatchListener.foundTaxonForTerm(null, term, found, doi == null ? NameType.NONE : NameType.SAME_AS);
                        }
                    }
                };
            } catch (IOException e) {
                throw new RuntimeException("failed to initialize [" + this.getName() + "]", e);
            }

        }
        return null;
    }

}
