package org.globalbioticinteractions.nomer.util;

import org.apache.commons.lang3.tuple.Pair;

import java.io.PrintStream;
import java.util.List;
import java.util.function.Predicate;

public interface TermValidator {
    void validate(PrintStream out);

    void setPredicates(List<Pair<Predicate<String>,String>> predicates);

    List<Pair<Predicate<String>,String>> getPredicates();
}
