package org.globalbioticinteractions.nomer.cmd;

import org.globalbioticinteractions.nomer.util.TermValidatorPredicates;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static org.globalbioticinteractions.nomer.util.TermValidatorPredicates.allFor;

@CommandLine.Command(
        name = "validate-terms",
        description = "Validate terms."
)
public class CmdValidateTerms extends CmdDefaultParams implements Runnable {

    @Override
    public void run() {
        CmdValidateLinks.validate(
                new BufferedReader(new InputStreamReader(System.in)).lines(),
                allFor(TermValidatorPredicates.TERM_VALIDATION_PREDICATES));
    }

}
