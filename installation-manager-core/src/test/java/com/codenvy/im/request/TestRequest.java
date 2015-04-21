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
package com.codenvy.im.request;


import com.codenvy.im.artifacts.ArtifactFactory;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.exceptions.ArtifactNotFoundException;
import com.codenvy.im.facade.UserCredentials;
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
                .setArtifactName("codenvy")
                .setVersion("1.0.1")
                .setUserCredentials(new UserCredentials("test token", "test account id"));

        assertEquals(testRequest.getArtifactName(), CDECArtifact.NAME);
        assertEquals(testRequest.createArtifact(), ArtifactFactory.createArtifact(CDECArtifact.NAME));
        assertEquals(testRequest.getVersion(), "1.0.1");
        assertEquals(testRequest.createVersion(), Version.valueOf("1.0.1"));
        Assert.assertNotNull(testRequest.getUserCredentials());
        assertEquals(testRequest.obtainAccessToken(), "test token");
        assertEquals(testRequest.obtainAccountId(), "test account id");
    }

    @Test
    public void testRequestWhenUserCredentialsUndefined() throws Exception {
        Request testRequest = new Request();
        assertEquals(testRequest.obtainAccessToken(), "");
        assertEquals(testRequest.obtainAccountId(), "");
    }

    @Test(expectedExceptions = ArtifactNotFoundException.class)
    public void testRequestErrorIfArtifactUnknown() throws Exception {
        Request testRequest = new Request()
                .setArtifactName("cdecxxx")
                .setVersion("1.0.1");

        testRequest.createArtifact();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRequestErrorIfVersionInvalid() throws Exception {
        Request testRequest = new Request()
                .setArtifactName("codenvy")
                .setVersion("00.1.1");

        testRequest.createVersion();
    }

    @Test
    public void testEquals() throws Exception {
        Request request1 = new Request()
                .setArtifactName("codenvy")
                .setVersion("1.0.1")
                .setUserCredentials(new UserCredentials("test token", "test account id"));

        Request request2 = new Request()
                .setArtifactName("codenvy")
                .setVersion("1.0.1")
                .setUserCredentials(new UserCredentials("test token", "test account id"));

        assertTrue(request1.equals(request2));
        assertEquals(request1.hashCode(), request2.hashCode());
    }
}
