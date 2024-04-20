package org.globalbioticinteractions.nomer.cmd;

import picocli.CommandLine;

import static org.globalbioticinteractions.nomer.cmd.CmdMatcherParams.NOMER_SCHEMA_INPUT;

@CommandLine.Command(
        name = "input-schema",
        description = "Show input schema in JSON."
)
public class CmdInputSchema extends CmdProperties {

    @Override
    public void run() {
        String property = getProperties().getProperty(NOMER_SCHEMA_INPUT);
        System.out.println(property);
    }
}
