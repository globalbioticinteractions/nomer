package org.eol.globi.taxon;

import org.apache.commons.io.IOUtils;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

public class BatNamesUtilIT {

    @Test
    public void getExplorePage() throws IOException {
        String htmlAsXmlString = HtmlUtil.getHtmlAsXmlString("https://batnames.org/explore.html");

        String patchedXml = BatNamesUtil.toPatchedXmlString(htmlAsXmlString);

        IOUtils.copy(IOUtils.toInputStream(patchedXml, StandardCharsets.UTF_8), new FileOutputStream("/home/jorrit/proj/globi/nomer/nomer-taxon-resolver/src/test/resources/org/globalbioticinteractions/nomer/match/batnames/explore.xml"));

        String expectedPatchedXml = IOUtils.toString(getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/batnames/explore.xml"), StandardCharsets.UTF_8);

        assertThat(patchedXml, Is.is(expectedPatchedXml));

    }

    @Test
    public void getGenusPage() throws IOException {
        String patchedXml = BatNamesUtil.getGenusXml("Rhinolophus");

        IOUtils.copy(IOUtils.toInputStream(patchedXml, StandardCharsets.UTF_8), new FileOutputStream("/home/jorrit/proj/globi/nomer/nomer-taxon-resolver/src/test/resources/org/globalbioticinteractions/nomer/match/batnames/rhinolophus.xml"));

        String expectedPatchedXml = IOUtils.toString(getClass().getResourceAsStream("/org/globalbioticinteractions/nomer/match/batnames/rhinolophus.xml"), StandardCharsets.UTF_8);

        assertThat(patchedXml, Is.is(expectedPatchedXml));

    }


}