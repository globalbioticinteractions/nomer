package org.globalbioticinteractions.nomer.match;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.DiscoverLifeUtil;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class DiscoverLifeTaxonService implements TermMatcher {
    private static final Logger LOG = LoggerFactory.getLogger(DiscoverLifeTaxonService.class);


    public static final String MAP_NAME = "discoverlife-names";
    private final TermMatcherContext ctx;

    private BTreeMap<String, List<Pair<NameType, Map<String, String>>>> nameMap = null;

    public DiscoverLifeTaxonService(TermMatcherContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void match(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException {
        try {
            lazyInit();

            for (Term term : terms) {
                List<Pair<NameType, Map<String, String>>> pairs = nameMap.get(term.getName());
                if (pairs == null || pairs.isEmpty()) {
                    noMatch(termMatchListener, term);
                } else {
                    foundMatches(termMatchListener, term, pairs);
                }
            }
        } catch (IOException e) {
            String msg = "failed to match terms [" + terms.stream().map(Term::getName).collect(Collectors.joining("|"));
            throw new PropertyEnricherException(msg, e);
        }
    }

    private void foundMatches(TermMatchListener termMatchListener, Term term, List<Pair<NameType, Map<String, String>>> pairs) {
        for (Pair<NameType, Map<String, String>> pair : pairs) {
            Taxon resolvedTaxon = TaxonUtil.mapToTaxon(pair.getRight());
            termMatchListener.foundTaxonForTerm(
                    null,
                    term,
                    resolvedTaxon,
                    pair.getLeft());

        }
    }

    private void noMatch(TermMatchListener termMatchListener, Term term) {
        termMatchListener.foundTaxonForTerm(
                null,
                term,
                new TaxonImpl(term.getName(), term.getId()),
                NameType.NONE);
    }

    private void lazyInit() throws IOException {
        if (nameMap == null) {
            File discoverLifeCacheDir = new File(ctx.getCacheDir(), "discover-life");
            FileUtils.forceMkdirParent(discoverLifeCacheDir);
            DB db = DBMaker
                    .newFileDB(discoverLifeCacheDir)
                    .mmapFileEnableIfSupported()
                    .compressionEnable()
                    .closeOnJvmShutdown()
                    .transactionDisable()
                    .make();


            if (db.exists(MAP_NAME)) {
                nameMap = db.getTreeMap(MAP_NAME);
            } else {
                initCache(db);
            }
        }
    }

    private void initCache(DB db) throws IOException {
        LOG.info("DiscoverLife name indexing started...");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        AtomicLong nameCounter = new AtomicLong();
        nameMap = db.createTreeMap(MAP_NAME).make();
        DiscoverLifeUtil.parse(DiscoverLifeUtil.getStreamOfBees(), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long requestId, Term providedTerm, Taxon resolvedTaxon, NameType nameType) {
                List<Pair<NameType, Map<String, String>>> matches = nameMap.getOrDefault(providedTerm.getName(), new ArrayList<>());
                List<Pair<NameType, Map<String, String>>> pairs = new ArrayList<>(matches);
                pairs.add(Pair.of(nameType, TaxonUtil.taxonToMap(resolvedTaxon)));
                nameMap.put(providedTerm.getName(), pairs);
                nameCounter.incrementAndGet();
            }
        });

        stopWatch.stop();
        long time = stopWatch.getTime(TimeUnit.SECONDS);
        LOG.info("[" + nameCounter.get() + "] DiscoverLife names were indexed in " + time + "s (@ " + (nameCounter.get() / time) + " names/s)");
    }
}