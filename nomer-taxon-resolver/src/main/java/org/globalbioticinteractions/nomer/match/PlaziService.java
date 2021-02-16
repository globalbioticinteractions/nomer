package org.globalbioticinteractions.nomer.match;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.lucene.store.Directory;
import org.eol.globi.taxon.TaxonLookupBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TaxonCacheListener;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.taxon.TaxonLookupServiceImpl;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.PropertyEnricherInfo;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@PropertyEnricherInfo(name = "plazi", description = "Lookup Plazi taxon treatment by name or id using offline-enabled database dump")
public class PlaziService implements TermMatcher {

    private static final Logger LOG = LoggerFactory.getLogger(PlaziService.class);

    private final TermMatcherContext ctx;

    private TaxonLookupServiceImpl taxonLookupService = null;

    public PlaziService(TermMatcherContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void match(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException {
        for (Term term : terms) {
            Taxon[] taxons = lookupLinkedTerms(term);
            if (taxons == null || taxons.length == 0) {
                termMatchListener.foundTaxonForTerm(
                        null,
                        term,
                        new TaxonImpl(term.getName(), term.getId()),
                        NameType.NONE
                );
            } else {
                for (Taxon taxon : taxons) {
                    Taxon taxonToBeSubmitted = taxon;
                    if (StringUtils.startsWith(taxon.getExternalId(), "doi:")
                            || StringUtils.startsWith(taxon.getExternalId(), "http://treatment.plazi.org/id/")) {
                        taxonToBeSubmitted = new TaxonImpl(taxon.getExternalId(), taxon.getExternalId());
                        taxonToBeSubmitted.setPath(taxon.getExternalId());
                    }
                    termMatchListener.foundTaxonForTerm(null, term, taxonToBeSubmitted, NameType.SAME_AS);
                }
            }
        }
    }

    private Taxon[] lookupLinkedTerms(Term term) throws PropertyEnricherException {
        Taxon[] taxa = null;
        if (StringUtils.isNotBlank(term.getName()) || StringUtils.isNotBlank(term.getId())) {
            if (needsInit()) {
                if (ctx == null) {
                    throw new PropertyEnricherException("context needed to initialize");
                }
                lazyInit();
            }

            try {
                String externalId = term.getId();
                if (StringUtils.isNotBlank(term.getId())) {
                    if (StringUtils.startsWith(term.getId(), "PLAZI:")) {
                        externalId = StringUtils.replace(term.getId(), "PLAZI:", "http://treatment.plazi.org/id/");
                    }
                    taxa = taxonLookupService.lookupTermsById(externalId);
                }
                if ((taxa == null || taxa.length == 0) && StringUtils.isNotBlank(term.getName())) {
                    taxa = taxonLookupService.lookupTermsByName(term.getName());
                }
            } catch (IOException e) {
                throw new PropertyEnricherException("failed to lookup [" + term.getName() + "]", e);
            }
        }
        return taxa;
    }


    private void lazyInit() throws PropertyEnricherException {
        File cacheDir = getCacheDir(this.ctx);
        boolean preExistingCacheDir = cacheDir.exists();
        if (!preExistingCacheDir) {
            if (!cacheDir.mkdirs()) {
                throw new PropertyEnricherException("failed to create cache dir at [" + cacheDir.getAbsolutePath() + "]");
            }
        }

        try {
            SimpleFSDirectory indexDir = new SimpleFSDirectory(cacheDir);
            taxonLookupService = new TaxonLookupServiceImpl(indexDir);
            if (preExistingCacheDir) {
                LOG.info("Plazi taxonomy already indexed at [" + cacheDir.getAbsolutePath() + "], no need to import.");
            } else {
                indexTreatments(indexDir);
            }

        } catch (IOException e) {
            throw new PropertyEnricherException("failed to init enricher", e);
        }

    }

    private void indexTreatments(Directory indexDir) throws PropertyEnricherException {
        LOG.info("Indexing Plazi treatments ...");
        StopWatch watch = new StopWatch();
        watch.start();
        AtomicLong counter = new AtomicLong();

        try (TaxonLookupBuilder taxonLookupBuilder = new TaxonLookupBuilder(indexDir)) {
            InputStream resource = this.ctx.getResource(getArchiveUrl());
            TaxonCacheListener listener = new TaxonCacheListener() {

                @Override
                public void start() {
                    taxonLookupBuilder.start();
                }

                @Override
                public void addTaxon(Taxon taxon) {
                    counter.incrementAndGet();
                    taxonLookupBuilder.addTerm(taxon);
                }

                @Override
                public void finish() {
                    taxonLookupBuilder.finish();
                }
            };
            ArchiveInputStream archiveInputStream = new ZipArchiveInputStream(resource);

            ArchiveEntry nextEntry;
            while ((nextEntry = archiveInputStream.getNextEntry()) != null) {
                if (!nextEntry.isDirectory() && StringUtils.endsWith(nextEntry.getName(), ".ttl")) {
                    CloseShieldInputStream closeShieldInputStream = new CloseShieldInputStream(archiveInputStream);
                    PlaziTreatmentsLoader.importTreatment(closeShieldInputStream, listener);
                }
            }


        } catch (IOException e) {
            throw new PropertyEnricherException("failed to load archive", e);
        }

        watch.stop();
        TaxonCacheService.logCacheLoadStats(watch.getTime(), counter.intValue(), LOG);
        LOG.info("Indexing Plazi treatments complete.");
    }

    private boolean needsInit() {
        return taxonLookupService == null;
    }

    private File getCacheDir(TermMatcherContext ctx) {
        return new File(ctx.getCacheDir(), "plazi");
    }

    private String getArchiveUrl() throws PropertyEnricherException {
        return ctx.getProperty("nomer.plazi.treatments.archive");
    }

}
