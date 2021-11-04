package org.globalbioticinteractions.nomer.match;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.eol.globi.service.ResourceService;
import org.globalbioticinteractions.nomer.cmd.CmdDefaultParams;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public abstract class TermMatcherContextCaching extends CmdDefaultParams implements TermMatcherContext {
    private final static Logger LOG = LoggerFactory.getLogger(TermMatcherContextCaching.class);

    @Override
    public InputStream retrieve(URI uri) throws IOException {
        return new ResourceServiceFactoryImpl(this)
                .createResourceService()
                .retrieve(uri);
    }

    public class ResourceServiceFactoryImpl implements ResourceServiceFactory {

        private final TermMatcherContext ctx;

        public ResourceServiceFactoryImpl(TermMatcherContext termMatcherContext) {
            this.ctx = termMatcherContext;
        }

        @Override
        public ResourceService createResourceService() throws IOException {
            return new CachingResourceService();
        }

        private class CachingResourceService implements ResourceService {

            @Override
            public InputStream retrieve(URI resource) throws IOException {
                File cachedFile = getCachedFileName(resource, new File(TermMatcherContextCaching.this.getCacheDir()));
                String location = "[" + resource + "] at [" + cachedFile.getAbsolutePath() + "]";
                if (!cachedFile.exists()) {
                    String msg = "caching " + location;
                    LOG.info(msg + "...");
                    FileSystemManager fsManager = VFS.getManager();
                    FileObject fileObj = fsManager.resolveFile(resource);
                    try (OutputStream output = StringUtils.endsWith(resource.toString(), ".gz") ?
                            new FileOutputStream(cachedFile) :
                            getOutputStreamForCache(cachedFile)) {
                        IOUtils.copyLarge(fileObj.getContent().getInputStream(), output);
                        output.flush();
                    }
                    LOG.info(msg + " done.");
                }
                LOG.info("using cached " + location);
                return getInputStreamForCache(cachedFile);
            }
        }

        private OutputStream getOutputStreamForCache(File cachedFile) throws IOException {
            return new GZIPOutputStream(new FileOutputStream(cachedFile));
        }

    }

    private static GZIPInputStream getInputStreamForCache(File cachedFile) throws IOException {
        return new GZIPInputStream(new FileInputStream(cachedFile));
    }

    private static File getCachedFileName(URI resource, File cacheDir) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(resource.toString().getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            String hex = String.format("%064x", new BigInteger(1, digest));
            FileUtils.forceMkdir(cacheDir);
            return new File(cacheDir, hex + ".gz");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("cannot lookup cache for [" + resource + "]", e);
        }
    }

}
