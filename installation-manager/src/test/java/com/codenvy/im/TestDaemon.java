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
import com.google.inject.Injector;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.quartz.SchedulerException;
import org.restlet.ext.jaxrs.internal.exceptions.IllegalPathException;
import org.restlet.ext.jaxrs.internal.exceptions.MissingAnnotationException;
import org.restlet.resource.ResourceException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.doThrow;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * @author Dmytro Nochevnov
 */
public class TestDaemon {
    private final UpdateManager originUpdateManager = Daemon.updateManager;
    private final RestletServer originRestletServer = Daemon.restletServer;

    private InstallationManagerService installationManagerServiceProxy;

    @Mock
    private UpdateManager mockUpdateManager;

    @Mock
    private RestletServer mockRestletServer;

    @BeforeMethod
    public void setUp() throws MissingAnnotationException, IllegalPathException {
        MockitoAnnotations.initMocks(this);

        Daemon.updateManager = originUpdateManager;
        Daemon.restletServer = originRestletServer;

        installationManagerServiceProxy = RestletClientFactory.createServiceProxy(InstallationManagerService.class);
    }

    @AfterMethod
    public void stopResteltServer() {
        Daemon.stop();
    }

    @Test
    public void testStartStop() throws Exception {
        Daemon.start();
        ensureRestletServerStarted();

        Daemon.stop();
        ensureRestletServerStopped();
    }

    @Test
    public void testMainMethod() throws MissingAnnotationException, IllegalPathException {
        Daemon.main(new String[0]);
        ensureRestletServerStarted();
    }

    @Test
    public void testStartException() throws Exception {
        doThrow(SchedulerException.class).when(mockUpdateManager).init();
        Daemon.updateManager = mockUpdateManager;
        Daemon.start();

        setUp();
        doThrow(Exception.class).when(mockRestletServer).start();
        Daemon.restletServer = mockRestletServer;
        Daemon.start();
    }

    @Test
    public void testStopException() throws Exception {
        doThrow(SchedulerException.class).when(mockUpdateManager).destroy();
        Daemon.updateManager = mockUpdateManager;
        Daemon.stop();

        setUp();
        doThrow(Exception.class).when(mockRestletServer).stop();
        Daemon.restletServer = mockRestletServer;
        Daemon.stop();
    }

    private void ensureRestletServerStopped() {
        try {
            installationManagerServiceProxy.getUpdateServerEndpoint();
        } catch(ResourceException e) {
            assertEquals(e.getMessage(), "Connection Error");
            return;
        }

        fail("Connection Error expected.");
    }

    private void ensureRestletServerStarted() {
        installationManagerServiceProxy.getUpdateServerEndpoint();
    }
}
