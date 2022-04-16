package org.globalbioticinteractions.nomer.cmd;

import picocli.CommandLine;

import java.util.Properties;

@CommandLine.Command(
        name = "properties",
        description = "Lists configuration properties. Can be used to make a local copy and override default settings using the [--properties=[local copy]] option."
)
public class CmdProperties extends CmdDefaultParams implements Runnable {

    @Override
    public void run() {
        printOnlyWithPrefix("nomer.");
    }

    private void printOnlyWithPrefix(String prefixFilter) {
        Properties properties = getProperties();
        properties.stringPropertyNames().stream()
                .filter(key -> key.startsWith(prefixFilter))
                .map(key -> key + "=" + properties.getProperty(key))
                .sorted()
                .forEach(System.out::println);
    }
}
