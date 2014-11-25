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

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.request.Request;
import com.codenvy.im.restlet.InstallationManager;
import com.codenvy.im.restlet.InstallationManagerService;
import com.codenvy.im.user.UserCredentials;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;

import org.restlet.ext.jackson.JacksonRepresentation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 */
public class TestInstallInstallationManagerServiceImpl {
    private InstallationManagerService installationManagerService;

    private InstallationManager mockInstallationManager;
    private HttpTransport       mockTransport;
    private Artifact            cdecArtifact;
    private UserCredentials     testCredentials;

    @BeforeMethod
    public void init() throws Exception {
        mockInstallationManager = mock(InstallationManager.class);
        mockTransport = mock(HttpTransport.class);
        cdecArtifact = createArtifact(CDECArtifact.NAME);
        installationManagerService = new InstallationManagerServiceImpl(mockInstallationManager, mockTransport, new DownloadDescriptorHolder());
        testCredentials = new UserCredentials("auth token");
    }

    @Test
    public void testInstall() throws Exception {
        InstallOptions installOptions = new InstallOptions();
        Version version = Version.valueOf("2.10.5");

        doReturn(version).when(mockInstallationManager).getLatestVersionToDownload(testCredentials.getToken(), cdecArtifact);
        doNothing().when(mockInstallationManager).install(testCredentials.getToken(), cdecArtifact, version, installOptions);

        Request request = new Request()
                .setUserCredentials(testCredentials)
                .setArtifactName(cdecArtifact.getName())
                .setInstallOptions(installOptions);

        String response = installationManagerService.install(new JacksonRepresentation<>(request));
        assertEquals(response, "{\n" +
                               "  \"artifacts\" : [ {\n" +
                               "    \"artifact\" : \"cdec\",\n" +
                               "    \"version\" : \"2.10.5\",\n" +
                               "    \"status\" : \"SUCCESS\"\n" +
                               "  } ],\n" +
                               "  \"status\" : \"OK\"\n" +
                               "}");
    }

    @Test
    public void testInstallError() throws Exception {
        InstallOptions installOptions = new InstallOptions();

        doThrow(new IOException("I/O error")).when(mockInstallationManager)
                                             .install(testCredentials.getToken(), cdecArtifact, Version.valueOf("1.0.1"), installOptions);

        Request request = new Request()
                .setUserCredentials(testCredentials)
                .setArtifactName(cdecArtifact.getName())
                .setVersion("1.0.1")
                .setInstallOptions(installOptions);

        String response = installationManagerService.install(new JacksonRepresentation<>(request));
        assertEquals(response, "{\n" +
                               "  \"artifacts\" : [ {\n" +
                               "    \"artifact\" : \"cdec\",\n" +
                               "    \"version\" : \"1.0.1\",\n" +
                               "    \"status\" : \"FAILURE\"\n" +
                               "  } ],\n" +
                               "  \"message\" : \"I/O error\",\n" +
                               "  \"status\" : \"ERROR\"\n" +
                               "}");
    }

    @Test
    public void testInstallErrorIfArtifactIsMissed() throws Exception {
        Request request = new Request()
                .setUserCredentials(testCredentials)
                .setInstallOptions(new InstallOptions());

        String response = installationManagerService.install(new JacksonRepresentation<>(request));
        assertEquals(response, "{\n" +
                               "  \"message\" : \"Request is incomplete. Artifact name is missed.\",\n" +
                               "  \"status\" : \"ERROR\"\n" +
                               "}");
    }
}
