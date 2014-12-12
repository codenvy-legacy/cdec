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

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/** @author Dmytro Nochevnov */
public class TestInstallationManagerConfig {

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
}
