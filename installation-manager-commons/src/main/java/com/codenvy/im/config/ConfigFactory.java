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
import static java.nio.file.Files.newInputStream;
import static org.apache.commons.io.IOUtils.copy;

/** @author Dmytro Nochevnov */
@Singleton
public class ConfigFactory {
    public static final String SSH_AGENT_PROPERTIES_FILE = "ssh-agent.properties";
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
    public Config loadOrCreateConfig(InstallOptions installOptions) throws IOException, IllegalArgumentException {
        Config config;

        if (installOptions.getInstallType() == null) {
            config = new DefaultConfig();

        } else if (installOptions.getInstallType() == InstallOptions.InstallType.CDEC_SINGLE_NODE) {
            // create config with properties stored in installOptions
            Map<String, String> properties = installOptions.getConfigProperties();
            if (properties == null) {
                properties = doLoadOrCreateDefaultProperties(CDEC_SINGLE_NODE_PROPERTIES_FILE);
            }
            config = new CdecConfig(properties);

        } else {
            throw new IllegalArgumentException("There is no configuration for installation type: " + installOptions.getInstallType());
        }

        validateConfig(config);
        return config;
    }

    /** Loads agent config */
    public AgentConfig loadOrCreateAgentConfig() throws IOException {
        Map<String, String> properties = doLoadOrCreateDefaultProperties(SSH_AGENT_PROPERTIES_FILE);
        AgentConfig agentConfig = new AgentConfig(properties);

        validateConfig(agentConfig);
        return agentConfig;
    }

    protected void validateConfig(Config config) {
        config.validate();
    }

    private boolean exists(String propertiesFile) {
        return Files.exists(Paths.get(configPath).resolve(propertiesFile));
    }

    /** Creates config from the template. */
    private void createConfig(String propertiesFile) throws IOException {
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

    private Map<String, String> doLoadOrCreateDefaultProperties(String propertiesFile) throws IOException {
        if (!exists(propertiesFile)) {
            createConfig(propertiesFile);
        }

        Path configFile = getConfFile(propertiesFile);
        return loadConfigProperties(configFile);
    }

    /** Write properties into the file */
    public void writeConfig(CdecConfig config) throws IOException {
        Path confFile = getConfFile(CDEC_SINGLE_NODE_PROPERTIES_FILE);
        Files.createDirectories(confFile.getParent());

        Properties properties = new Properties();
        for (Map.Entry<String, String> e : config.getProperties().entrySet()) {
            if (e.getValue() != null) {
                properties.put(e.getKey(), e.getValue());
            }
        }

        try (OutputStream out = Files.newOutputStream(confFile)) {
            properties.store(out, null);
        }
    }

    /** Loads properties from the given file. */
    public Map<String, String> loadConfigProperties(Path propertiesFile) throws ConfigException {
        Properties properties = new Properties();
        try (InputStream in = newInputStream(propertiesFile)) {
            properties.load(in);
        } catch (IOException e) {
            throw new ConfigException(format("Can't load properties: %s", e.getMessage()), e);
        }

        Map<String, String> m = new HashMap<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = entry.getKey().toString().toLowerCase();
            String value = entry.getValue().toString();

            m.put(key, value);
        }

        return m;
    }
}
