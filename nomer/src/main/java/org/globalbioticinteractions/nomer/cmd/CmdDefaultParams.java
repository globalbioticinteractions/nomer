package org.globalbioticinteractions.nomer.cmd;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.InputStreamFactory;
import org.eol.globi.util.ResourceServiceClasspathOrDataDirResource;
import org.eol.globi.util.ResourceServiceFactory;
import org.eol.globi.util.ResourceServiceGzipAware;
import org.eol.globi.util.ResourceServiceLocalFile;
import org.eol.globi.util.ResourceServiceLocalJarResource;
import org.eol.globi.util.ResourceUtil;
import org.globalbioticinteractions.nomer.util.PropertyContext;
import picocli.CommandLine;

import java.io.File;
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

    private String workDir = null;


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
        String propertiesOverride = getPropertiesResource();
        try {
            URI propertiesDefault = URI.create(PROPERTIES_DEFAULT);

            ResourceServiceFactory propertyFactory = new PropertyFactory(propertiesDefault, getWorkDir());

            ResourceService propertiesLocal =
                    propertyFactory
                            .serviceForResource(propertiesDefault);
            props.load(propertiesLocal.retrieve(propertiesDefault));
            props = new Properties(props);

            if (StringUtils.isNotBlank(propertiesOverride)) {
                URI propertiesResource = URI.create(propertiesOverride);
                try (InputStream inStream = propertyFactory
                        .serviceForResource(propertiesResource)
                        .retrieve(propertiesResource)) {
                    props.load(inStream);
                }
            }
            properties = props;
        } catch (IOException e) {
            throw new RuntimeException("failed to load properties from [" + propertiesOverride + "]: resource not found", e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("failed to load properties: please make sure that [" + propertiesOverride + "] is a valid URI", e);
        }
        return props;
    }

    public void setPropertiesResource(String propertiesResource) {
        this.propertiesResource = propertiesResource;
    }

    public String getPropertiesResource() {
        return StringUtils.trim(propertiesResource);
    }

    public String getWorkDir() {
        return StringUtils.isBlank(workDir)
                ? System.getProperty("user.dir")
                : workDir;
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }


    private static class PropertyFactory implements ResourceServiceFactory {
        private final InputStreamFactory factory;
        private final URI resource;
        private final String workDir;

        public PropertyFactory(URI resource, String workDir) {
            this.resource = resource;
            this.workDir = workDir;
            factory = is -> is;
        }


        @Override
        public ResourceService serviceForResource(URI uri) {
            ResourceService resourceService;

            if (ResourceUtil.isFileURI(uri)) {
                resourceService = new ResourceServiceLocalFile(factory);
            } else if (org.apache.commons.lang3.StringUtils.startsWith(resource.toString(), "jar:file:/")) {
                resourceService = new ResourceServiceLocalJarResource(factory);
            } else if (StringUtils.equals(uri.getScheme(), "classpath")) {
                resourceService = new ResourceServiceClasspathOrDataDirResource(factory, CmdDefaultParams.class, workDir);
            } else {
                resourceService = new ResourceServiceLocalFile(factory) {
                    @Override
                    public InputStream retrieve(URI uri) throws IOException {
                        try {
                            return super.retrieve(new URI("file://" + uri.toString()));
                        } catch (IllegalArgumentException | URISyntaxException e) {
                            // try to resolve relative path
                            File file = new File(workDir, uri.toString());
                            if (!file.exists()) {
                                throw new IOException("failed to find local property file [" + uri.toString() + "]: resource not found");
                            }
                            return super.retrieve(file.toURI());
                        }
                    }
                };
            }

            return new ResourceServiceGzipAware(resourceService);
        }
    }
}
