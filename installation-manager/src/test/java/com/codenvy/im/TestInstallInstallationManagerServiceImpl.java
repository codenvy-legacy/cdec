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
import com.codenvy.im.artifacts.ArtifactFactory;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.request.Request;
import com.codenvy.im.restlet.InstallationManager;
import com.codenvy.im.restlet.InstallationManagerService;
import com.codenvy.im.user.UserCredentials;
import com.codenvy.im.utils.HttpTransport;

import com.codenvy.im.utils.Version;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 */
public class TestInstallInstallationManagerServiceImpl {
    private InstallationManagerService installationManagerService;

    @Mock
    private InstallationManager mockInstallationManager;

    @Mock
    private HttpTransport       mockTransport;

    private Artifact            installManagerArtifact;
    private Artifact            cdecArtifact;
    private UserCredentials     testCredentials;

    @BeforeMethod
    public void init() {
        MockitoAnnotations.initMocks(this);

        installationManagerService =
            new InstallationManagerServiceImpl(mockInstallationManager, mockTransport, new DownloadDescriptorHolder());
        testCredentials = new UserCredentials("auth token");

        installManagerArtifact = ArtifactFactory.createArtifact(InstallManagerArtifact.NAME);
        cdecArtifact = ArtifactFactory.createArtifact(CDECArtifact.NAME);
    }

    @Test
    public void testInstallArtifact() throws Exception {
        Version testVersion = Version.valueOf("2.10.5");

        doReturn(testVersion).when(mockInstallationManager).getLatestVersionToDownload(testCredentials.getToken(), cdecArtifact);
        doNothing().when(mockInstallationManager).install(testCredentials.getToken(), cdecArtifact, testVersion, null);

        Request request = new Request()
            .setUserCredentials(testCredentials)
            .setArtifactName(cdecArtifact.getName());

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
    public void testInstallSpecificVersionArtifact() throws Exception {
        Version testVersion = Version.valueOf("2.10.5");
        doNothing().when(mockInstallationManager).install(testCredentials.getToken(), cdecArtifact, testVersion, null);

        Request request = new Request()
            .setUserCredentials(testCredentials)
            .setArtifactName(cdecArtifact.getName())
            .setVersion("2.10.5");

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
    public void testInstallArtifactWithoutUpdate() throws Exception {
        doReturn(null).when(mockInstallationManager).getLatestVersionToDownload(testCredentials.getToken(), cdecArtifact);

        Request request = new Request()
            .setUserCredentials(testCredentials)
            .setArtifactName(cdecArtifact.getName());

        String response = installationManagerService.install(new JacksonRepresentation<>(request));
        assertEquals(response, "{\n"
                               + "  \"message\" : \"Artifact 'cdec' isn't available to update.\",\n"
                               + "  \"status\" : \"ERROR\"\n"
                               + "}");
    }

    @Test
    public void testInstallVersionWhichIsNotDownloaded() throws Exception {
        Version testVersion = Version.valueOf("2.10.5");
        doThrow(new FileNotFoundException("Binaries to install artifact 'cdec' version '2.10.5' not found."))
            .when(mockInstallationManager)
            .install(testCredentials.getToken(), cdecArtifact, testVersion, null);

        Request request = new Request()
            .setUserCredentials(testCredentials)
            .setArtifactName(cdecArtifact.getName())
            .setVersion("2.10.5");

        String response = installationManagerService.install(new JacksonRepresentation<>(request));
        assertEquals(response, "{\n" +
                               "  \"artifacts\" : [ {\n" +
                               "    \"artifact\" : \"cdec\",\n" +
                               "    \"version\" : \"2.10.5\",\n" +
                               "    \"status\" : \"FAILURE\"\n" +
                               "  } ],\n" +
                               "  \"message\" : \"Binaries to install artifact 'cdec' version '2.10.5' not found.\",\n" +
                               "  \"status\" : \"ERROR\"\n" +
                               "}");
    }

    @Test
    public void testInstallVersionWhichIsNotInsallable() throws Exception {
        Version testVersion = Version.valueOf("2.10.5");
        doThrow(new IllegalStateException("Can not install the artifact 'installation-manager' version '2.10.5'."))
            .when(mockInstallationManager)
            .install(testCredentials.getToken(), installManagerArtifact, testVersion, null);

        Request request = new Request()
            .setUserCredentials(testCredentials)
            .setArtifactName(InstallManagerArtifact.NAME)
            .setVersion("2.10.5");

        String response = installationManagerService.install(new JacksonRepresentation<>(request));
        assertEquals(response, "{\n" +
                               "  \"artifacts\" : [ {\n" +
                               "    \"artifact\" : \"installation-manager\",\n" +
                               "    \"version\" : \"2.10.5\",\n" +
                               "    \"status\" : \"FAILURE\"\n" +
                               "  } ],\n" +
                               "  \"message\" : \"Can not install the artifact 'installation-manager' version '2.10.5'.\",\n" +
                               "  \"status\" : \"ERROR\"\n" +
                               "}");
    }

    @Test
    public void testInstallUnexistedArtifact() throws Exception {
        Request request = new Request()
            .setUserCredentials(testCredentials)
            .setArtifactName("qwerty");

        String response = installationManagerService.install(new JacksonRepresentation<>(request));
        assertEquals(response, "{\n" +
                               "  \"message\" : \"Artifact 'qwerty' not found\",\n" +
                               "  \"status\" : \"ERROR\"\n" +
                               "}");
    }

    @Test
    public void testInstallUnknownArtifact() throws Exception {
        Request request = new Request()
            .setUserCredentials(testCredentials);

        String response = installationManagerService.install(new JacksonRepresentation<>(request));
        assertEquals(response, "{\n" +
                               "  \"message\" : \"Request is incomplete: artifact name was missed.\",\n" +
                               "  \"status\" : \"ERROR\"\n" +
                               "}");
    }
}
