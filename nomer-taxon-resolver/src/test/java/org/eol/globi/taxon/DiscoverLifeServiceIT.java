package org.eol.globi.taxon;

import org.apache.commons.io.IOUtils;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;

public class DiscoverLifeServiceIT {

    @Test
    public void getCurrentBeeNames() throws IOException {
        String actual = DiscoverLifeService.getBeeNamesAsXmlString();

        String localCopy = IOUtils.toString(DiscoverLifeService.getStreamOfBees(), StandardCharsets.UTF_8);
        assertThat(actual, Is.is(localCopy));
    }

}
