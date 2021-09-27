package org.eol.globi.taxon;

import org.apache.http.client.cache.Resource;
import org.eol.globi.service.ResourceService;

import java.io.IOException;
import java.util.Map;

public interface TaxonReaderFactory {
    Map<String, Resource> getResources() throws IOException;
}
