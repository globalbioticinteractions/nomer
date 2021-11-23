package org.globalbioticinteractions.nomer.match;

import org.apache.commons.lang3.StringUtils;

public enum TabularColumn {
    source,
    taxonID,
    scientificNameID,
    acceptedNameUsageID,
    parentNameUsageID,
    originalNameUsageID,
    nameAccordingToID,
    namePublishedInID,
    taxonConceptID,
    scientificName,
    acceptedNameUsage,
    parentNameUsage,
    originalNameUsage,
    nameAccordingTo,
    namePublishedIn,
    namePublishedInYear,
    higherClassification,
    kingdom,
    phylum,
    clazz("class"),
    subclass,
    superorder,
    order,
    suborder,
    infraorder,
    parvorder,
    nanorder,
    superfamily,
    family,
    subfamily,
    tribe,
    subtribe,
    genus,
    infragenericEpithet,
    specificEpithet,
    infraspecificEpithet,
    taxonRank,
    verbatimTaxonRank,
    scientificNameAuthorship,
    vernacularName,
    nomenclaturalCode,
    taxonomicStatus,
    nomenclaturalStatus,
    taxonRemarks,
    canonical;

    public String getColumnName() {
        return StringUtils.isBlank(columnName) ? name() : columnName;
    }

    private String columnName;

    TabularColumn(String columnName) {
        this.columnName = columnName;
    }

    TabularColumn() {
    }




}
