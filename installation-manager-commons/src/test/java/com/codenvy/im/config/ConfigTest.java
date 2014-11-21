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

import com.codenvy.im.install.CdecInstallOptions;
import com.codenvy.im.install.DefaultOptions;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.apache.commons.io.FileUtils.write;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Dmytro Nochevnov
 */
public class ConfigTest {

    private ConfigFactory configFactory;
    private Path          configPath;


    @BeforeMethod
    public void setUp() throws Exception {
        configPath = Paths.get("target", "config");
        FileUtils.deleteDirectory(configPath.toFile());

        configFactory = new ConfigFactory(configPath.toString());
    }

    @Test
    public void testDefaultConfig() throws Exception {
        Config config = configFactory.loadOrCreateDefaultConfig(new DefaultOptions());
        assertTrue(config instanceof DefaultConfig);
    }

    @Test
    public void testCdecConfigSingleNode() throws Exception {
        CdecInstallOptions installOptions = new CdecInstallOptions();
        installOptions.setCdecInstallType(CdecInstallOptions.CDECInstallType.SINGLE_NODE);

        write(configPath.resolve(ConfigFactory.CDEC_SINGLE_NODE_PROPERTIES_FILE).toFile(), "host=172.0.0.1\nuser=anonym\npassword=secret\n");

        Config config = configFactory.loadOrCreateDefaultConfig(installOptions);
        assertTrue(config instanceof CdecConfig);

        CdecConfig cdecConfig = (CdecConfig)config;
        assertEquals(cdecConfig.getHost(), "172.0.0.1");
        assertEquals(cdecConfig.getUser(), "anonym");
        assertEquals(cdecConfig.getPassword(), "secret");
        assertNull(cdecConfig.getPuppetMasterPort());
        assertNull(cdecConfig.getPrivateKeyFileAbsolutePath());
        assertNull(cdecConfig.getPuppetResourceUrl());
        assertNull(cdecConfig.getPuppetVersion());
        assertNull(cdecConfig.getSSHPort());
    }
}
