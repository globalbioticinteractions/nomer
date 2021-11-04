package org.globalbioticinteractions.nomer.match;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.globalbioticinteractions.nomer.util.CacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger LOG = LoggerFactory.getLogger(TermMatcherPMDID2DOIFactory.class);

    @Override
    public String getPreferredName() {
        return "pmid-doi";
    }

    @Override
    public String getDescription() {
        return "resolves pubmed id to doi using https://www.ncbi.nlm.nih.gov/pmc/pmctopmid/";
    }

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {
        if (null != ctx) {
            return new TermMatcher() {
                private Map<Long, DOI> pmidToDOI = null;

                private Map<Long, DOI> init(TermMatcherContext ctx) throws IOException {
                    InputStream resource;
                    try {
                        resource = ctx.retrieve(CacheUtil.getValueURI(ctx, NOMER_PMID_CACHE));
                    } catch (PropertyEnricherException e) {
                        throw new IOException("failed to access properties", e);
                    }

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
                    return pmidToDOI;
                }

                @Override
                public void match(List<Term> list, TermMatchListener termMatchListener) throws PropertyEnricherException {
                    initIfNeeded();
                    for (Term term : list) {
                        String id = term.getId();
                        DOI doi = null;
                        if (NumberUtils.isDigits(id)) {
                            doi = pmidToDOI.get(Long.parseLong(id));
                        }
                        Taxon found = doi == null
                                ? new TaxonImpl(term.getName(), term.getId())
                                : new TaxonImpl(null, doi.toString());
                        termMatchListener.foundTaxonForTerm(
                                null,
                                term,
                                doi == null ? NameType.NONE : NameType.SAME_AS,
                                found);
                    }
                }

                private void initIfNeeded() throws PropertyEnricherException {
                    if (pmidToDOI == null) {
                        try {
                            pmidToDOI = init(ctx);
                        } catch (IOException e) {
                            throw new PropertyEnricherException("failed to create pmid-doi lookup", e);
                        }
                    }
                }
            };


        }
        return null;
    }


}
