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

import org.json.JSONArray;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * @author Dmytro Nochevnov
 */
public class TestInstallationManagerApplication {
    protected static final Logger LOG = LoggerFactory.getLogger(TestInstallationManagerApplication.class);

    InstallationManagerService managerSeviceProxy;

    @BeforeMethod
    public void setUp() throws Exception {
        Daemon.start();

//        System.in.read();

        managerSeviceProxy = RestletClientFactory.getServiceProxy(InstallationManagerService.class);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        Daemon.stop();
    }

    @Test
    public void testCheckNewVersions() throws Exception {
        String expectedContent = "[{\"status\":\"downloaded\",\"version\":\"v1\"}]";

        try {
            JsonRepresentation response = managerSeviceProxy.doCheckNewVersions("v1");

            if (response == null) {
                fail();
            }

            JSONArray artifacts = response.getJsonArray();
            LOG.info(artifacts.toString());

            for (int i = 0; i < artifacts.length(); i++) {
                JSONObject artifact = artifacts.getJSONObject(i);
                LOG.info(artifact.toString());
            }

            assertEquals(expectedContent, artifacts.toString());

        } catch (ResourceException re) {
            if (re.getStatus().equals(Status.SERVER_ERROR_INTERNAL)) {
                LOG.info(re.getMessage());
            }
            
            fail();
        }
    }
    

}
