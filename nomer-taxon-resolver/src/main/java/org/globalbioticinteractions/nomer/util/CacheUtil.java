package org.globalbioticinteractions.nomer.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TaxonomyImporter;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class CacheUtil {
    public static File getCacheDir(TermMatcherContext ctx, String namespace) {
        return new File(ctx.getCacheDir(), namespace);
    }

    public static boolean mkdir(File cacheDir) throws PropertyEnricherException {
        boolean preExistingCacheDir = cacheDir.exists();
        if (!preExistingCacheDir) {
            if (!cacheDir.mkdirs()) {
                throw new PropertyEnricherException("failed to create cache dir at [" + cacheDir.getAbsolutePath() + "]");
            }
        }
        return preExistingCacheDir;
    }

    public static File createTmpCacheDir() {
        File indexPath = new File(FileUtils.getTempDirectoryPath() + "/taxon" + System.currentTimeMillis());
        indexPath.deleteOnExit();
        return indexPath;
    }

    public static Directory luceneDirectoryFor(File indexDir) throws IOException {
        return new SimpleFSDirectory(indexDir.toPath());
    }

    public static URI getValueURI(TermMatcherContext ctx, String key) throws PropertyEnricherException {
        String value = ctx.getProperty(key);
        return parseURI(key, value);
    }

    public static URI parseURI(String key, String value) throws PropertyEnricherException {
        if (StringUtils.isBlank(value)) {
            throw new PropertyEnricherException("no uri for taxon resource [" + key + "] found");
        }
        try {
            return new URI(value);
        } catch (URISyntaxException e) {
            throw new PropertyEnricherException("invalid uri for taxon resource [" + key + "] found: [" + value + "]");
        }
    }
}
