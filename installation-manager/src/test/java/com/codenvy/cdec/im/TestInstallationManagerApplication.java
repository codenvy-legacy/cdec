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
package com.codenvy.cdec.im;

import com.codenvy.cdec.Daemon;
import com.codenvy.cdec.InstallationManagerService;
import com.codenvy.cdec.RestletClientFactory;

import org.restlet.ext.json.JsonRepresentation;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Dmytro Nochevnov
 */
public class TestInstallationManagerApplication {
    private InstallationManagerService managerServiceProxy;

    @BeforeMethod
    public void setUp() throws Exception {
        Daemon.start();
        managerServiceProxy = RestletClientFactory.getServiceProxy(InstallationManagerService.class);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        Daemon.stop();
    }

    @Test
    public void testCheckNewVersions() throws Exception {
        String expectedContent = "[{\"status\":\"downloaded\",\"version\":\"v1\"}]";

        JsonRepresentation response = managerServiceProxy.doCheckNewVersions("v1");
        assertNotNull(response);
        assertEquals(response.getJsonArray().toString(), expectedContent);
    }
}
