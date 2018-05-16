package org.globalbioticinteractions.nomer.util;

import org.apache.commons.lang3.tuple.Pair;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class TermValidatorFactory {

    public TermValidator createTermValidator(final Stream<String> lines) {
        return new TermValidator() {
            public List<Pair<Predicate<String>, String>> predicates = null;

            @Override
            public void validate(PrintStream out) {
                lines.flatMap(line -> {
                    Stream<Pair<Predicate<String>, String>> predicates = getPredicates().stream();
                    return predicates.map(p -> {
                        String okOrFail = p.getLeft().test(line) ? "OK" : "FAIL";
                        return String.format("%s\t%s\t%s", okOrFail, p.getRight(), line);
                    });
                }).forEach(out::println);
            }

            @Override
            public void setPredicates(List<Pair<Predicate<String>, String>> predicates) {
                this.predicates = predicates;
            }

            @Override
            public List<Pair<Predicate<String>, String>> getPredicates() {
                return predicates == null ? Collections.emptyList() : predicates;
            }

        };
    }

}
