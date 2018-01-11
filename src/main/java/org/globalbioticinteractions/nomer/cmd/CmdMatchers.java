package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameters;
import org.globalbioticinteractions.nomer.util.MatchUtil;
import org.globalbioticinteractions.nomer.util.TermMatcherFactory;
import org.globalbioticinteractions.nomer.util.TermMatcherRegistry;
import scala.tools.nsc.Global;

import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Parameters(separators = "= ", commandDescription = "Lists all or selected matcher configuration(s)")
public class CmdMatchers implements Runnable {

    @Override
    public void run() {
        Map<String, TermMatcherFactory> registry = TermMatcherRegistry.registry;
        for (Map.Entry<String, TermMatcherFactory> stringTermMatcherFactoryEntry : registry.entrySet()) {
            System.out.println(stringTermMatcherFactoryEntry.getKey() + ":");
            System.out.println("  " + stringTermMatcherFactoryEntry.getValue().getClass().getName());
        }
    }
}
