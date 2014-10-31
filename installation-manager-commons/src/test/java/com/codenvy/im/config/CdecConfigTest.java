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
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
  * @author Dmytro Nochevnov
 */
public class CdecConfigTest {
    CdecConfig testConfig;

    @Test
    public void testConfigSingleNodeWithoutPuppetMaster() {
        testConfig = ConfigFactory.loadConfig(CDECArtifact.InstallType.SINGLE_NODE_WITHOUT_PUPPET_MASTER);
        assertEquals(testConfig.getHost(), "127.0.0.1");
        assertEquals(testConfig.getSSHPort(), "22");
        assertEquals(testConfig.getPassword(), "");
        assertEquals(testConfig.getPrivateKeyFileAbsolutePath(), "~/.ssh/id_rsa");
        assertEquals(testConfig.getPuppetVersion(), "puppet-3.4.3-1.el6.noarch");
        assertEquals(testConfig.getPuppetResourceUrl(), "http://yum.puppetlabs.com/el/6/products/x86_64/puppetlabs-release-6-7.noarch.rpm");
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "CDEC config of type 'MULTI_NODES_WITH_PUPPET_MASTER' wasn't found.")
    public void testUnexistsConfig() {
        ConfigFactory.loadConfig(CDECArtifact.InstallType.MULTI_NODES_WITH_PUPPET_MASTER);
    }

}
