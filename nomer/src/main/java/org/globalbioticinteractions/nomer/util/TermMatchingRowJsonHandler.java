package org.globalbioticinteractions.nomer.util;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.RowHandler;
import org.eol.globi.taxon.TermMatcher;
import org.eol.globi.util.CSVTSVUtil;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;

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
            appendPrimaryTerm(taxonProvided, taxon, nameType, obj, resolved);
            appendSecondaryTerms(resolved, taxon, obj);
            if (StringUtils.isNotBlank(taxon.getPath())) {
                ObjectNode pathNode = obj.createObjectNode();
                ArrayNode names = addArray(pathNode, "names", taxon.getPath());
                ArrayNode ids = addArray(pathNode, "ids", taxon.getPathIds());
                ArrayNode ranks = addArray(pathNode, "ranks", taxon.getPathNames());
                if (names.size() == ids.size() && names.size() == ranks.size()) {
                    resolved.put("path", pathNode);
                }
            }

            out.println(resolved.toString());
        });
    }

    private ArrayNode addArray(ObjectNode pathNode, String names1, String path) {
        String[] split = CSVTSVUtil.splitPipes(path);
        ArrayNode names = pathNode.putArray(names1);
        for (String s : split) {
            names.add(StringUtils.trim(s));
        }
        return names;
    }

    private void appendSecondaryTerms(ObjectNode resolved, Taxon taxon, ObjectMapper obj) {
        String[] pathRanks = CSVTSVUtil.splitPipes(taxon.getPathNames());
        String[] pathIds = CSVTSVUtil.splitPipes(taxon.getPathIds());
        String[] path = CSVTSVUtil.splitPipes(taxon.getPath());
        for (int i = 0; path != null && i < path.length; i++) {
            if (pathIds != null && pathIds.length == path.length) {
                TaxonImpl taxon1 = new TaxonImpl(StringUtils.trim(path[i]), StringUtils.trim(pathIds[i]));
                if (pathRanks != null && pathRanks.length == path.length) {
                    taxon1.setRank(StringUtils.trim(pathRanks[i]));
                }
                ObjectNode resolvedTaxon = asJsonNode(taxon1, obj);
                appendRankedTaxon(resolved, resolvedTaxon, taxon1.getRank());
            }
        }
    }

    private void appendPrimaryTerm(Taxon taxonProvided, Taxon taxon, NameType nameType, ObjectMapper obj, ObjectNode resolved) {
        ObjectNode resolvedTaxon = asJsonNode(taxon, obj);
        ObjectNode provided = asJsonNode(taxonProvided, obj);
        String relationString = nameType == NameType.SAME_AS ? "equivalent_to" : nameType.name().toLowerCase();
        resolvedTaxon.put(relationString, provided);

        appendRankedTaxon(resolved, resolvedTaxon, taxon.getRank());
    }

    private void appendRankedTaxon(ObjectNode resolved, ObjectNode resolvedTaxon, String rank) {
        String rankLabel = StringUtils.isBlank(rank) ? "norank" : rank.toLowerCase();
        if (!resolved.has(rankLabel)) {
            resolved.put(rankLabel, resolvedTaxon);
        }
    }

    private ObjectNode asJsonNode(Taxon taxon, ObjectMapper obj) {
        ObjectNode resolvedTaxon = obj.createObjectNode();

        if (!StringUtils.isBlank(taxon.getExternalId())) {
            resolvedTaxon.put("@id", semantify(taxon.getExternalId()));
        }
        if (!StringUtils.isBlank(taxon.getName())) {
            resolvedTaxon.put("name", taxon.getName());
        }
        return resolvedTaxon;
    }

    private static String semantify(String externalId) {
        return StringUtils.startsWith(externalId, TaxonomyProvider.ID_PREFIX_NCBI)
                ? StringUtils.replace(externalId, TaxonomyProvider.ID_PREFIX_NCBI, "NCBITaxon:")
                : externalId;
    }

}
