package org.globalbioticinteractions.nomer.util;

import org.eol.globi.domain.Taxon;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.RowHandler;
import org.eol.globi.taxon.TermMatcher;
import org.eol.globi.util.CSVTSVUtil;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Appender  {

    void appendLinesForRow(String[] row, Stream<Taxon> resolvedTaxa, PrintStream p, NameTypeOf nameTypeOf);

}
