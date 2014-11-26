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

import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.apache.commons.io.FileUtils.write;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Dmytro Nochevnov
 */
public class TestConfig {

    private ConfigFactory configFactory;
    private Path          configPath;


    @BeforeMethod
    public void setUp() throws Exception {
        configPath = Paths.get("target", "config");
        FileUtils.deleteDirectory(configPath.toFile());

        configFactory = spy(new ConfigFactory(configPath.toString()));
    }

    @Test
    public void testDefaultConfig() throws Exception {
        Config config = configFactory.loadOrCreateConfig(new InstallOptions());
        assertTrue(config instanceof DefaultConfig);
    }

    @Test
    public void testLoadCdecConfigSingleNode() throws Exception {
        doNothing().when(configFactory).validateConfig(any(Config.class));

        InstallOptions installOptions = new InstallOptions();
        installOptions.setInstallType(InstallOptions.InstallType.CDEC_SINGLE_NODE);

        write(configPath.resolve(ConfigFactory.CDEC_SINGLE_NODE_PROPERTIES_FILE).toFile(), "host=172.0.0.1\nuser=anonym\npassword=secret\n");

        Config config = configFactory.loadOrCreateConfig(installOptions);
        assertTrue(config instanceof CdecConfig);

        CdecConfig cdecConfig = (CdecConfig)config;
        assertEquals(cdecConfig.getPuppetResourceUrl(), CdecConfig.Property.PUPPET_RESOURCE_URL.getDefaultValue());
        assertEquals(cdecConfig.getPuppetVersion(), CdecConfig.Property.PUPPET_VERSION.getDefaultValue());
        assertNull(cdecConfig.getDnsName());
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testLoadConfigErrorValidateFailed() throws Exception {
        InstallOptions installOptions = new InstallOptions();
        installOptions.setInstallType(InstallOptions.InstallType.CDEC_SINGLE_NODE);

        write(configPath.resolve(ConfigFactory.CDEC_SINGLE_NODE_PROPERTIES_FILE).toFile(), "host=172.0.0.1\nuser=anonym\npassword=secret\n");

        configFactory.loadOrCreateConfig(installOptions);
    }

    @Test
    public void testWriteConfig() throws Exception {
        CdecConfig cdecConfig = new CdecConfig(new HashMap<String, String>() {{
            put("mongo_admin_password", "mongoPassword");
            put("mongo_user_password", "mongoUserPassword");
            put("mongo_orgservice_user_password", "mongoOrgServiceUserPassword");
        }});

        configFactory.writeConfig(cdecConfig);
        String result = FileUtils.readFileToString(new File("target/config/cdec-single-node.properties"));
        assertTrue(result.endsWith("mongo_admin_password=mongoPassword\n" +
                                   "mongo_user_password=mongoUserPassword\n" +
                                   "puppet_version=puppet-3.4.3-1.el6.noarch\n" +
                                   "mongo_orgservice_user_password=mongoOrgServiceUserPassword\n" +
                                   "puppet_resource_url=http\\://yum.puppetlabs.com/el/6/products/x86_64/puppetlabs-release-6-7.noarch.rpm\n"));
    }
}
