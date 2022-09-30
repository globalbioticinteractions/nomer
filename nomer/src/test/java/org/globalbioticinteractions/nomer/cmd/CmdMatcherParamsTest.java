package org.globalbioticinteractions.nomer.cmd;

import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;

public class CmdMatcherParamsTest {

    @Test
    public void testSchema() {
        Map<Integer, String> pair =
                CmdMatcherParams.parseSchema("[ {\"column\": 2, \"type\": \"externalId\"}, {\"column\": 3, \"type\": \"name\"}]");
        assertThat(pair, Is.is(new TreeMap<Integer, String>() {{
            put(2, "externalId");
            put(3, "name");
        }}));
    }

    @Test
    public void defaultSchema() {
        Map<Integer, String> schema = new CmdMatcherParams() {

            @Override
            public void run() {

            }
        }.getInputSchema();
        assertThat(schema, Is.is(new TreeMap<Integer, String>() {{
            put(0, "externalId");
            put(1, "name");
        }}));
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidSchema() {
        CmdMatcherParams.parseSchema("[ this ain't valid n\": 3, \"type\": \"name\"}]");
    }

    @Test
    public void testGetCacheDir() {
        CmdMatcherParams cmd = new CmdMatcherParams() {

            @Override
            public void run() {

            }
        };


        File cachedir = CmdMatcherParams.getOrCreateDefaultCacheDir();
        assertThat(cmd.getCacheDir(), Is.is(cachedir.getAbsolutePath()));
    }

    @Test
    public void testInvalidCacheDir() {
        CmdMatcherParams cmd = new CmdMatcherParams() {

            @Override
            public void run() {

            }
        };


        File cachedir = CmdMatcherParams.getOrCreateDefaultCacheDir();
        assertThat(cmd.getCacheDir(), Is.is(cachedir.getAbsolutePath()));
    }

    @Test
    public void testGetCacheDirOverride() {
        CmdMatcherParams cmd = new CmdMatcherParams() {

            @Override
            public void run() {

            }
        };

        cmd.setPropertiesResource(getClass().getResource("cachedir.properties").toExternalForm());


        assertThat(cmd.getCacheDir(), Is.is(new File("./target/.nomer").getAbsolutePath()));
    }


}