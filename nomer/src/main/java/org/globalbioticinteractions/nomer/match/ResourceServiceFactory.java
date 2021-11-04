package org.globalbioticinteractions.nomer.match;

import org.eol.globi.service.ResourceService;

import java.io.IOException;

public interface ResourceServiceFactory {

    ResourceService createResourceService() throws IOException;
}
