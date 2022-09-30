package org.globalbioticinteractions.nomer.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.globalbioticinteractions.nomer.match.TermMatcherContextCaching;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public abstract class CmdMatcherParams extends TermMatcherContextCaching implements Runnable {

    @CommandLine.Parameters(description = "[matcher]")
    private List<String> matchers = new ArrayList<>();

    @Override
    public OutputFormat getOutputFormat() {
        return CmdOutput.OUTPUT_FORMAT_DEFAULT;
    }

    @Override
    public List<String> getMatchers() {
        return matchers;
    }

    @Override
    public String getCacheDir() {
        String property = getProperty("nomer.cache.dir");
        File cacheDir = StringUtils.isBlank(property)
                ? getOrCreateDefaultCacheDir()
                : getOrCreateCacheDir(new File(property));
        return cacheDir.getAbsolutePath();
    }

    @Override
    public Map<Integer, String> getInputSchema() {
        return parseSchema(getProperty("nomer.schema.input"));
    }

    @Override
    public Map<Integer, String> getOutputSchema() {
        return parseSchema(getProperty("nomer.schema.output"));
    }

    public static Map<Integer, String> parseSchema(String schema) {
        Map<Integer, String> schemaMap = new TreeMap<>();
        try {
            if (StringUtils.isNoneBlank(schema)) {
                JsonNode jsonNode = new ObjectMapper().readTree(schema);
                if (jsonNode.isArray() && jsonNode.size() > 0) {
                    for (JsonNode node : jsonNode) {
                        schemaMap.put(node.get("column").asInt(), node.get("type").asText());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("failed to parse schema \"" + schema + "\"", e);
        }
        TreeMap<Integer, String> defaultSchema = new TreeMap<Integer, String>() {{
            put(0, PropertyAndValueDictionary.EXTERNAL_ID);
            put(1, PropertyAndValueDictionary.NAME);
        }};
        return MapUtils.unmodifiableMap(schemaMap.size() < 1
                ? defaultSchema
                : schemaMap);
    }

    public static File getOrCreateDefaultCacheDir() {
        File userHome = new File(System.getProperty("user.home"));
        return getOrCreateCacheDir(new File(userHome, ".cache/nomer"));
    }

    private static File getOrCreateCacheDir(File cacheDir) {
        if (!cacheDir.exists()) {
            try {
                FileUtils.forceMkdir(cacheDir);
            } catch (IOException ex) {
                throw new IllegalArgumentException("invalid or missing cachedir [" + cacheDir.getAbsolutePath() + "]");
            }
        }
        return cacheDir;
    }

}
