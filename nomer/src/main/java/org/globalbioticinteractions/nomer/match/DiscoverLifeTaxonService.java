package org.globalbioticinteractions.nomer.match;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.DiscoverLifeUtilXHTML;
import org.eol.globi.taxon.DiscoverLifeUtilXML;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class DiscoverLifeTaxonService implements TermMatcher {
    private static final Logger LOG = LoggerFactory.getLogger(DiscoverLifeTaxonService.class);


    public static final String MAP_NAME = "discoverlife-names";
    public static final Map<String, String> NO_MATCH_TAXON_MAP = TaxonUtil.taxonToMap(new TaxonImpl(PropertyAndValueDictionary.NO_MATCH, PropertyAndValueDictionary.NO_MATCH));
    private final TermMatcherContext ctx;

    private BTreeMap<String, List<Triple<Map<String, String>, NameType, Map<String, String>>>> nameMap = null;

    public DiscoverLifeTaxonService(TermMatcherContext ctx) {
        this.ctx = ctx;
    }


    @Override
    public void match(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException {
        try {
            lazyInit();

            for (Term term : terms) {
                if (TermMatchUtil.shouldMatchAll(term, ctx.getInputSchema())) {
                    matchAll(termMatchListener);
                } else if (StringUtils.isNoneBlank(term.getName())) {
                    List<Triple<Map<String, String>, NameType, Map<String, String>>> pairs = nameMap.get(term.getName());
                    if (pairs == null || pairs.isEmpty()) {
                        noMatch(termMatchListener, term);
                    } else {
                        foundMatches(termMatchListener, term, pairs);
                    }
                } else {
                    noMatch(termMatchListener, term);
                }
            }
        } catch (IOException e) {
            String msg = "failed to match terms [" + terms.stream().map(Term::getName).collect(Collectors.joining("|"));
            throw new PropertyEnricherException(msg, e);
        }
    }

    private void matchAll(TermMatchListener termMatchListener) {
        nameMap.forEach((provided, resolvedPairs) -> {
            for (Triple<Map<String, String>, NameType, Map<String, String>> resolvedPair : resolvedPairs) {
                termMatchListener.foundTaxonForTerm(
                        null,
                        TaxonUtil.mapToTaxon(resolvedPair.getLeft()),
                        resolvedPair.getMiddle(),
                        TaxonUtil.mapToTaxon(resolvedPair.getRight())
                );

            }

        });
    }

    private void foundMatches(TermMatchListener termMatchListener,
                              Term term,
                              List<Triple<Map<String, String>, NameType, Map<String, String>>> pairs) {
        for (Triple<Map<String, String>, NameType, Map<String, String>> pair : pairs) {
            Taxon resolvedTaxon = TaxonUtil.mapToTaxon(pair.getRight());
            termMatchListener.foundTaxonForTerm(
                    null,
                    term,
                    pair.getMiddle(),
                    resolvedTaxon
            );

        }
    }

    private void noMatch(TermMatchListener termMatchListener, Term term) {
        termMatchListener.foundTaxonForTerm(
                null,
                term,
                NameType.NONE,
                new TaxonImpl(term.getName(), term.getId())
        );
    }

    private void lazyInit() throws IOException {
        if (nameMap == null) {
            DB db = DBMaker
                    .newFileDB(new File(getCacheDir(), "names"))
                    .mmapFileEnableIfSupported()
                    .mmapFileCleanerHackDisable()
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

    private File getCacheDir() throws IOException {
        File discoverLifeCacheDir = new File(ctx.getCacheDir(), "discoverlife");
        FileUtils.forceMkdir(discoverLifeCacheDir);
        return discoverLifeCacheDir;
    }

    private void initCache(DB db) throws IOException {
        LOG.info("DiscoverLife name indexing started...");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        AtomicLong nameCounter = new AtomicLong();
        nameMap = db.createTreeMap(MAP_NAME)
                .keySerializer(BTreeKeySerializer.STRING)
                .valueSerializer(Serializer.JAVA)
                .make();

        DB tmpDb = null;
        try {
            tmpDb = DBMaker
                    .newFileDB(new File(getCacheDir(), "tmp"))
                    .mmapFileEnableIfSupported()
                    .mmapFileCleanerHackDisable()
                    .compressionEnable()
                    .deleteFilesAfterClose()
                    .closeOnJvmShutdown()
                    .transactionDisable()
                    .make();

            Map<String, List<Pair<String, Map<String, String>>>> homonymsToBeResolved
                    = tmpDb
                    .createTreeMap("honomyns")
                    .keySerializer(BTreeKeySerializer.STRING)
                    .valueSerializer(Serializer.JAVA)
                    .make();

            TermListener listener = new TermListener(homonymsToBeResolved, nameCounter);

            String endpoint = ctx.getProperty("nomer.discoverlife.url");
            if (StringUtils.endsWith(endpoint, ".xml")) {
                DiscoverLifeUtilXML.parse(ctx.retrieve(URI.create(endpoint)), listener, new DiscoverLifeUtilXML.ParserService());
            } else {
                DiscoverLifeUtilXHTML.parse(DiscoverLifeUtilXHTML.getBeeNameTable(ctx, endpoint), listener);
            }

            for (List<Pair<String, Map<String, String>>> value : homonymsToBeResolved.values()) {
                attemptToResolveHomonyms(value);
            }
        } catch (ParserConfigurationException e) {
            throw new IOException("failed to parse discoverlife bee taxonomy resource", e);
        } finally {
            if (tmpDb != null) {
                tmpDb.close();
            }
        }


        stopWatch.stop();
        long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        LOG.info("[" + nameCounter.get() + "] DiscoverLife names were indexed in " + time + "s (@ " + (nameCounter.get() / (time + 1) / 1000) + " names/s)");
    }

    private void attemptToResolveHomonyms(List<Pair<String, Map<String, String>>> homonymsToBeResolved) {
        for (Pair<String, Map<String, String>> homonymToBeResolved : homonymsToBeResolved) {
            Taxon homonymToBeResolvedTaxon = TaxonUtil.mapToTaxon(homonymToBeResolved.getRight());
            List<Triple<Map<String, String>, NameType, Map<String, String>>> candidatePairs
                    = nameMap.get(homonymToBeResolvedTaxon.getName());

            if (candidatePairs != null) {
                List<Triple<Map<String, String>, NameType, Map<String, String>>> candidates = new ArrayList<>();
                for (Triple<Map<String, String>, NameType, Map<String, String>> nameTypeMapPair : candidatePairs) {
                    if (NameType.HAS_ACCEPTED_NAME.equals(nameTypeMapPair.getMiddle())) {
                        candidates.add(Triple.of(homonymToBeResolved.getRight(), NameType.HOMONYM_OF, nameTypeMapPair.getRight()));
                    }
                }
                candidatePairs.addAll(candidates);
                nameMap.put(homonymToBeResolvedTaxon.getName(), candidatePairs);
            } else {
                List<Triple<Map<String, String>, NameType, Map<String, String>>> candidatePairsTrimmed
                        = nameMap.get(DiscoverLifeUtilXHTML.trimScientificName(homonymToBeResolvedTaxon.getName()));
                List<Triple<Map<String, String>, NameType, Map<String, String>>> candidates = new ArrayList<>();

                if (candidatePairsTrimmed == null) {
                    candidates.add(Triple.of(homonymToBeResolved.getRight(), NameType.HOMONYM_OF, NO_MATCH_TAXON_MAP));
                } else {
                    for (Triple<Map<String, String>, NameType, Map<String, String>> nameTypeMapPair : candidatePairsTrimmed) {
                        if (NameType.HAS_ACCEPTED_NAME.equals(nameTypeMapPair.getMiddle())) {
                            candidates.add(Triple.of(homonymToBeResolved.getRight(), NameType.HOMONYM_OF, nameTypeMapPair.getRight()));
                        }
                    }

                }
                nameMap.put(homonymToBeResolvedTaxon.getName(), candidates);
            }
        }
    }

    public void close() {
        if (nameMap != null) {
            nameMap.getEngine().close();
        }
    }

    private class TermListener implements TermMatchListener {
        private final Map<String, List<Pair<String, Map<String, String>>>> homonymsToBeResolved;
        private final AtomicLong nameCounter;

        public TermListener(Map<String, List<Pair<String, Map<String, String>>>> homonymsToBeResolved, AtomicLong nameCounter) {
            this.homonymsToBeResolved = homonymsToBeResolved;
            this.nameCounter = nameCounter;
        }

        @Override
        public void foundTaxonForTerm(Long requestId, Term providedTerm, NameType nameType, Taxon resolvedTaxon) {
            if (resolvedTaxon == null && NameType.HOMONYM_OF.equals(nameType)) {
                List<Pair<String, Map<String, String>>> list = homonymsToBeResolved.getOrDefault(providedTerm.getName(), new ArrayList<>());
                list.add(Pair.of(providedTerm.getName(), TaxonUtil.taxonToMap((Taxon) providedTerm)));
                homonymsToBeResolved.put(providedTerm.getName(), list);
            } else if (resolvedTaxon != null) {
                List<Triple<Map<String, String>, NameType, Map<String, String>>> matches = nameMap.getOrDefault(providedTerm.getName(), new ArrayList<>());
                List<Triple<Map<String, String>, NameType, Map<String, String>>> pairs = new ArrayList<>(matches);
                pairs.add(Triple.of(TaxonUtil.taxonToMap((Taxon) providedTerm), nameType, TaxonUtil.taxonToMap(resolvedTaxon)));
                nameMap.put(providedTerm.getName(), pairs);
            }
            nameCounter.incrementAndGet();
        }
    }
}