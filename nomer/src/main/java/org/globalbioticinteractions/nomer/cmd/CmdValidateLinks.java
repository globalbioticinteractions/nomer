package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameters;
import org.apache.commons.lang3.tuple.Pair;
import org.globalbioticinteractions.nomer.util.TermValidator;
import org.globalbioticinteractions.nomer.util.TermValidatorFactory;
import org.globalbioticinteractions.nomer.util.TermValidatorPredicates;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.globalbioticinteractions.nomer.util.TermValidatorPredicates.allFor;

@Parameters(separators = "= ", commandDescription = "Validate term links")
public class CmdValidateLinks extends CmdDefaultParams implements Runnable {

    static void validate(Stream<String> lines, List<Pair<Predicate<String>, String>> mapPredicates) {
        TermValidator validator = new TermValidatorFactory().createTermValidator(lines);
        validator.setPredicates(mapPredicates);
        validator.validate(System.out);
    }

    @Override
    public void run() {
        validate(
                new BufferedReader(new InputStreamReader(System.in)).lines(),
                allFor(TermValidatorPredicates.LINK_VALIDATION_PREDICATES));
    }

}
