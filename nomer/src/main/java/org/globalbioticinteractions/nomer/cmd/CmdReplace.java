package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameters;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.AppendingRowHandler;
import org.globalbioticinteractions.nomer.util.MatchUtil;
import org.globalbioticinteractions.nomer.util.ReplacingRowHandler;

@Parameters(separators = "= ", commandDescription = "Replace exact term matches in row. If a term has multiple matches, first match is used.")
public class CmdReplace extends CmdMatcherParams  {

    private static final Log LOG = LogFactory.getLog(CmdReplace.class);

    @Override
    public void run() {
        TermMatcher matcher = MatchUtil.getTermMatcher(getMatchers(), this);
        LOG.info("using matcher [" + matcher.getClass().getName() + "]");
        MatchUtil.match(new ReplacingRowHandler(System.out, matcher, this));
    }

}
