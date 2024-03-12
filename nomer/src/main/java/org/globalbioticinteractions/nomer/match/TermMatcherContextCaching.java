package org.globalbioticinteractions.nomer.match;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.globalbioticinteractions.nomer.cmd.CmdDefaultParams;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public abstract class TermMatcherContextCaching extends CmdDefaultParams implements TermMatcherContext {

    @Override
    public InputStream retrieve(URI uri) throws IOException {
        return new ResourceServiceFactoryImpl(this)
                .createResourceService()
                .retrieve(uri);
    }

    @Override
    public String getCacheDir() {
        return getOrCreateCacheDir();
    }

    private String getOrCreateCacheDir() {
        String property = getProperty("nomer.cache.dir");
        File cacheDir = StringUtils.isBlank(property)
                ? getOrCreateDefaultCacheDir()
                : getOrCreateCacheDir(new File(property));
        return cacheDir.getAbsolutePath();
    }

    public static File getOrCreateDefaultCacheDir() {
        File userHome = new File(System.getProperty("user.home"));
        return getOrCreateCacheDir(new File(userHome, ".cache/nomer"));
    }

    private static File getOrCreateCacheDir(File cacheDir) {
        if (!cacheDir.exists()) {
            try {
                FileUtils.forceMkdir(cacheDir);
            } catch (IOException ex) {
                throw new IllegalArgumentException("invalid or missing cachedir [" + cacheDir.getAbsolutePath() + "]");
            }
        }
        return cacheDir;
    }

}
