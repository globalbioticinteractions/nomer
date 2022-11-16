package org.globalbioticinteractions.nomer.match;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import org.eol.globi.taxon.BatNamesUtil;
import org.eol.globi.taxon.DiscoverLifeUtil;
import org.eol.globi.taxon.HtmlUtil;
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
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class BatNamesTaxonService implements TermMatcher {
    private static final Logger LOG = LoggerFactory.getLogger(BatNamesTaxonService.class);


    public static final String MAP_NAME = "batnames";
    public static final Map<String, String> NO_MATCH_TAXON_MAP = TaxonUtil.taxonToMap(new TaxonImpl(PropertyAndValueDictionary.NO_MATCH, PropertyAndValueDictionary.NO_MATCH));
    private final TermMatcherContext ctx;

    private BTreeMap<String, List<Triple<Map<String, String>, NameType, Map<String, String>>>> nameMap = null;

    public BatNamesTaxonService(TermMatcherContext ctx) {
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
        File cacheDir = new File(ctx.getCacheDir(), "batnames");
        FileUtils.forceMkdir(cacheDir);
        return cacheDir;
    }

    private void initCache(DB db) throws IOException {
        LOG.info("BatNames name indexing started...");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        AtomicLong nameCounter = new AtomicLong();
        nameMap = db
                .createTreeMap(MAP_NAME)
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


            TermMatchListener termMatchListener = new TermMatchListener() {
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
            };

            String batNamesExploreUrl = "https://batnames.org/explore.html";
            String htmlAsXmlString = HtmlUtil.getHtmlAsXmlString(batNamesExploreUrl);
            String patchedXml = BatNamesUtil.toPatchedXmlString(htmlAsXmlString);

            try {
                Iterable<String> genera = BatNamesUtil.extractGenera(IOUtils.toInputStream(patchedXml, StandardCharsets.UTF_8));

                for (String genus : genera) {
                    String genusXml = BatNamesUtil.getGenusXml(genus);
                    BatNamesUtil.parseTaxaForGenus(IOUtils.toInputStream(genusXml, StandardCharsets.UTF_8),
                            termMatchListener);
                }

            } catch (SAXException | ParserConfigurationException | XPathExpressionException e) {
                throw new IOException("failed to retrieve resource [" + batNamesExploreUrl + "]");
            }

            for (List<Pair<String, Map<String, String>>> value : homonymsToBeResolved.values()) {
                attemptToResolveHomonyms(value);
            }
        } finally {
            if (tmpDb != null) {
                tmpDb.close();
            }
        }


        stopWatch.stop();
        long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        LOG.info("[" + nameCounter.get() + "] BatNames names were indexed in " + time + "ms (@ " + (nameCounter.get() / (1.0*time + 1.0)) + " names/ms)");
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
                        = nameMap.get(DiscoverLifeUtil.trimScientificName(homonymToBeResolvedTaxon.getName()));
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
}