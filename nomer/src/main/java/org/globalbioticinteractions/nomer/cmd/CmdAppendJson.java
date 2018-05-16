package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameters;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.taxon.TermMatcher;
import org.globalbioticinteractions.nomer.util.MatchUtil;
import org.globalbioticinteractions.nomer.util.TermMatchingRowJsonHandler;

@Parameters(separators = "= ", commandDescription = "embeds term matches into json")
public class CmdAppendJson extends CmdMatcherParams {
    private final static Log LOG = LogFactory.getLog(CmdAppendJson.class);

    @Override
    public void run() {
        TermMatcher matcher = MatchUtil.getTermMatcher(getMatchers(), this);
        LOG.info("using matcher [" + matcher.getClass().getName() + "]");
        MatchUtil.match(new TermMatchingRowJsonHandler(System.out, matcher, this));
    }

}
