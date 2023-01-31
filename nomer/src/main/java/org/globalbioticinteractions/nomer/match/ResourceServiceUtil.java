package org.globalbioticinteractions.nomer.match;

import bio.guoda.preston.RefNodeFactory;
import bio.guoda.preston.store.HashKeyUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.rdf.api.IRI;
import org.eol.globi.service.PropertyEnricherException;
import org.globalbioticinteractions.nomer.util.CacheUtil;
import org.globalbioticinteractions.nomer.util.PropertyContext;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

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

public class ResourceServiceUtil {

    public static final String NOMER_PRESTON_VERSION = "nomer.preston.version";
    public static final String NOMER_PRESTON_REMOTES = "nomer.preston.remotes";

    private static File getCachedFileName(File cacheDir, URI resource) throws IOException {
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

    public static File getCachedFileName(TermMatcherContext ctx, URI resource) throws IOException {
        String provenanceVersion = ctx.getProperty(NOMER_PRESTON_VERSION);
        File cacheDir;
        if (StringUtils.isBlank(provenanceVersion)) {
            cacheDir = new File(ctx.getCacheDir());
        }  else {
            cacheDir = new File(ctx.getCacheDir(), StringUtils.replace(provenanceVersion, ":", ""));
        }
        return getCachedFileName(cacheDir, resource);
    }

    public static OutputStream getOutputStreamForCache(File cachedFile) throws IOException {
        return new GZIPOutputStream(new FileOutputStream(cachedFile));
    }

    public static InputStream getInputStreamForCache(File cachedFile) throws IOException {
        return new GZIPInputStream(new FileInputStream(cachedFile));
    }

    public static IRI getProvenanceAnchor(PropertyContext ctx) throws PropertyEnricherException {
        URI hash = CacheUtil.getValueURI(ctx, NOMER_PRESTON_VERSION);
        if (!HashKeyUtil.isValidHashKey(RefNodeFactory.toIRI(hash))) {
            throw new PropertyEnricherException("expected sha256 hash uri, but found [" + hash + "]");
        }
        return RefNodeFactory.toIRI(hash);
    }

    public static boolean hasAnchor(PropertyContext ctx) {
        return org.apache.commons.lang.StringUtils.isNotBlank(ctx.getProperty(NOMER_PRESTON_VERSION));
    }
}
