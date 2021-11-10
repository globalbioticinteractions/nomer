package org.globalbioticinteractions.nomer.match;

import org.eol.globi.service.ResourceService;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ResourceServiceReadOnly implements ResourceService {
    private final static Logger LOG = LoggerFactory.getLogger(ResourceServiceReadOnly.class);

    private TermMatcherContext ctx;

    public ResourceServiceReadOnly(TermMatcherContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public InputStream retrieve(URI resource) throws IOException {
        File cachedFile = ResourceServiceUtil.getCachedFileName(
                resource,
                new File(ctx.getCacheDir())
        );

        InputStream is = null;
        if (cachedFile.exists()) {
            is = ResourceServiceUtil.getInputStreamForCache(cachedFile);
            LOG.info("using cached " + ("[" + resource + "] at [" + cachedFile.getAbsolutePath() + "]"));
        }
        return is;
    }
}
