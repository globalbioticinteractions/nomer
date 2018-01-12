package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.JCommander;
import org.junit.Test;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

public class CmdUsageTest {

    @Test
    public void usage() {
        JCommander jc = new CmdLine().buildCommander();
        StringBuilder usageString = new StringBuilder();
        jc.usage(usageString);
        System.out.println(usageString.toString());
        assertThat(usageString.toString(), containsString("Usage"));
    }


}