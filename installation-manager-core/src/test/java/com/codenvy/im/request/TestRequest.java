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
package com.codenvy.im.request;


import com.codenvy.im.artifacts.ArtifactFactory;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.exceptions.ArtifactNotFoundException;
import com.codenvy.im.service.UserCredentials;
import com.codenvy.im.utils.Version;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/** @author Dmytro Nochevnov */
public class TestRequest {

    @Test
    public void testRequest() throws Exception {
        Request testRequest = new Request()
                .setArtifactName("cdec")
                .setVersion("1.0.1")
                .setUserCredentials(new UserCredentials("test token", "test account id"));

        assertEquals(testRequest.getArtifact(), ArtifactFactory.createArtifact(CDECArtifact.NAME));
        assertEquals(testRequest.getVersion(), Version.valueOf("1.0.1"));
        Assert.assertNotNull(testRequest.getUserCredentials());
        assertEquals(testRequest.getAccessToken(), "test token");
        assertEquals(testRequest.getAccountId(), "test account id");
    }

    @Test(expectedExceptions = ArtifactNotFoundException.class)
    public void testRequestErrorIfArtifactUnknown() throws Exception {
        Request testRequest = new Request()
                .setArtifactName("cdecxxx")
                .setVersion("1.0.1");

        testRequest.getArtifact();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRequestErrorIfVersionInvalid() throws Exception {
        Request testRequest = new Request()
                .setArtifactName("cdec")
                .setVersion("00.1.1");

        testRequest.getVersion();
    }

    @Test
    public void testEquals() throws Exception {
        Request request1 = new Request()
                .setArtifactName("cdec")
                .setVersion("1.0.1")
                .setUserCredentials(new UserCredentials("test token", "test account id"));

        Request request2 = new Request()
                .setArtifactName("cdec")
                .setVersion("1.0.1")
                .setUserCredentials(new UserCredentials("test token", "test account id"));

        assertTrue(request1.equals(request2));
        assertEquals(request1.hashCode(), request2.hashCode());
    }
}
