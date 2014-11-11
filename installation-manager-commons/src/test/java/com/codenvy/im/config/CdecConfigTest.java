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
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static java.lang.String.format;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
  * @author Dmytro Nochevnov
 */
public class CdecConfigTest {
    @Test
    public void testConfigSingleNodeWithPuppetMaster() throws IOException {
        setupConfig(getTestConfigDirAbsolutePath(), "codenvy");

        try {
            ConfigFactory.loadConfig(Installer.Type.CDEC_SINGLE_NODE_WITH_PUPPET_MASTER.toString());
            fail("ConfigException should be thrown.");
        } catch(ConfigException e) {
            assertEquals(e.getMessage(),
                         format("Please complete install config file '%s'.",
                                ConfigFactory.configFilesAbsolutePaths.get(Installer.Type.CDEC_SINGLE_NODE_WITH_PUPPET_MASTER.toString())));
        }

        CdecConfig testConfig = ConfigFactory.loadConfig(Installer.Type.CDEC_SINGLE_NODE_WITH_PUPPET_MASTER.toString());
        assertEquals(testConfig.getHost(), "");
        assertEquals(testConfig.getSSHPort(), "");
        assertEquals(testConfig.getUser(), "");
        assertEquals(testConfig.getPassword(), "");
        assertEquals(testConfig.getPrivateKeyFileAbsolutePath(), "");
        assertEquals(testConfig.getPuppetVersion(), "puppet-3.4.3-1.el6.noarch");
        assertEquals(testConfig.getPuppetResourceUrl(),
                     "http://yum.puppetlabs.com/el/6/products/x86_64/puppetlabs-release-6-7.noarch.rpm");

        // tearDown
        Files.delete(Paths.get(System.getProperty("user.home"))
                          .resolve(ConfigFactory.configFilesAbsolutePaths.get(Installer.Type.CDEC_SINGLE_NODE_WITH_PUPPET_MASTER.toString())));
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Config 'CDEC_MULTI_NODES_WITH_PUPPET_MASTER' isn't supported.")
    public void testUnexistsConfig() {
        ConfigFactory.loadConfig(Installer.Type.CDEC_MULTI_NODES_WITH_PUPPET_MASTER.toString());
    }

    @Test(expectedExceptions = ConfigException.class,
          expectedExceptionsMessageRegExp = "Config 'CDEC_SINGLE_NODE_WITH_PUPPET_MASTER' error: AccessDeniedException: " +
                                            ".*../cdec-single-node-with-puppet-master.properties")
    public void testHandlingIOException() {
        setupConfig(getTestConfigDirAbsolutePath() + "/../../../../../../../../../../../../../../", "codenvy");
        ConfigFactory.loadConfig(Installer.Type.CDEC_SINGLE_NODE_WITH_PUPPET_MASTER.toString());
    }

    @Test(expectedExceptions = ConfigException.class,
          expectedExceptionsMessageRegExp = "Config 'CDEC_SINGLE_NODE_WITH_PUPPET_MASTER' error: IOException: Default config not " +
                                            "found at 'unexists_path_to_default_config/cdec-single-node-with-puppet-master.properties'.")
    public void testHandlingDefaultConfigException() {
        setupConfig(getTestConfigDirAbsolutePath(), "unexists_path_to_default_config");
        ConfigFactory.loadConfig(Installer.Type.CDEC_SINGLE_NODE_WITH_PUPPET_MASTER.toString());
    }

    // use "target/config" directory to store test config file
    private String getTestConfigDirAbsolutePath() {
        return getClass().getProtectionDomain().getCodeSource().getLocation().getPath() + "../config";
    }

    private void setupConfig(final String configPath, final String defaultConfigPath) {
        ConfigFactory.configFilesAbsolutePaths = new HashMap<String, Path>() {{
            put(Installer.Type.CDEC_SINGLE_NODE_WITH_PUPPET_MASTER.toString(),
                Paths.get(configPath).resolve(ConfigFactory.CDEC_SINGLE_NODE_WITH_PUPPET_MASTER_PROPERTIES_FILE));
        }};

        ConfigFactory.defaultConfigFilesRelativePaths = new HashMap<String, Path>() {{
            put(Installer.Type.CDEC_SINGLE_NODE_WITH_PUPPET_MASTER.toString(),
                Paths.get(defaultConfigPath).resolve(ConfigFactory.CDEC_SINGLE_NODE_WITH_PUPPET_MASTER_PROPERTIES_FILE));
        }};
    }
}
