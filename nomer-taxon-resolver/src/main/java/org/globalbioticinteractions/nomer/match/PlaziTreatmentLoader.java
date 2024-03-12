package org.globalbioticinteractions.nomer.match;

import org.eol.globi.taxon.TaxonCacheListener;

import java.io.InputStream;

public interface PlaziTreatmentLoader {

    void loadTreatment(InputStream is, TaxonCacheListener listener);

}
