/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
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
package com.codenvy.im.command;

import com.codenvy.im.service.InstallationManagerConfig;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Paths;

import static com.codenvy.im.service.InstallationManagerConfig.CONFIG_FILE;
import static org.testng.Assert.assertEquals;

/** @author Dmytro Nochevnov */
public class TestStoreIMConfigPropertyCommand {

    @BeforeMethod
    public void setup() {
        CONFIG_FILE = Paths.get(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath() + "another/im.properties");
    }

    @Test
    public void testCommandToString() {
        Command testCommand = new StoreIMConfigPropertyCommand("property", "value");
        assertEquals(testCommand.toString(), "{'propertyName':'property','propertyValue':'value'}");
    }

    @Test
    public void testCommandDescription() {
        Command testCommand = new StoreIMConfigPropertyCommand("property", "value");
        assertEquals(testCommand.getDescription(), "Save property property = value into the installation manager config");
    }

    @Test
    public void testCreateSavePuppetMasterHostDnsCommand() throws IOException {
        String testPuppetMasterHostDns = "test";
        Command command = StoreIMConfigPropertyCommand.createSavePuppetMasterHostDnsCommand(testPuppetMasterHostDns);
        command.execute();
        assertEquals(InstallationManagerConfig.readPuppetMasterNodeDns(), testPuppetMasterHostDns);
    }
}
