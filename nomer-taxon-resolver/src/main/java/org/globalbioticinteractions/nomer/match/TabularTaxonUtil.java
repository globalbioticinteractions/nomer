package org.globalbioticinteractions.nomer.match;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.TaxonUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.eol.globi.domain.PropertyAndValueDictionary.AUTHORSHIP;
import static org.eol.globi.domain.PropertyAndValueDictionary.EXTERNAL_ID;
import static org.eol.globi.domain.PropertyAndValueDictionary.NAME_SOURCE;
import static org.eol.globi.domain.PropertyAndValueDictionary.PATH;
import static org.eol.globi.domain.PropertyAndValueDictionary.PATH_NAMES;

public class TabularTaxonUtil {
    public static final Map<TabularColumn, String> TRANSLATION_TABLE = new TreeMap<TabularColumn, String>() {{
        put(TabularColumn.kingdom, TaxonUtil.KINGDOM);
        put(TabularColumn.phylum, TaxonUtil.PHYLUM);
        put(TabularColumn.clazz, TaxonUtil.CLASS);
        put(TabularColumn.subclass, TaxonUtil.SUBCLASS);
        put(TabularColumn.superorder, TaxonUtil.SUPERORDER);
        put(TabularColumn.parvorder, TaxonUtil.PARVORDER);
        put(TabularColumn.order, TaxonUtil.ORDER);
        put(TabularColumn.suborder, TaxonUtil.SUBORDER);

        put(TabularColumn.superfamily, TaxonUtil.SUPERFAMILY);
        put(TabularColumn.family, TaxonUtil.FAMILY);
        put(TabularColumn.subfamily, TaxonUtil.SUBFAMILY);

        put(TabularColumn.genus, TaxonUtil.GENUS);

        put(TabularColumn.specificEpithet, TaxonUtil.SPECIFIC_EPITHET);
        put(TabularColumn.infragenericEpithet, null);
        put(TabularColumn.infraspecificEpithet, TaxonUtil.SUBSPECIFIC_EPITHET);

        put(TabularColumn.subtribe, null);
        put(TabularColumn.scientificNameAuthorship, AUTHORSHIP);
        put(TabularColumn.source, NAME_SOURCE);
        put(TabularColumn.taxonID, EXTERNAL_ID);

    }};
    public static final List<TabularColumn> ORDERED_RANKS = Arrays.asList(
            TabularColumn.kingdom,
            TabularColumn.phylum,
            TabularColumn.clazz,
            TabularColumn.subclass,
            TabularColumn.superorder,
            TabularColumn.order,
            TabularColumn.suborder,
            TabularColumn.infraorder,
            TabularColumn.parvorder,
            TabularColumn.nanorder,
            TabularColumn.superfamily,
            TabularColumn.family,
            TabularColumn.subfamily,
            TabularColumn.tribe,
            TabularColumn.subtribe,
            TabularColumn.genus,
            TabularColumn.infragenericEpithet,
            TabularColumn.specificEpithet,
            TabularColumn.infraspecificEpithet
    );
    public static final String ACCEPTED_NAME_USAGE_ID = "acceptedNameUsageID";


    public static Triple<Taxon, NameType, Taxon> parseNameRelations(LabeledCSVParser labeledCSVParser) {
        Set<TabularColumn> tabularColumns = TRANSLATION_TABLE.keySet();

        Map<String, String> taxonMap = new TreeMap<>();
        List<String> path = new ArrayList<>();
        List<String> pathNames = new ArrayList<>();

        for (TabularColumn orderedRank : ORDERED_RANKS) {
            String value = labeledCSVParser.getValueByLabel(orderedRank.getColumnName());
            if (StringUtils.isNoneBlank(value)
                    && !StringUtils.equals(value, "NA")) {
                path.add(value);
                pathNames.add(orderedRank.getColumnName());
            }
        }


        for (TabularColumn tabularColumn : tabularColumns) {
            String key = TRANSLATION_TABLE.get(tabularColumn);
            if (key != null) {
                String value = labeledCSVParser.getValueByLabel(tabularColumn.getColumnName());
                if (StringUtils.isNoneBlank(value)) {
                    taxonMap.put(key, value);
                }
            }
        }

        taxonMap.put(PATH, StringUtils.join(path, CharsetConstant.SEPARATOR));
        taxonMap.put(PATH_NAMES, StringUtils.join(pathNames, CharsetConstant.SEPARATOR));

        List<String> collect = ORDERED_RANKS
                .stream()
                .map(TRANSLATION_TABLE::get)
                .filter(StringUtils::isNoneBlank)
                .collect(Collectors.toList());
        Collections.reverse(collect);
        Taxon taxon1 = TaxonUtil.generateTaxonName(
                taxonMap,
                collect,
                TaxonUtil.GENUS,
                TaxonUtil.SPECIFIC_EPITHET,
                "InfraspecificEpithet",
                "no:match",
                "no:match");


        Map<String, String> taxonName = TaxonUtil.taxonToMap(taxon1);
        for (String key : taxonName.keySet()) {
            if (StringUtils.isNoneBlank(taxonName.get(key))) {
                taxonMap.put(key, taxonName.get(key));
            }
        }

        Taxon providedTaxon = TaxonUtil.mapToTaxon(taxonMap);

        String taxonomicStatus = labeledCSVParser.getValueByLabel("taxonomicStatus");


        Taxon acceptedTaxon = providedTaxon;
        String acceptedNameUsageID = labeledCSVParser.getValueByLabel(ACCEPTED_NAME_USAGE_ID);
        NameType nameType =
                shouldInferSynonymStatusFromPresenceOfAcceptedNameUsageID(taxonomicStatus, acceptedNameUsageID)
                ? NameType.SYNONYM_OF
                : parseNameType(taxonomicStatus);

        if (NameType.SYNONYM_OF.equals(nameType)) {
            if (StringUtils.isBlank(acceptedNameUsageID)) {
                throw new IllegalStateException("failed to resolve accepted name for [" + TaxonUtil.taxonToMap(providedTaxon) + "]: no [" + ACCEPTED_NAME_USAGE_ID + "]");
            }
            acceptedTaxon = new TaxonImpl();
            acceptedTaxon.setExternalId(acceptedNameUsageID);
        }

        return Triple.of(providedTaxon, nameType, acceptedTaxon);
    }

    private static boolean shouldInferSynonymStatusFromPresenceOfAcceptedNameUsageID(String taxonomicStatus, String acceptedNameUsageID) {
        return StringUtils.isBlank(taxonomicStatus) && StringUtils.isNotBlank(acceptedNameUsageID);
    }

    private static NameType parseNameType(String taxonomicStatus) {
        final Map<String, NameType> statusMap = new TreeMap<String, NameType>() {{
            put("accepted", NameType.HAS_ACCEPTED_NAME);
            put("synonym", NameType.SYNONYM_OF);
            put("heterotypic synonym", NameType.SYNONYM_OF);
            put("doubtful", NameType.NONE);
        }};

        return StringUtils.isBlank(taxonomicStatus)
                ? NameType.HAS_ACCEPTED_NAME
                : statusMap.getOrDefault(StringUtils.lowerCase(taxonomicStatus), NameType.NONE);
    }


}
