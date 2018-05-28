package org.eol.globi.taxon;

import org.apache.commons.io.Charsets;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.util.ResourceUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RemoveStopWordService implements org.eol.globi.service.NameSuggester {
    private static final Log LOG = LogFactory.getLog(RemoveStopWordService.class);

    private List<String> stopwords = null;

    private String resource = "classpath:/org/eol/globi/service/non-taxa-words.tsv";

    @Override
    public String suggest(String name) {
        String suggested = name;
        if (StringUtils.isNotBlank(name)) {
            init();
            suggested = Stream.of(name)
                    .flatMap(s -> Stream.of(StringUtils.split(s, " ()-")))
                    .map(StringUtils::trim)
                    .filter(w -> !stopwords.contains(StringUtils.lowerCase(w)))
                    .collect(Collectors.joining(" "));

        }
        return StringUtils.length(name) == StringUtils.length(suggested) ? name : suggested;
    }

    private void init() {
        if (stopwords == null) {
            InputStream is;
            try {
                is = ResourceUtil.asInputStream(resource);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charsets.UTF_8));
                stopwords = reader.lines().map(StringUtils::lowerCase).collect(Collectors.toList());
            } catch (IOException e) {
                LOG.error("failed to read stopwords at [" + resource + "]", e);
            } finally {
                if (stopwords == null) {
                    stopwords = new ArrayList<>();
                }
            }
        }
    }
}
