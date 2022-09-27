package org.globalbioticinteractions.nomer.cmd;

import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;

public class CmdDefaultParamsTest {

    @Test
    public void getProperty() {
        CmdDefaultParams cmdMatcherParams = new CmdDefaultParams() {
        };

        assertNull(System.getProperty("foo"));
        System.setProperty("foo", "bar");
        assertNotNull(System.getProperty("foo"));

        assertThat(cmdMatcherParams.getProperty("foo"), Is.is("bar"));
        System.clearProperty("foo");

        String propertyDefault = cmdMatcherParams.getProperty("nomer.nodc.url");

        System.setProperty("nomer.nodc.url", "testing123");

        assertThat(cmdMatcherParams.getProperty("nomer.nodc.url"), Is.is("testing123"));

        System.clearProperty("nomer.nodc.url");

        assertThat(cmdMatcherParams.getProperty("nomer.nodc.url"), Is.is(propertyDefault));

    }

    @Test
    public void withPropertiesFile() throws URISyntaxException {
        CmdDefaultParams cmdDefaultParams = new CmdDefaultParams() {

        };

        URL resource = getClass().getResource("some.properties");

        File file = new File(resource.toURI());

        assertTrue(file.exists());

        cmdDefaultParams.setPropertiesResource(file.getAbsolutePath());

        assertThat(cmdDefaultParams.getProperty("foo"), Is.is("bar"));

    }

    @Test
    public void withPropertiesFileURL() throws URISyntaxException {
        CmdDefaultParams cmdDefaultParams = new CmdDefaultParams() {

        };

        URL resource = getClass().getResource("some.properties");

        File file = new File(resource.toURI());

        assertTrue(file.exists());

        cmdDefaultParams.setPropertiesResource("file://" + file.getAbsolutePath());

        assertThat(cmdDefaultParams.getProperty("foo"), Is.is("bar"));

    }

    @Test
    public void withPropertiesResource() throws URISyntaxException {
        CmdDefaultParams cmdDefaultParams = new CmdDefaultParams() {

        };

        cmdDefaultParams.setPropertiesResource("classpath:/org/globalbioticinteractions/nomer/cmd/some.properties");

        assertThat(cmdDefaultParams.getProperty("foo"), Is.is("bar"));

    }

    @Test(expected = RuntimeException.class)
    public void withNonURIPropertyResource() throws URISyntaxException {
        CmdDefaultParams cmdDefaultParams = new CmdDefaultParams() {

        };
        try {
            cmdDefaultParams.setPropertiesResource("some.properties");
            cmdDefaultParams.getProperty("foo");
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage(), Is.is("failed to load properties: please make sure that [some.properties] is a valid URI"));
            throw ex;
        }

    }

}