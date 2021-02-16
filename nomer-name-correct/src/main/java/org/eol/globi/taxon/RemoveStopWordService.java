package org.eol.globi.taxon;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eol.globi.service.Initializing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RemoveStopWordService implements org.eol.globi.service.NameSuggester, Initializing {
    private static final Logger LOG = LoggerFactory.getLogger(RemoveStopWordService.class);

    private List<String> stopwords = null;

    @Override
    public String suggest(String name) {
        String suggested = name;
        if (StringUtils.isNotBlank(name)) {
            init();
            String[] nameParts = StringUtils.splitByCharacterTypeCamelCase(name);
            Stream<String> suggestedParts = Stream.of(nameParts)
                    .filter(w -> !stopwords.contains(StringUtils.trim(StringUtils.lowerCase(w))));
            List<String> collect = suggestedParts.collect(Collectors.toList());
            suggested = collect.size() == nameParts.length ? name : StringUtils.join(collect, "");

        }
        return StringUtils.length(name) == StringUtils.length(suggested)
                ? name
                : StringUtils.trim(suggested);
    }

    private void init() {
        if (stopwords == null) {
            throw new IllegalStateException("not yet initialized");
        }
    }

    @Override
    public void init(InputStream is) throws IOException {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            stopwords = reader.lines().map(StringUtils::lowerCase).collect(Collectors.toList());
        } finally {
            if (stopwords == null) {
                stopwords = new ArrayList<>();
            }
        }
    }
}
