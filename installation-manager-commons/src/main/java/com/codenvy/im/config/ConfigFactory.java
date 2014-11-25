/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.im.config;

import com.codenvy.im.install.InstallOptions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static java.lang.String.format;
import static org.apache.commons.io.IOUtils.copy;

/** @author Dmytro Nochevnov */
@Singleton
public class ConfigFactory {
    public static final String CDEC_SINGLE_NODE_PROPERTIES_FILE = "cdec-single-node.properties";

    private final String configPath;

    @Inject
    public ConfigFactory(@Named("installation-manager.config.path") String configPath) {
        this.configPath = configPath;
    }

    /**
     * Config factory.
     * If config file doesn't exist, create it with default content from file [classResourceDir]/[defaultConfigFileRelativePath]
     *
     * @throws java.io.IOException
     *         in case of the I/O error
     * @throws IllegalArgumentException
     *         unknown class of {@link com.codenvy.im.install.InstallOptions}
     */
    public Config loadOrCreateDefaultConfig(InstallOptions installOptions) throws IOException, IllegalArgumentException {
        Config config;
        if (installOptions.getInstallType() == null) {
            config = new DefaultConfig();

        } else if (installOptions.getInstallType() == InstallOptions.InstallType.CDEC_SINGLE_NODE) {
            // create config with properties stored in installOptions
            if (installOptions.getConfigProperties() != null) {
                return new CdecConfig(installOptions.getConfigProperties());
            }

            // load config from file
            String propertiesFile = CDEC_SINGLE_NODE_PROPERTIES_FILE;
            if (!exists(propertiesFile)) {
                createDefaultConfig(propertiesFile);
            }

            Path configFile = getConfFile(propertiesFile);
            config = new CdecConfig(load(configFile));
        } else {
            throw new IllegalArgumentException("There is no configuration for installation type: " + installOptions.getInstallType());
        }

        validateConfig(config);
        return config;
    }

    protected void validateConfig(Config config) {
        config.validate();
    }

    private boolean exists(String propertiesFile) {
        return Files.exists(Paths.get(configPath).resolve(propertiesFile));
    }

    /** Creates config from the template. */
    private void createDefaultConfig(String propertiesFile) throws IOException {
        Path confFile = getConfFile(propertiesFile);
        Files.createDirectories(confFile.getParent());

        try (InputStream in = ConfigFactory.class.getClassLoader().getResourceAsStream(propertiesFile);
             OutputStream out = Files.newOutputStream(confFile)) {

            copy(in, out);
        }
    }

    private Path getConfFile(String propertiesFile) {
        return Paths.get(configPath).resolve(propertiesFile);
    }

    public static Map<String, String> load(Path propertiesFile) throws ConfigException {
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(propertiesFile)) {
            properties.load(in);
        } catch (IOException e) {
            throw new ConfigException(format("Can't load properties: %s", e.getMessage()), e);
        }

        Map<String, String> map = new HashMap<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = entry.getKey().toString().toLowerCase();
            String value = (String)entry.getValue();

            map.put(key, value);
        }

        return map;
    }
}
