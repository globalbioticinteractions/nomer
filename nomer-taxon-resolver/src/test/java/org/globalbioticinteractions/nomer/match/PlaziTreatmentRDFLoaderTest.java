package org.globalbioticinteractions.nomer.match;

public class PlaziTreatmentRDFLoaderTest extends PlaziTreatmentLoaderTest {

    public PlaziTreatmentLoader createLoader() {
        return new PlaziTreatmentRDFLoader();
    }

    public String getExtension() {
        return ".ttl";
    }


}
