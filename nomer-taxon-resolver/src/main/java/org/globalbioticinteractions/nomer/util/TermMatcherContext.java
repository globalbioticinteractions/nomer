package org.globalbioticinteractions.nomer.util;

import org.eol.globi.service.ResourceService;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

public interface TermMatcherContext extends PropertyContext, ResourceService {

    String getCacheDir();

    List<String> getMatchers();

    Map<Integer, String> getInputSchema();

    Map<Integer, String> getOutputSchema();

}
