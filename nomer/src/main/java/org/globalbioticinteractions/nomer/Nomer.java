package org.globalbioticinteractions.nomer;

import org.globalbioticinteractions.nomer.cmd.CmdAppend;
import org.globalbioticinteractions.nomer.cmd.CmdClean;
import org.globalbioticinteractions.nomer.cmd.CmdDump;
import org.globalbioticinteractions.nomer.cmd.CmdInputSchema;
import org.globalbioticinteractions.nomer.cmd.CmdMatchers;
import org.globalbioticinteractions.nomer.cmd.CmdOutputSchema;
import org.globalbioticinteractions.nomer.cmd.CmdProperties;
import org.globalbioticinteractions.nomer.cmd.CmdReplace;
import org.globalbioticinteractions.nomer.cmd.CmdValidateLinks;
import org.globalbioticinteractions.nomer.cmd.CmdValidateTerms;
import org.globalbioticinteractions.nomer.cmd.CmdVersion;
import picocli.CommandLine;
import picocli.codegen.docgen.manpage.ManPageGenerator;

import static java.lang.System.exit;

@CommandLine.Command(name = "nomer",
        description = "nomer - maps identifiers and names to other identifiers and names",
        versionProvider = Nomer.class,
        mixinStandardHelpOptions = true,
        subcommands = {
                CmdVersion.class,
                CmdReplace.class,
                CmdAppend.class,
                CmdDump.class,
                CmdMatchers.class,
                CmdProperties.class,
                CmdInputSchema.class,
                CmdOutputSchema.class,
                CmdValidateTerms.class,
                CmdValidateLinks.class,
                CmdClean.class,
                ManPageGenerator.class,
                CommandLine.HelpCommand.class
        }
)

public class Nomer implements CommandLine.IVersionProvider {
    public static void main(String[] args) {
        try {
            int exitCode = run(args);
            System.exit(exitCode);
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            exit(1);
        }
    }


    public static int run(String[] args) {
        CommandLine commandLine = new CommandLine(new Nomer());
        return commandLine.execute(args);
    }

    public static String getVersionString() {
        return CmdVersion.class.getPackage().getImplementationVersion();
    }

    @Override
    public String[] getVersion() throws Exception {
        return new String[]{getVersionString()};
    }
}
