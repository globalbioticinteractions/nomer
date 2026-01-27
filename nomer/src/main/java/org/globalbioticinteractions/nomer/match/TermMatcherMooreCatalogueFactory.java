package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.TermMatcherContext;

public class TermMatcherMooreCatalogueFactory implements TermMatcherFactory {

	@Override
	public String getDescription() {
		return "Lookup Moure's Catalog for Neotropical bee species taxon by name using offline-enabled databases dump";
	}

	@Override
	public String getPreferredName() {
		return "moure";
	}


	@Override
	public TermMatcher createTermMatcher(TermMatcherContext ctx) { 
		return new MoureCatalogueTaxonService(ctx);
	}
}
