package org.globalbioticinteractions.nomer.match;

public class PlaziTreatmentXMLLoaderTest extends PlaziTreatmentLoaderTest {

    public PlaziTreatmentLoader createLoader() {
        return new PlaziTreatmentXMLLoader();
    }

    public String getExtension() {
        return ".xml";
    }


}
