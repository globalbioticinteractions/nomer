package org.eol.globi.taxon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TaxonEnricherImpl extends PropertyEnricherSimple implements TermMatcher {
    private static final Logger LOG = LoggerFactory.getLogger(TaxonEnricherImpl.class);

    private final List<PropertyEnricher> services = new ArrayList<PropertyEnricher>();
    private final HashMap<Class, Integer> errorCounts = new HashMap<Class, Integer>();
    private boolean hasLoggedError = false;

    @Override
    public Map<String, String> enrich(final Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enrichedProperties = new HashMap<String, String>(properties);
        for (PropertyEnricher service : services) {
            try {
                enrichedProperties = enrichTaxonWithPropertyValue(errorCounts, service, properties);
                if (TaxonUtil.isResolved(enrichedProperties)) {
                    break;
                }
            } catch (PropertyEnricherException e) {
                LOG.warn("problem with taxon lookup", e);
                service.shutdown();
            }
        }
        return Collections.unmodifiableMap(TaxonUtil.isResolved(enrichedProperties)
                ? enrichedProperties
                : new HashMap<String, String>(properties) {{
            put(PropertyAndValueDictionary.NAME_MATCH_TYPE, NameType.NONE.name());
        }});
    }

    @Override
    public void shutdown() {
        services.forEach(PropertyEnricher::shutdown);
        services.clear();
    }

    private Map<String, String> enrichTaxonWithPropertyValue(Map<Class, Integer> errorCounts, PropertyEnricher service, Map<String, String> properties) throws
            PropertyEnricherException {
        Integer errorCount = errorCounts.get(service.getClass());
        if (errorCount != null && errorCount > 10) {
            if (!hasLoggedError) {
                LOG.error("skipping taxon match against [" + service.getClass().toString() + "], error count [" + errorCount + "] too high.");
                hasLoggedError = true;
            }
        } else {
            properties = enrichTaxon(errorCounts, service, errorCount, properties);
        }
        return Collections.unmodifiableMap(properties);
    }

    private Map<String, String> enrichTaxon(Map<Class, Integer> errorCounts, PropertyEnricher
            service, Integer errorCount, Map<String, String> properties) throws PropertyEnricherException {
        try {
            Map<String, String> enrichedProperties = service.enrichFirstMatch(properties);
            resetErrorCount(errorCounts, service);
            return enrichedProperties;
        } catch (PropertyEnricherException ex) {
            LOG.warn("failed to find a match for [" + properties + "] in [" + service.getClass().getSimpleName() + "]", ex);
            incrementErrorCount(errorCounts, service, errorCount);
            throw new PropertyEnricherException("re-throwing", ex);
        }
    }

    private void resetErrorCount(Map<Class, Integer> errorCounts, PropertyEnricher service) {
        errorCounts.put(service.getClass(), 0);
    }

    private void incrementErrorCount(Map<Class, Integer> errorCounts, PropertyEnricher
            service, Integer errorCount) {
        if (errorCounts.containsKey(service.getClass()) && errorCount != null) {
            errorCounts.put(service.getClass(), ++errorCount);
        } else {
            resetErrorCount(errorCounts, service);
        }
    }

    public void setServices(List<PropertyEnricher> services) {
        shutdown();
        this.services.addAll(services);
    }

    @Override
    public void match(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException {
        for (Term name : terms) {
            Map<String, String> enriched = enrich(new TreeMap<String, String>() {{
                put(PropertyAndValueDictionary.NAME, name.getName());
                put(PropertyAndValueDictionary.EXTERNAL_ID, name.getId());
            }});
            if (enriched != null) {
                NameType nameMatchType = TaxonUtil.isResolved(enriched) ? NameType.SAME_AS : NameType.NONE;
                termMatchListener.foundTaxonForTerm(null, name, TaxonUtil.mapToTaxon(enriched), nameMatchType);
            }
        }
    }
}
