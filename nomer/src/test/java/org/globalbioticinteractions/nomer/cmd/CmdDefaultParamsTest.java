package org.globalbioticinteractions.nomer.cmd;

import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

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

}