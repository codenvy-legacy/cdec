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
package com.codenvy.im.service;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;

import static com.codenvy.im.service.InstallationManagerConfig.CONFIG_FILE;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.mockito.Mockito.doReturn;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/** @author Dmytro Nochevnov */
public class TestInstallationManagerConfig {
    @BeforeMethod
    public void setupConfigFile() throws IOException {
        CONFIG_FILE = Paths.get(this.getClass().getClassLoader().getResource("im.properties").getPath());
        FileUtils.writeStringToFile(CONFIG_FILE.toFile(), "cdec.host.dns=localhost");
    }

    @Test
    public void testFields() throws Exception {
        InstallationManagerConfig config = new InstallationManagerConfig();
        config.setDownloadDir("dir");
        assertEquals(config.getDownloadDir(), "dir");

        config.setProxyUrl("localhost");
        assertEquals(config.getProxyUrl(), "localhost");

        config.setProxyPort("1234");
        assertEquals(config.getProxyPort(), "1234");
    }

    @Test
    public void testCheckEmptyConfig() throws Exception {
        InstallationManagerConfig config = new InstallationManagerConfig();
        assertTrue(config.checkEmptyConfig());

        config.setDownloadDir("");
        assertTrue(config.checkEmptyConfig());

        config.setProxyUrl("localhost");
        assertFalse(config.checkEmptyConfig());
    }

    @Test
    public void testReadStoreProperty() throws Exception {
        Properties result = InstallationManagerConfig.readProperties(CONFIG_FILE);
        assertEquals(result.entrySet().toString(), "[cdec.host.dns=localhost]");

        InstallationManagerConfig.storeProperty("hello", "value");
        result = InstallationManagerConfig.readProperties(CONFIG_FILE);
        assertEquals(result.entrySet().toString(), "[hello=value, cdec.host.dns=localhost]");
    }

    @Test
    public void testReadCdecHostDns() throws Exception {
        assertEquals(InstallationManagerConfig.readCdecHostDns(), "localhost");
    }

    @Test
    public void testGetConfFile() {
        String testConfigPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath() + "another/im.properties";
        CONFIG_FILE = Paths.get(testConfigPath);
        assertEquals(InstallationManagerConfig.getConfFile().toAbsolutePath().toString(), testConfigPath);
        assertTrue(exists(CONFIG_FILE.getParent()));
    }
}
