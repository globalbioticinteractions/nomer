package org.globalbioticinteractions.nomer.util;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class UUIDUtil {
    private static final Pattern UUID_PATTERN =
            Pattern.compile("([a-fA-F0-9]{8}(-[a-fA-F0-9]{4}){4}[a-fA-F0-9]{8})");

    public static boolean isaUUID(String id) {
        return StringUtils.isNotBlank(id)
                && UUID_PATTERN.asPredicate().test(StringUtils.trim(id));
    }
}
