package org.globalbioticinteractions.nomer.cmd;

import picocli.CommandLine;

import static org.globalbioticinteractions.nomer.cmd.CmdMatcherParams.NOMER_SCHEMA_OUTPUT;

@CommandLine.Command(name = "output-schema", description = "Show output schema in JSON.")
public class CmdOutputSchema extends CmdProperties {

    @Override
    public void run() {
        System.out.println(getProperties().getProperty(NOMER_SCHEMA_OUTPUT));
    }
}
