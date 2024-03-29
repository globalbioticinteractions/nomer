package org.eol.globi.taxon;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.client.cache.Resource;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.Taxon;
import org.globalbioticinteractions.nomer.util.CacheUtil;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class TaxonomyImporter {
    private static final Logger LOG = LoggerFactory.getLogger(TaxonomyImporter.class);

    private static final int BATCH_TRANSACTION_SIZE = 250000;
    private final TermMatcherContext ctx;
    private int counter;
    private StopWatch stopwatch;

    private TaxonParser parser;

    private TaxonReaderFactory taxonReaderFactory;

    public TaxonomyImporter(TermMatcherContext ctx, TaxonParser taxonParser, TaxonReaderFactory taxonReaderFactory) {
        this.ctx = ctx;
        this.parser = taxonParser;
        this.taxonReaderFactory = taxonReaderFactory;
        stopwatch = new StopWatch();
    }

    public TaxonomyImporter(TaxonParser taxonParser, TaxonReaderFactory taxonReaderFactory) {
        this(null, taxonParser, taxonReaderFactory);
    }

    public TaxonParser getParser() {
        return parser;
    }

    protected String formatProgressString(double avg) {
        return String.format("%d %.1f terms/s", getCounter(), avg);
    }

    private void count() {
        this.counter++;
    }

    public TaxonLookupService createLookupService() throws StudyImporterException {
        getStopwatch().reset();
        getStopwatch().start();


        setCounter(0);
        File indexDir = ctx == null
                ? initTmpCacheDir()
                : new File(ctx.getCacheDir(), parser.getClass().getSimpleName());

        if (!indexDir.exists()) {

            try (Directory cacheDir = CacheUtil.luceneDirectoryFor(indexDir);
                 TaxonLookupBuilder taxonImportListener = new TaxonLookupBuilder(cacheDir)) {

                LOG.debug("index directory at [" + indexDir + "] initializing...");
                Map<String, Resource> allReaders = taxonReaderFactory.getResources();
                for (Map.Entry<String, Resource> entry : allReaders.entrySet()) {
                    try {
                        parse(entry.getValue().getInputStream(), taxonImportListener);
                    } catch (IOException ex) {
                        throw new IOException("failed to read from [" + entry.getKey() + "]", ex);
                    }
                    LOG.debug("index directory at [" + indexDir + "] initialized.");

                }
            } catch (IOException e) {
                throw new StudyImporterException("failed to import taxonomy", e);
            }
        }
        try {
            return new TaxonLookupServiceImpl(new SimpleFSDirectory(indexDir.toPath()));
        } catch (IOException e) {
            throw new StudyImporterException("failed to initialize taxon index", e);
        }

    }

    private File initTmpCacheDir() throws StudyImporterException {
        File indexDir = CacheUtil.createTmpCacheDir();
        try {
            SimpleFSDirectory simpleFSDirectory = new SimpleFSDirectory(indexDir.toPath());
            return indexDir;
        } catch (IOException e) {
            throw new StudyImporterException("failed to create index dir [" + indexDir.getAbsolutePath() + "]");
        }
    }

    private void parse(InputStream is, TaxonLookupBuilder taxonImportListener) throws IOException {
        getParser().parse(is, new TaxonImportListener() {
            @Override
            public void addTerm(Taxon term) {
                taxonImportListener.addTerm(term);
                count();
                if (getCounter() % BATCH_TRANSACTION_SIZE == 0) {
                    StopWatch stopwatch = getStopwatch();
                    stopwatch.stop();
                    double avg = 1000.0 * BATCH_TRANSACTION_SIZE / (stopwatch.getTime() + 1);
                    String format = formatProgressString(avg);
                    LOG.info(format);
                    stopwatch.reset();
                    stopwatch.start();
                }
            }

            @Override
            public void addTerm(String key, Taxon term) {
                taxonImportListener.addTerm(key, term);
            }

            @Override
            public void start() {
                taxonImportListener.start();
            }

            @Override
            public void finish() {
                taxonImportListener.finish();
            }
        });
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public StopWatch getStopwatch() {
        return stopwatch;
    }

}
