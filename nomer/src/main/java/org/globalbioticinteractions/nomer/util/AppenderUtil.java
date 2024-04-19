package org.globalbioticinteractions.nomer.util;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.CSVTSVUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class AppenderUtil {

    static String valueForTaxonPropertyName2(Taxon taxon, Map<String, String> taxonMap, String taxonPropertyName) {
        String taxonPropertyValue = taxonMap.get(taxonPropertyName);

        taxonPropertyValue =
                taxonPropertyValue == null
                        ? valueForTaxonProperty(taxon, taxonPropertyName)
                        : taxonPropertyValue;
        return taxonPropertyValue;
    }

    static String valueForTaxonPropertyName(Taxon taxon, String taxonPropertyName) {
        Map<String, String> taxonMap = TaxonUtil.taxonToMap(taxon);

        String taxonPropertyValue = taxonMap.get(taxonPropertyName);

        taxonPropertyValue =
                taxonPropertyValue == null
                        ? valueForTaxonProperty(taxon, taxonPropertyName)
                        : taxonPropertyValue;
        return taxonPropertyValue;
    }

    public static String valueForTaxonProperty(Taxon taxon,
                                               String taxonPropertyName) {
        List<String> ranks = splitAndTrim(taxon.getPathNames());
        List<String> ids = splitAndTrim(taxon.getPathIds());
        List<String> names = splitAndTrim(taxon.getPath());
        String colValue = "";
        if (StringUtils.equalsIgnoreCase(taxonPropertyName, "id")) {
            colValue = taxon.getExternalId();
        } else if (StringUtils.equalsIgnoreCase(taxonPropertyName, "name")) {
            colValue = taxon.getName();
        } else if (StringUtils.equalsIgnoreCase(taxonPropertyName, "rank")) {
            colValue = taxon.getRank();
        } else if (StringUtils.equalsIgnoreCase(taxonPropertyName, "authorship")) {
            colValue = taxon.getAuthorship();
        } else if (StringUtils.equalsIgnoreCase(taxonPropertyName, "path.id")) {
            colValue = taxon.getPathIds();
        } else if (StringUtils.equalsIgnoreCase(taxonPropertyName, "path.name")) {
            colValue = taxon.getPath();
        } else if (StringUtils.equalsIgnoreCase(taxonPropertyName, "path.rank")) {
            colValue = taxon.getPathNames();
        } else if (StringUtils.equalsIgnoreCase(taxonPropertyName, "path.authorship")) {
            colValue = taxon.getPathAuthorships();
        } else if (StringUtils.startsWith(taxonPropertyName, "path.")
                && ranks.size() > 0
                && ranks.size() == ids.size()
                && names.size() == ids.size()) {
            String[] split = StringUtils.split(taxonPropertyName, '.');
            if (split != null && split.length > 1) {
                String rank = split[1];
                int i1 = ranks.indexOf(rank);
                if (i1 > -1) {
                    if (split.length > 2) {
                        boolean shouldUseId = "id".equalsIgnoreCase(split[2]);
                        colValue = shouldUseId
                                ? ids.get(i1)
                                : names.get(i1);
                    } else {
                        colValue = rank;
                    }
                }
            }
        }
        return colValue;
    }

    private static List<String> splitAndTrim(String pathNames) {
        return StringUtils.isBlank(pathNames)
                ? Collections.emptyList()
                : Arrays.stream(CSVTSVUtil.splitPipes(pathNames)).map(String::trim).collect(Collectors.toList());
    }
}
