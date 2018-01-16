package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameter;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.globalbioticinteractions.nomer.util.TermMatcherContextCaching;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

abstract class CmdDefaultParams extends TermMatcherContextCaching implements Runnable {

    private static final Log LOG = LogFactory.getLog(CmdDefaultParams.class);
    public static final String SCHEMA_DEFAULT = "[1,2]";

    @Parameter(names = {"--cache-dir", "-c"}, description = "cache directory")
    private String cacheDir = "./.nomer";

    @Parameter(names = {"--schema", "-s"}, description = "terse schema definition: [[column index term id1, column index term label1], [column index term id2, column index term label2], ...]")
    private String schema = SCHEMA_DEFAULT;

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

    @Override
    public List<String> getMatchers() {
        return matchers;
    }

    @Override
    public Pair<Integer, Integer> getSchema() {
        String schema = this.schema;
        return parseSchema(schema);
    }

    static Pair<Integer, Integer> parseSchema(String schema) {
        Pair<Integer, Integer> termIdLabelColumnPair = null;
        try {
            JsonNode jsonNode = new ObjectMapper().readTree(schema);
            if (jsonNode.isArray() && jsonNode.size() > 1) {
                termIdLabelColumnPair = new ImmutablePair<>(jsonNode.get(0).asInt(), jsonNode.get(1).asInt());
            }
        } catch (IOException e) {
            LOG.error("failed to parse schema \"" + schema + "\", returning default \"" + SCHEMA_DEFAULT + "\" instead.", e);

        }
        return termIdLabelColumnPair == null
                ? new ImmutablePair<>(1, 2)
                : termIdLabelColumnPair;
    }
}
