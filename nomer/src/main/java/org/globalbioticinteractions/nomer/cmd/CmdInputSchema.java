package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameters;

import java.util.Properties;

@Parameters(separators = "= ", commandDescription = "Show input schema in JSON.")
public class CmdInputSchema extends CmdProperties {

    @Override
    public void run() {
        String property = getProperties().getProperty("nomer.schema.input in JSON.");
        System.out.println(property);
    }
}
