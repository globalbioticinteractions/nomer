package org.globalbioticinteractions.nomer.util;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.RowHandler;
import org.eol.globi.taxon.TermMatcher;
import org.eol.globi.util.ExternalIdUtil;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TermMatchingRowJsonHandler implements RowHandler {
    private final TermMatcherContext ctx;
    private final TermMatcher matcher;
    private PrintStream out;

    public TermMatchingRowJsonHandler(OutputStream os, TermMatcher matcher, TermMatcherContext ctx) {
        this.ctx = ctx;
        this.matcher = matcher;
        this.out = new PrintStream(os);
    }

    @Override
    public void onRow(String[] row) throws PropertyEnricherException {
        final Taxon taxonProvided = TermMatchingRowHandler.asTaxon(row, ctx.getInputSchema());
        matcher.findTerms(Collections.singletonList(taxonProvided), (id, name, taxon, nameType) -> {
            ObjectMapper obj = new ObjectMapper();
            ObjectNode resolved = obj.createObjectNode();
            ObjectNode resolvedTaxon = asJsonNode(taxon, obj);
            ObjectNode provided = asJsonNode(taxonProvided, obj);
            String relationString = nameType == NameType.SAME_AS ? "equivalent_to" : nameType.name().toLowerCase();
            resolvedTaxon.put(relationString, provided);

            resolved.put(StringUtils.isBlank(taxon.getRank()) ? "norank" : taxon.getRank().toLowerCase(), resolvedTaxon);
            out.println(resolved.toString());
        });
    }

    private ObjectNode asJsonNode(Taxon taxon, ObjectMapper obj) {
        ObjectNode resolvedTaxon = obj.createObjectNode();

        resolvedTaxon.put("@id", semantify(taxon.getExternalId()));
        resolvedTaxon.put("name", taxon.getName());
        return resolvedTaxon;
    }

    private static String semantify(String externalId) {
        return StringUtils.startsWith(externalId, TaxonomyProvider.ID_PREFIX_NCBI)
                ? StringUtils.replace(externalId, TaxonomyProvider.ID_PREFIX_NCBI, "NCBITaxon:")
                : externalId;
    }

}
