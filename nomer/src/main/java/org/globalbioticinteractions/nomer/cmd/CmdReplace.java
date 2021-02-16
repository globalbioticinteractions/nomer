package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.AppendingRowHandler;
import org.globalbioticinteractions.nomer.util.MatchUtil;
import org.globalbioticinteractions.nomer.util.ReplacingRowHandler;

@Parameters(separators = "= ", commandDescription = "Replace exact term matches in row. The input schema is used to select the id and/or name to match to. The output schema is used to select the columns to write into. If a term has multiple matches, first match is used.")
public class CmdReplace extends CmdMatcherParams  {

    @Override
    public void run() {
        TermMatcher matcher = MatchUtil.getTermMatcher(getMatchers(), this);
        MatchUtil.match(new ReplacingRowHandler(System.out, matcher, this));
    }

}
