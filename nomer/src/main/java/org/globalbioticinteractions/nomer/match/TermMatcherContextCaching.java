package org.globalbioticinteractions.nomer.match;

import org.globalbioticinteractions.nomer.cmd.CmdDefaultParams;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public abstract class TermMatcherContextCaching extends CmdDefaultParams implements TermMatcherContext {

    @Override
    public InputStream retrieve(URI uri) throws IOException {
        return new ResourceServiceFactoryImpl(this)
                .createResourceService()
                .retrieve(uri);
    }

}
