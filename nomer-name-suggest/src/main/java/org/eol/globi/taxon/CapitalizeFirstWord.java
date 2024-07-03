package org.eol.globi.taxon;

import org.eol.globi.service.NameSuggester;

public class CapitalizeFirstWord implements NameSuggester {

    @Override
    public String suggest(final String name) {
        return Capitalizer.capitalize(name);
    }

}
