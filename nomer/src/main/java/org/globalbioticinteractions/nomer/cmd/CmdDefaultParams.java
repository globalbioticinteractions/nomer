package org.globalbioticinteractions.nomer.cmd;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.InputStreamFactory;
import org.eol.globi.util.ResourceServiceClasspathResource;
import org.eol.globi.util.ResourceServiceFactory;
import org.eol.globi.util.ResourceServiceGzipAware;
import org.eol.globi.util.ResourceServiceLocalFile;
import org.eol.globi.util.ResourceServiceLocalJarResource;
import org.eol.globi.util.ResourceUtil;
import org.globalbioticinteractions.nomer.util.PropertyContext;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
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

            ResourceServiceFactory serviceFactory = new ResourceServiceFactory() {
                private final InputStreamFactory factory = is -> is;


                @Override
                public ResourceService serviceForResource(URI uri) {
                    ResourceService resourceService;

                    if (ResourceUtil.isFileURI(uri)) {
                        resourceService = new ResourceServiceLocalFile(factory);
                    } else if (org.apache.commons.lang3.StringUtils.startsWith(resource.toString(), "jar:file:/")) {
                        resourceService = new ResourceServiceLocalJarResource(factory);
                    } else if (StringUtils.equals(uri.getScheme(), "classpath")) {
                        resourceService = new ResourceServiceClasspathResource(factory, CmdDefaultParams.class);
                    } else {
                        resourceService = new ResourceServiceLocalFile(factory) {
                            @Override
                            public InputStream retrieve(URI uri) throws IOException {
                                try {
                                    return super.retrieve(new URI("file://" + uri.toString()));
                                } catch (URISyntaxException e) {
                                    throw new IOException("failed to handle [" + uri.toString() + "]", e);
                                }
                            }
                        };
                    }

                    return new ResourceServiceGzipAware(resourceService);
                }
            };

            ResourceService serviceLocal =
                    serviceFactory
                    .serviceForResource(resource);
            props.load(serviceLocal.retrieve(resource));
            props = new Properties(props);
            if (StringUtils.isNotBlank(getPropertiesResource())) {
                URI propertiesResource = URI.create(getPropertiesResource());
                try (InputStream inStream = serviceFactory.serviceForResource(propertiesResource).retrieve(propertiesResource)) {
                    props.load(inStream);
                }
            }
            properties = props;
        } catch (IOException e) {
            throw new RuntimeException("failed to load properties from [" + getPropertiesResource() + "]", e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("failed to load properties: likely invalid properties URI [" + getPropertiesResource() + "]", e);
        }
        return props;
    }

    public void setPropertiesResource(String propertiesResource) {
        this.propertiesResource = propertiesResource;
    }

    public String getPropertiesResource() {
        return StringUtils.trim(propertiesResource);
    }
}
