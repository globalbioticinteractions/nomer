package org.eol.globi.taxon;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.ORCIDResolverImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.globalbioticinteractions.nomer.util.PropertyEnricherInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PropertyEnricherInfo(name = "orcid-web", description = "Lookup ORCID by id with ORCID:* prefix.")
public class ORCIDService extends PropertyEnricherSimple {

    private static final Pattern PATTERN = Pattern.compile(".*(orcid)*.*([0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{4}).*", Pattern.CASE_INSENSITIVE);

    @Override
    public Map<String, String> enrich(final Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enrichedProperties = new HashMap<String, String>(properties);
        String id = properties.get(PropertyAndValueDictionary.EXTERNAL_ID);
        Matcher matcher = PATTERN.matcher(id);
        if (matcher.matches()) {
            try {
                String orcid = matcher.group(2);
                Map<String, String> match = new ORCIDResolverImpl().findAuthor(orcid);
                if (match.isEmpty()) {
                    throw new PropertyEnricherException("no match for orcid [" + orcid + "]");
                }
                enrichedProperties.putAll(match);
            } catch (IOException e) {
                throw new PropertyEnricherException("failed to lookup name associated with ORCID [" + id + "]", e);
            }
        }
        return enrichedProperties;
    }

    @Override
    public void shutdown() {

    }
}
