package org.globalbioticinteractions.nomer.cmd;

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
import java.util.Map;
import java.util.stream.Stream;

@Parameters(separators = "= ", commandDescription = "Lists supported matcher and descriptions in JSONT.")
public class CmdMatchers implements Runnable {

    @Override
    public void run() {
        ObjectMapper f = new ObjectMapper();
        ArrayNode matchers = f.createArrayNode();
        for (Map.Entry<String, TermMatcherFactory> matcherEntry : TermMatcherRegistry.registry.entrySet()) {
            ObjectNode matcher = f.createObjectNode();
            matcher.put("name", matcherEntry.getKey());
            matcher.put("description", matcherEntry.getValue().getDescription());
            matchers.add(matcher);
        }
        try {
            System.out.println(f.writerWithDefaultPrettyPrinter().writeValueAsString(matchers));
        } catch (IOException e) {
            //
        }
    }
}
