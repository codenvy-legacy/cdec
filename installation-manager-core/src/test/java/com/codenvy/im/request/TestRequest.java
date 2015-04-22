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
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.utils.Version;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
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

    @Test
    public void testSimpleEquals() {
        Request request = new Request().setArtifactName("codenvy");
        assertTrue(request.equals(request));
        assertFalse(request.equals(null));
    }

    @Test(dataProvider = "testEqualsAndHashCodeData")
    public void testEqualsAndHashCode(UserCredentials credentials1, String artifact1, String version1, InstallOptions options1,
                                      UserCredentials credentials2, String artifact2, String version2, InstallOptions options2,
                                      boolean expectedEquality) throws Exception {
        Request request1 = new Request().setUserCredentials(credentials1).setArtifactName(artifact1).setVersion(version1).setInstallOptions(options1);
        Request request2 = new Request().setUserCredentials(credentials2).setArtifactName(artifact2).setVersion(version2).setInstallOptions(options2);

        assertEquals(request1.equals(request2), expectedEquality);
        request1.hashCode();
    }

    @DataProvider(name = "testEqualsAndHashCodeData")
    public Object[][] TestEqualsAndHashCodeData() {
        return new Object[][]{
            {null, null, null, null,
             null, null, null, null,
             true},

            {new UserCredentials("token1"), "artifact1", "1.0.0", new InstallOptions().setStep(1),
             new UserCredentials("token1"), "artifact1", "1.0.0", new InstallOptions().setStep(1),
             true},

            {new UserCredentials("token1"), null, null, null,
             null, null, null, null,
             false},
            {new UserCredentials("token1"), null, null, null,
             new UserCredentials("token2"), null, null, null,
             false},
            {null, null, null, null,
             new UserCredentials("token2"), null, null, null,
             false},

            {new UserCredentials("token1"), "artifact1", null, null,
             new UserCredentials("token1"), null, null, null,
             false},
            {new UserCredentials("token1"), "artifact1", null, null,
             new UserCredentials("token1"), "artifact2", null, null,
             false},
            {new UserCredentials("token1"), null, null, null,
             new UserCredentials("token1"), "artifact2", null, null,
             false},

            {new UserCredentials("token1"), "artifact1", "1.0.0", null,
             new UserCredentials("token1"), "artifact1", null, null,
             false},
            {new UserCredentials("token1"), "artifact1", "1.0.0", null,
             new UserCredentials("token1"), "artifact1", "1.0.1", null,
             false},
            {new UserCredentials("token1"), "artifact1", null, null,
             new UserCredentials("token1"), "artifact1", "1.0.1", null,
             false},

            {new UserCredentials("token1"), "artifact1", "1.0.0", new InstallOptions().setStep(1),
             new UserCredentials("token1"), "artifact1", "1.0.0", null,
             false},
            {new UserCredentials("token1"), "artifact1", "1.0.0", new InstallOptions().setStep(1),
             new UserCredentials("token1"), "artifact1", "1.0.0", new InstallOptions().setStep(2),
             false},
            {new UserCredentials("token1"), "artifact1", "1.0.0", null,
             new UserCredentials("token1"), "artifact1", "1.0.0", new InstallOptions().setStep(2),
             false},
        };
    }
}
