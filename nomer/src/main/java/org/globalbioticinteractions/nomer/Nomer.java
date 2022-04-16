package org.globalbioticinteractions.nomer;

import org.globalbioticinteractions.nomer.cmd.CmdVersion;
import picocli.CommandLine;
import picocli.codegen.docgen.manpage.ManPageGenerator;

import static java.lang.System.exit;

@CommandLine.Command(name = "nomer",
        description = "nomer - maps identifiers and names to other identifiers and names",
        versionProvider = Nomer.class,
        subcommands = {

                ManPageGenerator.class,
                CommandLine.HelpCommand.class
        },
        mixinStandardHelpOptions = true)

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
