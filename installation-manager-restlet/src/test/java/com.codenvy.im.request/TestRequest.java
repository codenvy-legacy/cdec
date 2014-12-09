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


import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.exceptions.ArtifactNotFoundException;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.user.UserCredentials;

import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;

import org.testng.annotations.Test;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

/** @author Dmytro Nochevnov */
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

        // check real-life scenario of parsing JSON to recognize errors like "Unrecognized field"
        String json = requestRep.getText();
        Representation rep = new JsonRepresentation(json);
        Request restoredRequest = Request.fromRepresentation(new JacksonRepresentation<>(rep, Request.class));
        assertNotNull(restoredRequest);

        assertEquals(restoredRequest.getArtifactName(), "artifact name");
        assertEquals(restoredRequest.getVersion(), "artifact version");

        assertEquals(restoredRequest.getUserCredentials().getToken(), "test token");
        assertEquals(restoredRequest.getUserCredentials().getAccountId(), "test account id");
        assertNotNull(restoredRequest.getInstallOptions());
        assertEquals(restoredRequest.getInstallOptions().getInstallType(), InstallOptions.InstallType.CODENVY_SINGLE_SERVER);
    }

    @Test
    public void testValidateArtifact() throws Exception {
        Request request = new Request()
                .setArtifactName("cdec")
                .setVersion("3.1.0")
                .setUserCredentials(new UserCredentials("test token", "test account id"));
        request.validate(Request.ValidationType.ARTIFACT);

        ensureFullValidationException(request);
    }

    @Test
    public void testValidateCredentials() throws Exception {
        Request request = new Request()
            .setUserCredentials(new UserCredentials("test token", "test account id"));
        request.validate(Request.ValidationType.CREDENTIALS);

        ensureFullValidationException(request);
    }

    @Test
    public void testValidateInstallOptions() throws Exception {
        Request request = new Request()
            .setInstallOptions(new InstallOptions().setInstallType(InstallOptions.InstallType.CODENVY_SINGLE_SERVER));
        request.validate(Request.ValidationType.INSTALL_OPTIONS);

        ensureFullValidationException(request);
    }

    @Test
    public void testValidationType() {
        assertEquals(Request.ValidationType.ARTIFACT
                     + Request.ValidationType.CREDENTIALS
                     + Request.ValidationType.INSTALL_OPTIONS
                     + Request.ValidationType.FULL, 14);
    }

    @Test(expectedExceptions = ResourceException.class,
          expectedExceptionsMessageRegExp = "Request is incomplete. User credentials are missed.")
    public void testValidateEmptyCredentials() throws Exception {
        Request request = new Request();
        request.validate(Request.ValidationType.CREDENTIALS);
    }

    @Test(expectedExceptions = ResourceException.class,
          expectedExceptionsMessageRegExp = "Request is incomplete. Artifact name is missed.")
    public void testValidateEmptyArtifact() throws Exception {
        Request request = new Request();
        request.validate(Request.ValidationType.ARTIFACT);
    }

    @Test(expectedExceptions = ResourceException.class,
          expectedExceptionsMessageRegExp = "Request is incomplete. Installation options are missed.")
    public void testValidateEmptyInstallOptions() throws Exception {
        Request request = new Request();
        request.validate(Request.ValidationType.INSTALL_OPTIONS);
    }

    @Test(expectedExceptions = ArtifactNotFoundException.class,
          expectedExceptionsMessageRegExp = "Artifact 'unknown' not found")
    public void testValidateUnknownArtifact() throws Exception {
        Request request = new Request().setArtifactName("unknown");
        request.validate(Request.ValidationType.ARTIFACT);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Illegal version 'incorrect'")
    public void testValidateIncorrectVersion() throws Exception {
        Request request = new Request().setArtifactName(InstallManagerArtifact.NAME)
                                       .setVersion("incorrect");
        request.validate(Request.ValidationType.ARTIFACT);
    }

    @Test(expectedExceptions = ResourceException.class,
          expectedExceptionsMessageRegExp = "Request is incomplete. Request is empty.")
    public void testNullRequest() throws IOException {
        Request.fromRepresentation(null);
    }

    private void ensureFullValidationException(Request request) throws ArtifactNotFoundException {
        try {
            request.validate(Request.ValidationType.FULL);
        } catch(ResourceException e) {
            return;
        }

        fail("FULL validation doesn't work as expected");
    }
}
