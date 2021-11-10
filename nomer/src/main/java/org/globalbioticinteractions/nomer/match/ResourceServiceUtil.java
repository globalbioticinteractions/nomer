package org.globalbioticinteractions.nomer.match;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ResourceServiceUtil {

    public static File getCachedFileName(File cacheDir, URI resource) throws IOException {
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

    public static OutputStream getOutputStreamForCache(File cachedFile) throws IOException {
        return new GZIPOutputStream(new FileOutputStream(cachedFile));
    }

    public static GZIPInputStream getInputStreamForCache(File cachedFile) throws IOException {
        return new GZIPInputStream(new FileInputStream(cachedFile));
    }
}
