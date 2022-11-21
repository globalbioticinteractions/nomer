package org.globalbioticinteractions.nomer.match;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

public class ResourceServiceLocationBased extends ResourceServiceReadOnly {
    private final static Logger LOG = LoggerFactory.getLogger(ResourceServiceLocationBased.class);

    private TermMatcherContext ctx;

    public ResourceServiceLocationBased(TermMatcherContext ctx) {
        super(ctx);
        this.ctx = ctx;
    }

    @Override
    public InputStream retrieve(URI resource) throws IOException {
        cacheResource(resource);
        return super.retrieve(resource);
    }

    private void cacheResource(URI resource) throws IOException {
        File cachedFile = ResourceServiceUtil.getCachedFileName(ctx, resource);

        String location = "[" + resource + "] at [" + cachedFile.getAbsolutePath() + "]";
        String msg = "caching " + location;
        LOG.info(msg + "...");

        ResourceService resourceService = new ResourceServiceLocalAndRemote(is -> is);
        try (OutputStream output = StringUtils.endsWith(resource.toString(), ".gz") ?
                new FileOutputStream(cachedFile) :
                ResourceServiceUtil.getOutputStreamForCache(cachedFile)) {
            IOUtils.copyLarge(resourceService.retrieve(resource), output);
            output.flush();
        }
        LOG.info(msg + " done.");
    }
}
