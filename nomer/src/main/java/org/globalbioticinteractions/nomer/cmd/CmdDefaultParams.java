package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameter;

import java.util.ArrayList;
import java.util.List;

abstract class CmdDefaultParams implements Runnable {
    @Parameter(names = {"--cache-dir", "-c"}, description = "cache directory")
    private String cacheDir = "./datasets";

    String getCacheDir() {
        return cacheDir;
    }

    @Parameter(description = "[matcher1] [matcher2] ...")
    private List<String> matchers = new ArrayList<>();

    List<String> getMatchers() {
        return matchers;
    }
}
