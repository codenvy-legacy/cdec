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

import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.utils.InjectorBootstrap;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static com.codenvy.im.utils.InjectorBootstrap.INJECTOR;
import static java.lang.String.format;

/**
 * @author Dmytro Nochevnov
 */
public class ConfigFactory {
    private static final String CONFIG_PATH         = InjectorBootstrap.getProperty("config.path");
    private static final String DEFAULT_CONFIG_PATH = InjectorBootstrap.getProperty("config.path.default");

    protected static final String CDEC_CONFIG_PATH                                  = "cdec";
    protected static final String SINGLE_NODE_WITHOUT_PUPPET_MASTER_PROPERTIES_FILE = "single-node-without-puppet-master.properties";

    protected static Map<String, Path> configFiles = new HashMap<String, Path>() {{
        put(CDECArtifact.InstallType.SINGLE_NODE_WITHOUT_PUPPET_MASTER.toString(),
            Paths.get(CONFIG_PATH).resolve(CDEC_CONFIG_PATH).resolve(SINGLE_NODE_WITHOUT_PUPPET_MASTER_PROPERTIES_FILE));
    }};

    protected static Map<String, Path> defaultConfigFiles = new HashMap<String, Path>() {{
        put(CDECArtifact.InstallType.SINGLE_NODE_WITHOUT_PUPPET_MASTER.toString(),
            Paths.get(DEFAULT_CONFIG_PATH).resolve(CDEC_CONFIG_PATH).resolve(SINGLE_NODE_WITHOUT_PUPPET_MASTER_PROPERTIES_FILE));
    }};

    /**
     * Config factory.
     */
    public static CdecConfig loadConfig(String configType) throws IllegalArgumentException, ConfigException {
        Path configFileRelativePath = configFiles.get(configType);
        if (configFileRelativePath == null) {
            throw new IllegalArgumentException("Config '" + configType + "' isn't supported.");
        }

        try {
            Path configFileAbsolutePath = getConfigFileAbsolutePath(configFileRelativePath, defaultConfigFiles.get(configType));
            return loadConfig(Files.newInputStream(configFileAbsolutePath), CdecConfig.class);
        } catch (Exception e) {
            throw new ConfigException(format("Config '%s' error: %s: %s",
                                             configType,
                                             e.getClass().getSimpleName(),
                                             e.getMessage()));
        }
    }

    /**
     * Return path to existed config file on file system = [fileSystemAccountRootPath]/[configFileRelativePath].
     * If config file doesn't exist, create it with default content from file [classResourceDir]/[defaultConfigFileRelativePath]
     * @throws IOException if it's impossible to create config file with default content.
     */
    private static Path getConfigFileAbsolutePath(Path configFileRelativePath, Path defaultConfigFileRelativePath) throws IOException {
        Path fileSystemAccountRootPath = Paths.get(System.getProperty("user.home"));
        Path configFileAbsolutePath = fileSystemAccountRootPath.resolve(configFileRelativePath);

        if (!Files.exists(configFileAbsolutePath)) {
            // copy default config file [classResourceDir]/[defaultConfigFileRelativePath] to [configFileAbsolutePath]
            Files.createDirectories(configFileAbsolutePath.getParent());

            InputStream defaultConfigContent =
                ConfigFactory.class.getClassLoader().getResourceAsStream(String.valueOf(defaultConfigFileRelativePath));

            if (defaultConfigContent == null) {
                throw new IOException(format("Default config not found at '%s'.", defaultConfigFileRelativePath));
            }

            Files.copy(defaultConfigContent, configFileAbsolutePath);
        }

        return configFileAbsolutePath;
    }

    private static <T extends Config> T loadConfig(InputStream configFile, Class<T> configDataClass) throws ConfigException {
        T config = INJECTOR.getInstance(configDataClass);
        config.load(configFile);
        return config;
    }
}
