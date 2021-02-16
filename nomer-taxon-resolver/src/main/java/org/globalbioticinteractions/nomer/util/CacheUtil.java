package org.globalbioticinteractions.nomer.util;

import org.eol.globi.service.PropertyEnricherException;

import java.io.File;

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
}
