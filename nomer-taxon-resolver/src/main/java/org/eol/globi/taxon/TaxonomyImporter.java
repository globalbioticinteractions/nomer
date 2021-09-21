package org.eol.globi.taxon;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.Taxon;
import org.globalbioticinteractions.nomer.util.CacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class TaxonomyImporter {
    private static final Logger LOG = LoggerFactory.getLogger(TaxonomyImporter.class);

    private static final int BATCH_TRANSACTION_SIZE = 250000;
    private int counter;
    private StopWatch stopwatch;

    private TaxonParser parser;

    private TaxonReaderFactory taxonReaderFactory;

    public TaxonomyImporter(TaxonParser taxonParser, TaxonReaderFactory taxonReaderFactory) {
        this.parser = taxonParser;
        this.taxonReaderFactory = taxonReaderFactory;
        stopwatch = new StopWatch();
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

        File indexDir = CacheUtil.createTmpCacheDir();
        LOG.info("index directory at [" + indexDir + "] created.");


        setCounter(0);
        try (Directory cacheDir = CacheUtil.luceneDirectoryFor(indexDir);
             TaxonLookupBuilder taxonImportListener = new TaxonLookupBuilder(cacheDir)) {
            Map<String, BufferedReader> allReaders = taxonReaderFactory.getAllReaders();
            for (Map.Entry<String, BufferedReader> entry : allReaders.entrySet()) {
                try {
                    parse(entry.getValue(), taxonImportListener);
                } catch (IOException ex) {
                    throw new IOException("failed to read from [" + entry.getKey() + "]");
                }
            }
            return new TaxonLookupServiceImpl(new SimpleFSDirectory(indexDir.toPath()));
        } catch (IOException e) {
            throw new StudyImporterException("failed to import taxonomy", e);
        }
    }

    private Directory initCacheDir() throws StudyImporterException {
        File indexDir = CacheUtil.createTmpCacheDir();
        try {
            return new SimpleFSDirectory(indexDir.toPath());
        } catch (IOException e) {
            throw new StudyImporterException("failed to create index dir [" + indexDir.getAbsolutePath() + "]");
        }
    }

    private void parse(BufferedReader reader, TaxonLookupBuilder taxonImportListener) throws IOException {
        getParser().parse(reader, new TaxonImportListener() {
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
