package org.eol.globi.taxon;

import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class PropertyEnricherSimple implements PropertyEnricher {

    abstract public Map<String, String> enrich(Map<String, String > properties) throws PropertyEnricherException;

    @Override
    public Map<String, String> enrichFirstMatch(Map<String, String> properties) throws PropertyEnricherException {
        return enrich(properties);
    }

    @Override
    public List<Map<String, String>> enrichAllMatches(Map<String, String> properties) throws PropertyEnricherException {
        return Collections.singletonList(enrich(properties));
    }

}
