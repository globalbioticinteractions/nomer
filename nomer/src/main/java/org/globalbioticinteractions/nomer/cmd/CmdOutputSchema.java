package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameters;

@Parameters(separators = "= ", commandDescription = "Show output schema.")
public class CmdOutputSchema extends CmdProperties {

    @Override
    public void run() {
        System.out.println(getProperties().getProperty("nomer.schema.output"));
    }
}
