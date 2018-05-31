package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.apache.commons.collections4.list.TreeList;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.globalbioticinteractions.nomer.match.TermMatcherFactory;
import org.globalbioticinteractions.nomer.match.TermMatcherRegistry;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Parameters(separators = "= ", commandDescription = "Lists supported matcher and descriptions in JSONT.")
public class CmdMatchers implements Runnable {

    @Parameter(names = {"-o", "--output-format"}, description = "tsv, json", validateWith = JsonTsvFormatValidator.class)
    private String outputFormat = "tsv";

    @Parameter(names = {"-v", "--verbose"}, description = "if set, matcher descriptions are included for tsv.")
    private Boolean verbose = false;

    @Override
    public void run() {
        String outputString;
        Map<String, TermMatcherFactory> registry = TermMatcherRegistry.getRegistry(null);
        if ("json".equalsIgnoreCase(outputFormat)) {
            ObjectMapper f = new ObjectMapper();
            ArrayNode matchers = f.createArrayNode();
            for (Map.Entry<String, TermMatcherFactory> matcherEntry : registry.entrySet()) {
                ObjectNode matcher = f.createObjectNode();
                matcher.put("name", matcherEntry.getKey());
                matcher.put("description", matcherEntry.getValue().getDescription());
                matchers.add(matcher);
            }
            try {
                outputString = f.writerWithDefaultPrettyPrinter().writeValueAsString(matchers);
            } catch (IOException e) {
                throw new RuntimeException("failed to create a json list of matchers", e);
            }
        } else {
            List<String> lines = new TreeList<>();
            for (Map.Entry<String, TermMatcherFactory> matcherEntry : registry.entrySet()) {
                if (verbose) {
                    lines.add(matcherEntry.getKey() + "\t" + matcherEntry.getValue().getDescription());
                } else {
                    lines.add(matcherEntry.getKey());
                }
            }
            Collections.sort(lines);
            outputString = StringUtils.join(lines, '\n');
        }
        System.out.println(outputString);
    }
}
