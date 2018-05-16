package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameters;
import org.globalbioticinteractions.nomer.util.TermValidatorPredicates;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Parameters(separators = "= ", commandDescription = "Validate terms")
public class CmdValidateTerms extends CmdDefaultParams {

    @Override
    public void run() {
        CmdValidateLinks.validate(
                new BufferedReader(new InputStreamReader(System.in)).lines(),
                TermValidatorPredicates.TERM_VALIDATION_PREDICATES);
    }

}
