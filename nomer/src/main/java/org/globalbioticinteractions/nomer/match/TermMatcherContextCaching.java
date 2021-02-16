package org.globalbioticinteractions.nomer.match;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.globalbioticinteractions.nomer.cmd.CmdDefaultParams;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public abstract class TermMatcherContextCaching extends CmdDefaultParams implements TermMatcherContext {
    private final static Logger LOG = LoggerFactory.getLogger(TermMatcherContextCaching.class);

    @Override
    public InputStream getResource(String uri) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
            md.update(uri.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            String hex = String.format("%064x", new BigInteger(1, digest));
            FileUtils.forceMkdir(new File(getCacheDir()));
            File cachedFile = new File(getCacheDir(), hex + ".gz");
            String location = "[" + uri + "] at [" + cachedFile.getAbsolutePath() + "]";
            if (!cachedFile.exists()) {
                String msg = "caching " + location;
                LOG.info(msg + "...");
                FileSystemManager fsManager = VFS.getManager();
                FileObject fileObj = fsManager.resolveFile(uri);

                try (OutputStream output = StringUtils.endsWith(uri, ".gz") ?
                        new FileOutputStream(cachedFile) :
                        new GZIPOutputStream(new FileOutputStream(cachedFile))) {
                    IOUtils.copyLarge(fileObj.getContent().getInputStream(), output);
                    output.flush();
                }

                LOG.info(msg + " done.");
            }
            LOG.info("using cached " + location);
            return new GZIPInputStream(new FileInputStream(cachedFile));
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("cannot lookup cache for [" + uri + "]", e);
        }
    }

}
