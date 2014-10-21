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

import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.fail;

/**
 * @author Dmytro Nochevnov
 */
public class TestDaemon {
    protected static final Logger LOG = LoggerFactory.getLogger(TestDaemon.class);

    private InstallationManagerService installationManagerServiceProxy;

    @BeforeTest
    public void setUp() throws Exception {
        Daemon.start();
        installationManagerServiceProxy = RestletClientFactory.createServiceProxy(InstallationManagerService.class);
    }

    @AfterTest
    public void tearDown() throws Exception {
        Daemon.stop();
    }

    @Test
    public void testObtainChallengeRequest() throws Exception {
        try {
            installationManagerServiceProxy.obtainChallengeRequest();
        } catch (ResourceException re) {
            fail(re.getStatus().toString() + ". " + re.getMessage());
        }
    }
}