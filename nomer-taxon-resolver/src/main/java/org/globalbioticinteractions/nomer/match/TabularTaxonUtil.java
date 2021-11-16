package org.globalbioticinteractions.nomer.match;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.Taxon;
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
import static org.globalbioticinteractions.nomer.match.TabularColumn.clazz;
import static org.globalbioticinteractions.nomer.match.TabularColumn.family;
import static org.globalbioticinteractions.nomer.match.TabularColumn.genus;
import static org.globalbioticinteractions.nomer.match.TabularColumn.infragenericEpithet;
import static org.globalbioticinteractions.nomer.match.TabularColumn.infraorder;
import static org.globalbioticinteractions.nomer.match.TabularColumn.infraspecificEpithet;
import static org.globalbioticinteractions.nomer.match.TabularColumn.kingdom;
import static org.globalbioticinteractions.nomer.match.TabularColumn.nanorder;
import static org.globalbioticinteractions.nomer.match.TabularColumn.order;
import static org.globalbioticinteractions.nomer.match.TabularColumn.parvorder;
import static org.globalbioticinteractions.nomer.match.TabularColumn.phylum;
import static org.globalbioticinteractions.nomer.match.TabularColumn.scientificNameAuthorship;
import static org.globalbioticinteractions.nomer.match.TabularColumn.source;
import static org.globalbioticinteractions.nomer.match.TabularColumn.specificEpithet;
import static org.globalbioticinteractions.nomer.match.TabularColumn.subclass;
import static org.globalbioticinteractions.nomer.match.TabularColumn.subfamily;
import static org.globalbioticinteractions.nomer.match.TabularColumn.suborder;
import static org.globalbioticinteractions.nomer.match.TabularColumn.subtribe;
import static org.globalbioticinteractions.nomer.match.TabularColumn.superfamily;
import static org.globalbioticinteractions.nomer.match.TabularColumn.superorder;
import static org.globalbioticinteractions.nomer.match.TabularColumn.taxonID;
import static org.globalbioticinteractions.nomer.match.TabularColumn.tribe;

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

    public static Taxon parseLine(LabeledCSVParser labeledCSVParser) {
        Set<TabularColumn> tabularColumns = TRANSLATION_TABLE.keySet();

        Map<String, String> taxonMap = new TreeMap<>();
        List<String> path = new ArrayList<>();
        List<String> pathNames = new ArrayList<>();

        for (TabularColumn orderedRank : ORDERED_RANKS) {
            String value = labeledCSVParser.getValueByLabel(orderedRank.name());
            if (StringUtils.isNoneBlank(value)
                    && !StringUtils.equals(value, "NA")) {
                path.add(value);
                pathNames.add(orderedRank.name());
            }
        }


        for (TabularColumn tabularColumn : tabularColumns) {
            String key = TRANSLATION_TABLE.get(tabularColumn);
            if (key != null) {
                String value = labeledCSVParser.getValueByLabel(tabularColumn.name());
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

        Taxon taxon = TaxonUtil.mapToTaxon(taxonMap);
        String externalId = taxon.getExternalId();
        String nameSource = taxon.getNameSource();

        if (StringUtils.isNoneBlank(externalId)){
            if (StringUtils.startsWith(nameSource, "TPT")) {
                taxon.setExternalId("TPT:" + externalId);
            } else if (StringUtils.startsWith(nameSource, "GBIF")) {
                taxon.setExternalId("GBIF:" + externalId);
            }
        }
        return taxon;
    }
}
