package org.eol.globi.service;

import java.io.IOException;
import java.util.Map;

public interface AuthorIdResolver {
    Map<String, String> findAuthor(String authorURI) throws IOException;
}
