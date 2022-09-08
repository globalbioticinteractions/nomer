package org.globalbioticinteractions.nomer.cmd;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.ResourceServiceFactoryLocal;
import org.eol.globi.util.ResourceUtil;
import org.globalbioticinteractions.nomer.util.PropertyContext;
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

public abstract class CmdDefaultParams implements PropertyContext {
    public static final String PROPERTIES_DEFAULT = "classpath:/org/globalbioticinteractions/nomer/default.properties";
    private Properties properties = null;

    @CommandLine.Option(
            names = {"-p", "--properties"},
            description = "Path to properties file to override defaults.")
    private String propertiesResource = "";


    @Override
    public String getProperty(String key) {
        String value = System.getProperty(key, getProperties().getProperty(key));
        return StringUtils.trim(value);
    }

    Properties getProperties() {
        return properties == null ? initProperties() : properties;
    }

    private Properties initProperties() {
        Properties props = new Properties();
        try {
            URI resource = URI.create(PROPERTIES_DEFAULT);
            ResourceService serviceLocal = new ResourceServiceFactoryLocal(is -> is).serviceForResource(resource);
            props.load(serviceLocal.retrieve(resource));
            props = new Properties(props);
            if (StringUtils.isNotBlank(getPropertiesResource())) {
                URI propertiesResource = URI.create(getPropertiesResource());
                try (InputStream inStream = serviceLocal.retrieve(propertiesResource)) {
                    props.load(inStream);
                }
            }
            properties = props;
        } catch (IOException e) {
            throw new RuntimeException("failed to load properties from [" + getPropertiesResource() + "]", e);
        }

        return props;
    }

    public String getPropertiesResource() {
        return StringUtils.trim(propertiesResource);
    }
}
