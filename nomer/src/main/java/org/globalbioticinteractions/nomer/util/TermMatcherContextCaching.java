package org.globalbioticinteractions.nomer.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public abstract class TermMatcherContextCaching implements TermMatcherContext {
    private final static Log LOG = LogFactory.getLog(TermMatcherContextCaching.class);

    @Override
    public InputStream getResource(String uri) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance( "SHA-256" );
            md.update( uri.getBytes( StandardCharsets.UTF_8 ) );
            byte[] digest = md.digest();
            String hex = String.format( "%064x", new BigInteger( 1, digest ) );
            File cachedFile = new File(getCacheDir(), hex + ".gz");
            String location = "[" + uri + "] at [" + cachedFile.getAbsolutePath() + "]";
            if (!cachedFile.exists()) {
                String msg = "caching " + location;
                LOG.info(msg + "...");
                FileSystemManager fsManager = VFS.getManager();
                FileObject fileObj = fsManager.resolveFile(uri);
                GZIPOutputStream output = new GZIPOutputStream(new FileOutputStream(cachedFile));
                IOUtils.copyLarge(fileObj.getContent().getInputStream(), output);
                output.flush();
                IOUtils.closeQuietly(output);
                LOG.info(msg + " done.");
            }
            LOG.info("using cached " + location);
            return new GZIPInputStream(new FileInputStream(cachedFile));
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("cannot lookup cache for [" + uri + "]", e);
        }
    }

}
