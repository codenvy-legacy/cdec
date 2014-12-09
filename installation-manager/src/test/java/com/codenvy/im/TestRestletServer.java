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
package com.codenvy.im;

import com.codenvy.im.restlet.InstallationManagerService;
import com.codenvy.im.restlet.RestletClientFactory;
import com.codenvy.im.restlet.RestletServer;
import org.restlet.resource.ResourceException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Application;
import java.net.MalformedURLException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**  @author Dmytro Nochevnov */
public class TestRestletServer {
    private InstallationManagerService installationManagerServiceProxy;
    private Application imApplication;
    private RestletServer restletServer;

    @BeforeClass
    public void setUp() throws Exception {
        installationManagerServiceProxy = RestletClientFactory.createServiceProxy(InstallationManagerService.class);
        imApplication = new InstallationManagerApplication();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        restletServer.stop();
    }

    @Test
    public void testStartStop() throws Exception {
        try {
            restletServer = new RestletServer(imApplication);
        } catch (MalformedURLException e) {
            fail(e.getMessage());
        }

        restletServer.start();
        ensureRestletServerStarted();

        restletServer.stop();
        ensureRestletServerStopped();
    }

    private void ensureRestletServerStopped() {
        try {
            installationManagerServiceProxy.getUpdateServerEndpoint();
        } catch (ResourceException e) {
            assertEquals(e.getMessage(), "Connection Error");
            return;
        }

        fail("Connection Error expected.");
    }

    private void ensureRestletServerStarted() {
        installationManagerServiceProxy.getUpdateServerEndpoint();
    }
}
