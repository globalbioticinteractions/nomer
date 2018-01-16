package org.globalbioticinteractions.nomer.cmd;

import com.beust.jcommander.Parameters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.Properties;
import java.util.StringJoiner;

@Parameters(separators = "= ", commandDescription = "Lists properties.")
public class CmdProperties extends CmdDefaultParams {
    private final static Log LOG = LogFactory.getLog(CmdProperties.class);

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
