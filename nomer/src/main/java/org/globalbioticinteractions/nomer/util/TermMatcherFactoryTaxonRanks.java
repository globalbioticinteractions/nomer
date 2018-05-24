package org.globalbioticinteractions.nomer.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.taxon.TaxonCacheService;
import org.eol.globi.taxon.TermMatcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TermMatcherFactoryTaxonRanks implements TermMatcherFactory {

    @Override
    public TermMatcher createTermMatcher(TermMatcherContext ctx) {

        try {
            File cacheDir = new File(ctx.getCacheDir());
            FileUtils.forceMkdir(cacheDir);
            List<WikidataTaxonRankLoader.TermListener> listeners = new ArrayList<>();

            String taxonRankCacheUrl = ctx.getProperty("nomer.taxon.rank.cache.url");
            if (StringUtils.isBlank(taxonRankCacheUrl)) {
                String resourceNameRanks = "taxon_ranks.tsv";
                URL terms = getClass().getResource(resourceNameRanks);
                Pair<PrintStream, File> termWriter = writerAndFileFor(terms, resourceNameRanks, cacheDir);
                listeners.add(WikidataTaxonRankLoader.createCacheWriter(termWriter.getLeft()));
                taxonRankCacheUrl = termWriter.getRight().toURI().toASCIIString();
            }

            String taxonRankMapUrl = ctx.getProperty("nomer.taxon.rank.map.url");
            if (StringUtils.isBlank(taxonRankMapUrl)) {
                String resourceNameRankLinks = "taxon_rank_links.tsv";
                URL links = getClass().getResource(resourceNameRankLinks);
                Pair<PrintStream, File> linkwriter = writerAndFileFor(links, resourceNameRankLinks, cacheDir);
                listeners.add(WikidataTaxonRankLoader.createMapWriter(linkwriter.getLeft()));
                taxonRankMapUrl = linkwriter.getRight().toURI().toASCIIString();
            }

            if (listeners.size() > 0) {
                WikidataTaxonRankLoader.importTaxonRanks(taxon -> {
                    for (WikidataTaxonRankLoader.TermListener listener : listeners) {
                        listener.onTerm(taxon);
                    }
                });
            }
            TaxonCacheService taxonCacheService = new TaxonCacheService(taxonRankCacheUrl, taxonRankMapUrl);
            taxonCacheService.setCacheDir(new File(cacheDir, "wikidata-taxon-ranks"));
            taxonCacheService.setTemporary(false);
            return taxonCacheService;
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException("failed to create matcher", e);
        }

    }

    private Pair<PrintStream, File> writerAndFileFor(URL terms, String resourceName, File cacheDir) throws IOException {
        File tmpRanks = new File(cacheDir, "wikidata_appended_" + resourceName);
        FileUtils.copyURLToFile(terms, tmpRanks);
        FileOutputStream os = FileUtils.openOutputStream(tmpRanks, true);
        return Pair.of(new PrintStream(os), tmpRanks);

    }

}