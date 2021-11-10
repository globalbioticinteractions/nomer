package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.service.ResourceService;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.globalbioticinteractions.nomer.match.ResourceServiceUtil.NOMER_PRESTON_VERSION;

public class ResourceServiceFactoryImpl implements ResourceServiceFactory {

    private final TermMatcherContext ctx;

    public ResourceServiceFactoryImpl(TermMatcherContext termMatcherContext) {
        this.ctx = termMatcherContext;
    }

    @Override
    public ResourceService createResourceService() throws IOException {
        final List<ResourceService> services = new ArrayList<>();

        services.add(new ResourceServiceReadOnly(ctx));

        if (StringUtils.isBlank(ctx.getProperty(NOMER_PRESTON_VERSION))) {
            services.add(new ResourceServiceLocationBased(ctx));
        } else {
            services.add(new ResourceServiceContentBased(ctx));
        }

        return new ResourceService() {
            @Override
            public InputStream retrieve(URI uri) throws IOException {
                InputStream is = null;
                for (ResourceService service : services) {
                    is = service.retrieve(uri);
                    if (is != null) {
                        break;
                    }
                }

                if (is == null) {
                    throw new IOException("failed to access [" + uri.toString() + "]");
                }

                return is;
            }
        };

    }

}
