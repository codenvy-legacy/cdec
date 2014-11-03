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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.testng.Assert.assertEquals;

/**
  * @author Dmytro Nochevnov
 */
public class CdecConfigTest {
    @Test
    public void testConfigSingleNodeWithoutPuppetMaster() throws IOException {
        setupConfig(getTestConfigDirRelativePath(), "codenvy");

        CdecConfig testConfig = ConfigFactory.loadConfig(CDECArtifact.InstallType.SINGLE_NODE_WITHOUT_PUPPET_MASTER.toString());
        assertEquals(testConfig.getHost(), "127.0.0.1");
        assertEquals(testConfig.getSSHPort(), "22");
        assertEquals(testConfig.getUser(), "");
        assertEquals(testConfig.getPassword(), "");
        assertEquals(testConfig.getPrivateKeyFileAbsolutePath(), "~/.ssh/id_rsa");
        assertEquals(testConfig.getPuppetVersion(), "puppet-3.4.3-1.el6.noarch");
        assertEquals(testConfig.getPuppetResourceUrl(),
                     "http://yum.puppetlabs.com/el/6/products/x86_64/puppetlabs-release-6-7.noarch.rpm");

        // tearDown
        Files.delete(Paths.get(System.getProperty("user.home"))
                          .resolve(ConfigFactory.configFiles.get(CDECArtifact.InstallType.SINGLE_NODE_WITHOUT_PUPPET_MASTER.toString())));
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Config 'MULTI_NODES_WITH_PUPPET_MASTER' isn't supported.")
    public void testUnexistsConfig() {
        ConfigFactory.loadConfig(CDECArtifact.InstallType.MULTI_NODES_WITH_PUPPET_MASTER.toString());
    }

    @Test(expectedExceptions = ConfigException.class,
          expectedExceptionsMessageRegExp = "Config 'SINGLE_NODE_WITHOUT_PUPPET_MASTER' error: AccessDeniedException: .*../cdec")
    public void testHandlingIOException() {
        setupConfig(getTestConfigDirRelativePath() + "/../../../../../../../../../../../../../../", "codenvy");
        ConfigFactory.loadConfig(CDECArtifact.InstallType.SINGLE_NODE_WITHOUT_PUPPET_MASTER.toString());
    }

    @Test(expectedExceptions = ConfigException.class,
          expectedExceptionsMessageRegExp = "Config 'SINGLE_NODE_WITHOUT_PUPPET_MASTER' error: IOException: Default config not " +
                                            "found at 'unexists_path_to_default_config/cdec/single-node-without-puppet-master.properties'.")
    public void testHandlingDefaultConfigException() {
        setupConfig(getTestConfigDirRelativePath(), "unexists_path_to_default_config");
        ConfigFactory.loadConfig(CDECArtifact.InstallType.SINGLE_NODE_WITHOUT_PUPPET_MASTER.toString());
    }

    // use "target/config" directory to store test config file
    private String getTestConfigDirRelativePath() {
        String testConfigDirPath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath() + "../config";
        String redundantPrefix = System.getProperty("user.home") + "/";
        testConfigDirPath = testConfigDirPath.replace(redundantPrefix, "");
        return testConfigDirPath;
    }

    private void setupConfig(final String configPath, final String defaultConfigPath) {
        ConfigFactory.configFiles = new HashMap<String, Path>() {{
            put(CDECArtifact.InstallType.SINGLE_NODE_WITHOUT_PUPPET_MASTER.toString(),
                Paths.get(configPath).resolve(ConfigFactory.CDEC_CONFIG_PATH).resolve(ConfigFactory.SINGLE_NODE_WITHOUT_PUPPET_MASTER_PROPERTIES_FILE));
        }};

        ConfigFactory.defaultConfigFiles = new HashMap<String, Path>() {{
            put(CDECArtifact.InstallType.SINGLE_NODE_WITHOUT_PUPPET_MASTER.toString(),
                Paths.get(defaultConfigPath).resolve(ConfigFactory.CDEC_CONFIG_PATH).resolve(ConfigFactory.SINGLE_NODE_WITHOUT_PUPPET_MASTER_PROPERTIES_FILE));
        }};
    }
}
