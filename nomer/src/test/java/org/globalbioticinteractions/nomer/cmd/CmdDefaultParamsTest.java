package org.globalbioticinteractions.nomer.cmd;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class CmdDefaultParamsTest {

    @Test
    public void testSchema() {
        Map<Integer, String> pair =
                CmdDefaultParams.parseSchema("[ {\"column\": 2, \"type\": \"externalId\"}, {\"column\": 3, \"type\": \"name\"}]");
        assertThat(pair, Is.is(new TreeMap<Integer, String>() {{
            put(2, "externalId");
            put(3, "name");
        }}));
    }

    @Test
    public void defaultSchema() {
        Map<Integer, String> pair =
                CmdDefaultParams.parseSchema(CmdDefaultParams.SCHEMA_DEFAULT);
        assertThat(pair, Is.is(new TreeMap<Integer, String>() {{
            put(0, "externalId");
            put(1, "name");
        }}));
    }

   @Test
    public void testInvalidSchema() {
       Map<Integer, String> pair =
               CmdDefaultParams.parseSchema("[ this ain't valid n\": 3, \"type\": \"name\"}]");
       assertThat(pair, Is.is(new TreeMap<Integer, String>() {{
           put(0, "externalId");
           put(1, "name");
       }}));
    }

   @Test
    public void getProperty() {
       CmdDefaultParams cmdDefaultParams = new CmdDefaultParams() {

           @Override
           public void run() {

           }
       };

       assertNull(System.getProperty("foo"));
       System.setProperty("foo", "bar");
       assertNotNull(System.getProperty("foo"));

       assertThat(cmdDefaultParams.getProperty("foo"), Is.is("bar"));
       assertNotNull(cmdDefaultParams.getProperty("nomer.nodc.url"));
    }

}