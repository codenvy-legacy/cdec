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


import com.codenvy.im.exceptions.ArtifactNotFoundException;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.user.UserCredentials;

import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.resource.ResourceException;
import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Dmytro Nochevnov
 */
public class TestRequest {

    @Test
    public void testJacksonRepresentation() throws Exception {
        Request testRequest = new Request()
                .setArtifactName("artifact name")
                .setVersion("artifact version")
                .setUserCredentials(new UserCredentials("test token", "test account id"))
                .setInstallOptions(new InstallOptions().setInstallType(InstallOptions.InstallType.CODENVY_SINGLE_SERVER));

        JacksonRepresentation<Request> requestRep = testRequest.toRepresentation();
        assertNotNull(requestRep);

        Request restoredRequest = Request.fromRepresentation(requestRep);
        assertNotNull(restoredRequest);

        assertEquals(restoredRequest.getArtifactName(), "artifact name");
        assertEquals(restoredRequest.getVersion(), "artifact version");

        assertEquals(restoredRequest.getUserCredentials().getToken(), "test token");
        assertEquals(restoredRequest.getUserCredentials().getAccountId(), "test account id");
        assertNotNull(restoredRequest.getInstallOptions());
        assertEquals(restoredRequest.getInstallOptions().getInstallType(), InstallOptions.InstallType.CODENVY_SINGLE_SERVER);
    }

    @Test
    public void testValidate() throws Exception {
        Request request = new Request()
                .setArtifactName("cdec")
                .setVersion("3.1.0")
                .setUserCredentials(new UserCredentials("test token", "test account id"));
        request.validate(Request.ValidationType.CREDENTIALS);
    }

    @Test(expectedExceptions = ResourceException.class)
    public void testValidateCredentials() throws Exception {
        Request request = new Request();
        request.validate(Request.ValidationType.CREDENTIALS);
    }

    @Test(expectedExceptions = ResourceException.class)
    public void testValidateArtifact() throws Exception {
        Request request = new Request();
        request.validate(Request.ValidationType.ARTIFACT);
    }

    @Test(expectedExceptions = ResourceException.class)
    public void testValidateInstallOptions() throws Exception {
        Request request = new Request();
        request.validate(Request.ValidationType.INSTALL_OPTIONS);
    }

    @Test(expectedExceptions = ArtifactNotFoundException.class)
    public void testValidateUnknownArtifact() throws Exception {
        Request request = new Request().setArtifactName("artifact");
        request.validate(Request.ValidationType.ARTIFACT);
    }
}
