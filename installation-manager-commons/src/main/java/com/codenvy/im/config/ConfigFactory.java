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

import com.codenvy.im.installer.Installer;
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

    protected static final String CDEC_SINGLE_NODE_WITH_PUPPET_MASTER_PROPERTIES_FILE = "cdec-single-node-with-puppet-master.properties";

    protected static Map<String, Path> configFilesAbsolutePaths = new HashMap<String, Path>() {{
        put(Installer.Type.CDEC_SINGLE_NODE_WITH_PUPPET_MASTER.toString(),
            getConfigFileAbsolutePath(CDEC_SINGLE_NODE_WITH_PUPPET_MASTER_PROPERTIES_FILE));
    }};

    protected static Map<String, Path> defaultConfigFilesRelativePaths = new HashMap<String, Path>() {{
        put(Installer.Type.CDEC_SINGLE_NODE_WITH_PUPPET_MASTER.toString(),
            getDefaultConfigFileRelativePath(CDEC_SINGLE_NODE_WITH_PUPPET_MASTER_PROPERTIES_FILE));
    }};

    /**
     * This is utility class.
     */
    private ConfigFactory() {
    }

    /**
     * Config factory.
     * If config file doesn't exist, create it with default content from file [classResourceDir]/[defaultConfigFileRelativePath]
     * @throws ConfigException if it's impossible to create config file with default content.
     * @throws IllegalArgumentException if config of configType isn't supported.
     */
    public static CdecConfig loadConfig(String configType) throws IllegalArgumentException, ConfigException {
        Path configFileAbsolutePath = configFilesAbsolutePaths.get(configType);
        if (configFileAbsolutePath == null) {
            throw new IllegalArgumentException("Config '" + configType + "' isn't supported.");
        }

        try {
            if (!Files.exists(configFileAbsolutePath)) {
                Path defaultConfigFilesRelativePath = defaultConfigFilesRelativePaths.get(configType);
                createConfigFile(defaultConfigFilesRelativePath, configFileAbsolutePath);
                throw new ConfigException(format("Please complete install config file '%s'.", configFileAbsolutePath));
            }

            return loadConfig(Files.newInputStream(configFileAbsolutePath), CdecConfig.class, configFileAbsolutePath.toString());
        } catch (IOException e) {
            throw new ConfigException(format("Config '%s' error: %s: %s",
                                             configType,
                                             e.getClass().getSimpleName(),
                                             e.getMessage()));
        }
    }

    /**
     * Load content of default config file [classResourceDir]/[defaultConfigFileRelativePath] into file [configFileAbsolutePath]
     */
    private static void createConfigFile(Path defaultConfigFilesRelativePath, Path configFileAbsolutePath) throws
                                                                                                           ConfigException,
                                                                                                           IOException {
        InputStream defaultConfigContent =
            ConfigFactory.class.getClassLoader().getResourceAsStream(String.valueOf(defaultConfigFilesRelativePath));

        if (defaultConfigContent == null) {
            throw new IOException(format("Default config not found at '%s'.", defaultConfigFilesRelativePath));
        }

        Config config = new Config() {
        };
        config.load(defaultConfigContent, defaultConfigFilesRelativePath.toString());

        Files.createDirectories(configFileAbsolutePath.getParent());
        config.store(Files.newOutputStream(configFileAbsolutePath));
    }

    /**
     * Return path to existed config file on file system = [fileSystemAccountRootPath]/[configFileRelativePath].
     */
    private static Path getConfigFileAbsolutePath(String configFileName) {
        Path configFileRelativePath = Paths.get(CONFIG_PATH).resolve(configFileName);
        Path fileSystemAccountRootPath = Paths.get(System.getProperty("user.home"));
        return fileSystemAccountRootPath.resolve(configFileRelativePath);
    }

    /**
     * Return relative path to default config file on file system = [classResourceDir]/[defaultConfigFileRelativePath].
     */
    private static Path getDefaultConfigFileRelativePath(String configFileName) {
        return Paths.get(DEFAULT_CONFIG_PATH).resolve(configFileName);
    }


    private static <T extends Config> T loadConfig(InputStream configFile, Class<T> configDataClass, String configSource) throws ConfigException {
        T config = INJECTOR.getInstance(configDataClass);
        config.load(configFile, configSource);
        return config;
    }
}
