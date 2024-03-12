package org.globalbioticinteractions.nomer.match;

import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.TermMatchEnsembleFactory;
import org.eol.globi.taxon.TaxonEnricherImpl;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.PropertyEnricherInfo;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TermMatcherFactoryEnricherFactory {

    private static PropertyEnricherInfo getInfoFor(final PropertyEnricher enricher) {
        return enricher.getClass().getAnnotation(PropertyEnricherInfo.class);
    }


    public List<TermMatcherFactory> createTermMatchFactories(TermMatcherContext ctx) {
        return new ArrayList<TermMatcherFactory>() {{
            ArrayList<PropertyEnricher> enrichers = TermMatchEnsembleFactory.getEnrichers(ctx);
            for (PropertyEnricher enricher : enrichers) {
                if (getInfoFor(enricher) != null) {
                    add(new TermMatcherFactory() {
                        @Override
                        public TermMatcher createTermMatcher(TermMatcherContext ctx) {
                            return new TaxonEnricherImpl() {{
                                setServices(Collections.singletonList(enricher));
                            }};
                        }

                        @Override
                        public String getPreferredName() {
                            PropertyEnricherInfo info = getInfoFor(enricher);
                            return info == null ? enricher.getClass().getSimpleName() : info.name();
                        }

                        @Override
                        public String getDescription() {
                            PropertyEnricherInfo info = getInfoFor(enricher);
                            return info == null ? "no description" : info.description();
                        }

                    });
                }
            }
        }};
    }

}
