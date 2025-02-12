package org.eol.globi.taxon;

import org.apache.commons.io.IOUtils;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

public class BatNamesUtilIT {

    private final boolean shouldReplaceResource = false;

    @Test
    public void getExplorePage() throws IOException, URISyntaxException {
        String htmlAsXmlString = HtmlUtil.getHtmlAsXmlString("https://batnames.org/explore.html");

        String patchedXml = BatNamesUtil.toPatchedXmlString(htmlAsXmlString);

        String resourceName = "/org/globalbioticinteractions/nomer/match/batnames/explore.xml";
        replaceResource(patchedXml, resourceName);

        String expectedPatchedXml = IOUtils.toString(getClass().getResourceAsStream(resourceName), StandardCharsets.UTF_8);

        assertThat(patchedXml, Is.is(expectedPatchedXml));

    }

    private void replaceResource(String patchedXml, String resourceName) throws IOException, URISyntaxException {
        if (shouldReplaceResource) {
            URL resource = getClass().getResource(resourceName);
            IOUtils.copy(IOUtils.toInputStream(patchedXml, StandardCharsets.UTF_8), new FileOutputStream(new File(resource.toURI())));
        }
    }

    @Test
    public void getGenusPageRhinolophus() throws IOException, URISyntaxException {
        compareGenusPage("Rhinolophus");
    }

    @Test
    public void getGenusPageCistugo() throws IOException, URISyntaxException {
        compareGenusPage("Cistugo");
    }

    @Test
    public void getGenusPageMiniopterus() throws IOException, URISyntaxException {
        compareGenusPage("Miniopterus");
    }

    @Test
    public void getGenusPageNesonycteris() throws IOException, URISyntaxException {
        compareGenusPage("Nesonycteris");
    }

    @Test
    public void getGenusPageMormoops() throws IOException, URISyntaxException {
        compareGenusPage("Mormoops");
    }

    public void compareGenusPage(String genusName) throws IOException, URISyntaxException {
        String patchedXml = BatNamesUtil.getGenusXml(BatNamesUtil.getGenusUrl(genusName));

        String genusResourceName = "/org/globalbioticinteractions/nomer/match/batnames/" + genusName + ".xml";
        replaceResource(patchedXml, genusResourceName);

        String expectedPatchedXml = IOUtils.toString(getClass().getResourceAsStream(genusResourceName), StandardCharsets.UTF_8);

        assertThat(patchedXml, Is.is(expectedPatchedXml));
    }


}