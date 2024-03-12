package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.globalbioticinteractions.nomer.util.CacheUtil;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.io.IOException;

public final class ServiceUtil {


    public static void initWith(Initializing service, String propertyName, TermMatcherContext ctx) {
        try {
            if (ctx != null && StringUtils.isNoneBlank(ctx.getProperty(propertyName))) {
                service.init(ctx.retrieve(
                        CacheUtil.getValueURI(ctx, propertyName))
                );
            }
        } catch (IOException | PropertyEnricherException e) {
            throw new IllegalArgumentException("failed to instantiate name service [" + service.getClass().getSimpleName() + "] using property [" + propertyName + "]", e);
        }
    }
}
