package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Term;

public class TermMatchUtil {
    public static final String WILDCARD_MATCH = ".*";

    public static boolean shouldMatchAll(Term term) {
        return StringUtils.equals(term.getName(), WILDCARD_MATCH)
                && StringUtils.equals(term.getId(), WILDCARD_MATCH);

    }
}
