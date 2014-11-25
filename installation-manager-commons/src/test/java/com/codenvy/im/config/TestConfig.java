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

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.apache.commons.io.FileUtils.write;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
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
        Config config = configFactory.loadOrCreateDefaultConfig(new InstallOptions());
        assertTrue(config instanceof DefaultConfig);
    }

    @Test
    public void testLoadCdecConfigSingleNode() throws Exception {
        doNothing().when(configFactory).validateConfig(any(Config.class));

        InstallOptions installOptions = new InstallOptions();
        installOptions.setInstallType(InstallOptions.InstallType.CDEC_SINGLE_NODE);

        write(configPath.resolve(ConfigFactory.CDEC_SINGLE_NODE_PROPERTIES_FILE).toFile(), "host=172.0.0.1\nuser=anonym\npassword=secret\n");

        Config config = configFactory.loadOrCreateDefaultConfig(installOptions);
        assertTrue(config instanceof CdecConfig);

        CdecConfig cdecConfig = (CdecConfig)config;
        assertNull(cdecConfig.getPuppetResourceUrl());
        assertNull(cdecConfig.getPuppetVersion());
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testLoadConfigErrorValidateFailed() throws Exception {
        InstallOptions installOptions = new InstallOptions();
        installOptions.setInstallType(InstallOptions.InstallType.CDEC_SINGLE_NODE);

        write(configPath.resolve(ConfigFactory.CDEC_SINGLE_NODE_PROPERTIES_FILE).toFile(), "host=172.0.0.1\nuser=anonym\npassword=secret\n");

        configFactory.loadOrCreateDefaultConfig(installOptions);
    }
}
