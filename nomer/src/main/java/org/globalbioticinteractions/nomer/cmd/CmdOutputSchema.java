package org.globalbioticinteractions.nomer.cmd;

import picocli.CommandLine;

@CommandLine.Command(name = "output-schema", description = "Show output schema in JSON.")
public class CmdOutputSchema extends CmdProperties {

    @Override
    public void run() {
        System.out.println(getProperties().getProperty("nomer.schema.output"));
    }
}
