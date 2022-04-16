package org.globalbioticinteractions.nomer.cmd;

import picocli.CommandLine;

@CommandLine.Command(
        name = "input-schema",
        description = "Show input schema in JSON."
)
public class CmdInputSchema extends CmdProperties {

    @Override
    public void run() {
        String property = getProperties().getProperty("nomer.schema.input");
        System.out.println(property);
    }
}
