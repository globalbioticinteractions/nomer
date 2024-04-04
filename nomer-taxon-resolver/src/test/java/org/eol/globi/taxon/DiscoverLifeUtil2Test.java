package org.eol.globi.taxon;

import org.junit.Test;

import java.io.IOException;

public class DiscoverLifeUtil2Test {

    @Test
    public void compareLocalVersionToRemoteVersion() throws IOException {
        DiscoverLifeTestUtil.compareLocalVersionToRemoteVersion(
                "/org/globalbioticinteractions/nomer/match/discoverlife/bees.xml.gz",
                DiscoverLifeUtil.URL_ENDPOINT_DISCOVER_LIFE + "/nh/id/20q/Apoidea_species.xml"
        );
    }


}