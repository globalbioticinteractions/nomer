package org.globalbioticinteractions.nomer.util;

import org.eol.globi.service.ResourceService;
import org.globalbioticinteractions.nomer.cmd.OutputFormat;

import java.util.List;
import java.util.Map;

public interface TermMatcherContext extends PropertyContext, ResourceService {

    String getCacheDir();

    List<String> getMatchers();

    Map<Integer, String> getInputSchema();

    Map<Integer, String> getOutputSchema();

    OutputFormat getOutputFormat();

}
