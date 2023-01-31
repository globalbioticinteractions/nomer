package org.globalbioticinteractions.nomer.match;

import bio.guoda.preston.RefNodeFactory;
import bio.guoda.preston.cmd.CmdGet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.eol.globi.service.PropertyEnricherException;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ResourceServiceContentBased extends ResourceServiceReadOnly {
    private final static Logger LOG = LoggerFactory.getLogger(ResourceServiceContentBased.class);

    private final TermMatcherContext ctx;

    public ResourceServiceContentBased(TermMatcherContext ctx) {
        super(ctx);
        this.ctx = ctx;
    }

    private CmdGet createCmdGet() throws IOException {
        CmdGet cmdGet = new CmdGet();
        try {
            cmdGet.setProvenanceArchor(ResourceServiceUtil.getProvenanceAnchor(ctx));
        } catch (PropertyEnricherException e) {
            throw new IOException("failed to access preston verse", e);
        }
        String[] remotes = StringUtils.split(ctx.getProperty("nomer.preston.remotes"), ",");
        if (remotes != null) {
            List<String> ts = Arrays.asList(remotes);
            List<URI> remotesList = ts.stream()
                    .map(StringUtils::trim)
                    .map(URI::create)
                    .collect(Collectors.toList());
            cmdGet.setRemotes(remotesList);
        }

        String prestonDataDir = ctx.getProperty("nomer.preston.dir");
        File prestonDataDirFile;
        if (StringUtils.isBlank(prestonDataDir)) {
            prestonDataDirFile = new File(ctx.getCacheDir(), "data");
        } else {
            prestonDataDirFile = new File(prestonDataDir);
        }
        String cacheDir = prestonDataDirFile.getAbsolutePath();
        LOG.info("using local Preston data dir: [" + cacheDir + "]");
        cmdGet.setLocalDataDir(cacheDir);
        return cmdGet;
    }

    @Override
    public InputStream retrieve(URI resource) throws IOException {
        CmdGet cmdGet = createCmdGet();
        cmdGet.setContentIdsOrAliases(Collections.singletonList(RefNodeFactory.toIRI(resource)));
        File parentDir = new File(ctx.getCacheDir(), "tmp");
        FileUtils.forceMkdir(parentDir);
        File tmpFile = File.createTempFile("nomer", ".gz", parentDir);
        String location = "[" + resource + "] at [" + tmpFile.getAbsolutePath() + "]";

        try (OutputStream outputStream = ResourceServiceUtil.getOutputStreamForCache(tmpFile)) {
            String msg = "caching " + location;
            LOG.info(msg + "...");
            cmdGet.setOutputStream(outputStream);
            cmdGet.run();
            outputStream.flush();
            LOG.info(msg + " done.");
            File destFile = ResourceServiceUtil.getCachedFileName(ctx, resource);
            FileUtils.moveFile(tmpFile, destFile);
        } catch (IOException ex) {
            throw new IOException("failed to access [" + resource + "] in preston verse [", ex);
        }
        finally {
            FileUtils.deleteQuietly(tmpFile);
        }
        return super.retrieve(resource);
    }

}
