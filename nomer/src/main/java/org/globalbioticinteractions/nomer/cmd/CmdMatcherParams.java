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

    public static final String NOMER_SCHEMA_INPUT = "nomer.schema.input";
    public static final String NOMER_SCHEMA_OUTPUT = "nomer.schema.output";
    @CommandLine.Parameters(description = "[matcher]", arity = "1")
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
    public Map<Integer, String> getInputSchema() {
        return parseSchema(getProperty(NOMER_SCHEMA_INPUT));
    }

    @Override
    public Map<Integer, String> getOutputSchema() {
        return parseSchema(getProperty(NOMER_SCHEMA_OUTPUT));
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


}
