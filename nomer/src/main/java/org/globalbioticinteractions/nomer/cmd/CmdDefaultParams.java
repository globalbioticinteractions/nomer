package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.globalbioticinteractions.nomer.util.TermMatcherContextCaching;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

abstract class CmdDefaultParams extends TermMatcherContextCaching implements Runnable {

    @Parameter(names = {"--cache-dir", "-c"}, description = "cache directory")
    private String cacheDir = "./.nomer";

    @Override
    public String getCacheDir() {
        return cacheDir;
    }

    @Override
    public String getProperty(String key) {
        Properties props = new Properties(System.getProperties());
        try {
            props.load(getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/default.properties"));
        } catch (IOException e) {
            throw new RuntimeException("failed to load defaults", e);
        }
        return props.getProperty(key);
    }

    @Parameter(description = "[matcher1] [matcher2] ...")
    private List<String> matchers = new ArrayList<>();

    List<String> getMatchers() {
        return matchers;
    }
}
