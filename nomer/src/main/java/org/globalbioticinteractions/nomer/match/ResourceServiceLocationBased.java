package org.globalbioticinteractions.nomer.match;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
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
        File cachedFile = ResourceServiceUtil.getCachedFileName(
                resource,
                new File(ctx.getCacheDir())
        );

        String location = "[" + resource + "] at [" + cachedFile.getAbsolutePath() + "]";
        String msg = "caching " + location;
        LOG.info(msg + "...");
        FileSystemManager fsManager = VFS.getManager();
        FileObject fileObj = fsManager.resolveFile(resource);
        try (OutputStream output = StringUtils.endsWith(resource.toString(), ".gz") ?
                new FileOutputStream(cachedFile) :
                ResourceServiceUtil.getOutputStreamForCache(cachedFile)) {
            IOUtils.copyLarge(fileObj.getContent().getInputStream(), output);
            output.flush();
        }
        LOG.info(msg + " done.");
    }
}
