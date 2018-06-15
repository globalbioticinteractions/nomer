package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameters;
import scala.tools.nsc.Global;

import java.util.Properties;

@Parameters(separators = "= ", commandDescription = "Lists configuration properties. Can be used to make a local copy and override default settings using the [--properties=[local copy]] option.")
public class CmdProperties extends CmdDefaultParams implements Runnable {

    @Override
    public void run() {
        printOnlyWithPrefix("nomer.");
    }

    protected void printOnlyWithPrefix(String prefixFilter) {
        Properties properties = getProperties();
        properties.stringPropertyNames().stream()
                .filter(key -> key.startsWith(prefixFilter))
                .map(key -> key + "=" + properties.getProperty(key))
                .sorted()
                .forEach(System.out::println);
    }
}
