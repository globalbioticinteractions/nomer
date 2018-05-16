package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameters;
import scala.tools.nsc.Global;

import java.util.Properties;

@Parameters(separators = "= ", commandDescription = "Lists properties.")
public class CmdProperties extends CmdDefaultParams implements Runnable {

    @Override
    public void run() {
        Properties properties = getProperties();
        properties.stringPropertyNames().stream()
                .filter(key -> key.startsWith("nomer."))
                .sorted()
                .map(key -> key + "=" + properties.getProperty(key))
                .forEach(System.out::println);
    }
}
