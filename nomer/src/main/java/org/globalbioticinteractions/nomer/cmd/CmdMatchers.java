package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.eol.globi.util.CSVTSVUtil;
import org.globalbioticinteractions.nomer.util.TermMatcherFactory;
import org.globalbioticinteractions.nomer.util.TermMatcherRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Parameters(separators = "= ", commandDescription = "Lists supported matcher and descriptions in JSONT.")
public class CmdMatchers implements Runnable {

    @Parameter(names = {"-o", "--output-format"}, description = "tsv, json", validateWith = JsonTsvFormatValidator.class)
    private String outputFormat = "tsv";

    @Parameter(names = {"-v", "--verbose"}, description = "if set, matcher descriptions are included for tsv.")
    private Boolean verbose = false;

    @Override
    public void run() {
        String outputString;
        if ("json".equalsIgnoreCase(outputFormat)) {
            ObjectMapper f = new ObjectMapper();
            ArrayNode matchers = f.createArrayNode();
            for (Map.Entry<String, TermMatcherFactory> matcherEntry : TermMatcherRegistry.registry.entrySet()) {
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
            List<String> lines = new ArrayList<>();
            for (Map.Entry<String, TermMatcherFactory> matcherEntry : TermMatcherRegistry.registry.entrySet()) {
                if (verbose) {
                    lines.add(matcherEntry.getKey() + "\t" + matcherEntry.getValue().getDescription());
                } else {
                    lines.add(matcherEntry.getKey());
                }
            }
            outputString = StringUtils.join(lines, '\n');
        }
        System.out.println(outputString);
    }
}
