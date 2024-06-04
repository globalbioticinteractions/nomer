package org.globalbioticinteractions.nomer.cmd;

import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.match.MatchUtil;
import org.globalbioticinteractions.nomer.util.ReplacingRowHandler;
import picocli.CommandLine;

@CommandLine.Command(
        name = "replace",
        description = "Replace exact term matches in row from stdin. " +
                "The input schema is used to select the id and/or name to match to. " +
                "The output schema is used to select the columns to write into. " +
                "If a term has multiple matches, first match is used." +
                "%nFor example:%n"+ "echo -e '\\tHomo sapiens' | nomer replace col" +
                "%nhas expected result:%n"+ "COL:6MB3T\tHomo sapiens"
)
public class CmdReplace extends CmdMatcherParams  {

    @Override
    public void run() {
        TermMatcher matcher = MatchUtil.getTermMatcher(getMatchers(), this);
        MatchUtil.match(new ReplacingRowHandler(System.out, matcher, this));
    }

}
