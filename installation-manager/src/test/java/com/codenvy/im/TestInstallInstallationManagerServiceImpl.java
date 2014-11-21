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
import com.codenvy.im.install.CdecInstallOptions;
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
import java.util.Collections;
import java.util.LinkedHashMap;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
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
    @Mock
    private Artifact            mockCdecArtifact;
    @Mock
    private Artifact            installManagerArtifact;
    @Mock
    private Artifact            cdecArtifact;
    private UserCredentials     testCredentials;

    @BeforeMethod
    public void init() {
        MockitoAnnotations.initMocks(this);

        installationManagerService =
                new InstallationManagerServiceImpl(mockInstallationManager, mockTransport, new DownloadDescriptorHolder());
        testCredentials = new UserCredentials("auth token");
    }

    @Test
    public void testInstallArtifact() throws Exception {
        Version testVersion = Version.valueOf("2.10.5");

        doReturn(testVersion).when(mockInstallationManager).getLatestVersionToDownload(testCredentials.getToken(), cdecArtifact);
        doNothing().when(mockInstallationManager).install(testCredentials.getToken(), cdecArtifact, testVersion, null);

        Request request = new Request()
            .setUserCredentials(testCredentials)
            .setArtifactName(mockCdecArtifact.getName())
            .setInstallOptions(new CdecInstallOptions());
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
    public void testInstallSpecificArtifactDoNothing() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.5");
            }
        }).when(mockInstallationManager).getUpdates(testCredentials.getToken());
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.5");
            }
        }).when(mockInstallationManager).getInstalledArtifacts(testCredentials.getToken());

        Request request = new Request()
            .setUserCredentials(testCredentials)
            .setArtifactName(mockCdecArtifact.getName())
            .setInstallOptions(new CdecInstallOptions())
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
    public void testInstallErrorIfTryToInstallSpecificArtifactLowerVersion() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.5");
            }
        }).when(mockInstallationManager).getUpdates(testCredentials.getToken());
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.6");
            }
        }).when(mockInstallationManager).getInstalledArtifacts(testCredentials.getToken());

        Artifact artifact = createArtifact(mockCdecArtifact.getName());
        doThrow(new IllegalStateException(
                "Can not install the artifact '" + mockCdecArtifact.getName() + ":2.10.5', because we don't support downgrade artifacts."))
                .when(mockInstallationManager).install("auth token", artifact, Version.valueOf("2.10.5"), new CdecInstallOptions());

        Request request = new Request()
            .setUserCredentials(testCredentials)
            .setArtifactName(mockCdecArtifact.getName())
            .setInstallOptions(new CdecInstallOptions());
        String response = installationManagerService.install(new JacksonRepresentation<>(request));
        assertEquals(response, "{\n" +
                                "  \"artifacts\" : [ {\n" +
                                "    \"artifact\" : \"cdec\",\n" +
                                "    \"version\" : \"2.10.5\",\n" +
                                "    \"status\" : \"FAILURE\"\n" +
                                "  } ],\n" +
                                "  \"message\" : \"Can not install the artifact 'cdec:2.10.5', because we don't support downgrade artifacts.\",\n" +
                                "  \"status\" : \"ERROR\"\n" +
                                "}");
    }

    @Test
    public void testInstallErrorIfSpecificArtifactAbsent() throws Exception {
        doReturn(Collections.emptyMap()).when(mockInstallationManager).getUpdates(testCredentials.getToken());
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.6");
            }
        }).when(mockInstallationManager).getInstalledArtifacts(testCredentials.getToken());

        Request request = new Request()
            .setUserCredentials(testCredentials)
            .setArtifactName(mockCdecArtifact.getName())
            .setInstallOptions(new CdecInstallOptions());
        String response = installationManagerService.install(new JacksonRepresentation<>(request));
        assertEquals(response, "{\n" +
                               "  \"message\" : \"Artifact 'cdec' isn't available to update.\",\n" +
                               "  \"status\" : \"ERROR\"\n" +
                               "}");
    }

    @Test
    public void testInstallSpecificArtifact() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.5");
            }
        }).when(mockInstallationManager).getUpdates(testCredentials.getToken());
        doReturn(new LinkedHashMap<Artifact, String>() {
        }).when(mockInstallationManager).getInstalledArtifacts(testCredentials.getToken());

        Request request = new Request()
            .setUserCredentials(testCredentials)
            .setArtifactName(mockCdecArtifact.getName())
            .setInstallOptions(new CdecInstallOptions())
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
            .setArtifactName(mockCdecArtifact.getName())
            .setVersion("2.10.5")
            .setInstallOptions(new CdecInstallOptions());

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
            .setArtifactName(mockCdecArtifact.getName())
            .setVersion("2.10.5")
            .setInstallOptions(new CdecInstallOptions())
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
    public void testInstallSpecificArtifactErrorTryToInstallLowerVersion() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.5");
            }
        }).when(mockInstallationManager).getUpdates(testCredentials.getToken());
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.6");
            }
        }).when(mockInstallationManager).getInstalledArtifacts(testCredentials.getToken());

        Artifact artifact = createArtifact(mockCdecArtifact.getName());
        doThrow(new IllegalStateException(
                "Can not install the artifact '" + mockCdecArtifact.getName() + ":2.10.5', because we don't support downgrade artifacts."))
                .when(mockInstallationManager).install("auth token", artifact, Version.valueOf("2.10.5"), new CdecInstallOptions());

        Request request = new Request()
            .setUserCredentials(testCredentials)
            .setArtifactName(mockCdecArtifact.getName())
            .setVersion("2.10.5")
            .setInstallOptions(new CdecInstallOptions());

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
    public void testInstallErrorIfSpecificVersionArtifactAbsent() throws Exception {
        doReturn(Collections.emptyMap()).when(mockInstallationManager).getUpdates(testCredentials.getToken());
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.6");
            }
        }).when(mockInstallationManager).getInstalledArtifacts(testCredentials.getToken());

        Artifact artifact = createArtifact(mockCdecArtifact.getName());
        doThrow(new IllegalStateException(
                "Artifact '" + mockCdecArtifact.getName() + "'  isn't available to update."))
                .when(mockInstallationManager).install("auth token", artifact, Version.valueOf("2.10.7"), new CdecInstallOptions());

        Request request = new Request()
            .setUserCredentials(testCredentials)
            .setArtifactName(mockCdecArtifact.getName())
            .setVersion("2.10.7")
            .setInstallOptions(new CdecInstallOptions());

        String response = installationManagerService.install(new JacksonRepresentation<>(request));
        assertEquals(response, "{\n" +
                               "  \"message\" : \"Artifact 'qwerty' not found\",\n" +
                               "  \"status\" : \"ERROR\"\n" +
                               "}");
    }

    @Test
    public void testInstallUnknownArtifact() throws Exception {
        Request request = new Request()
                .setUserCredentials(testCredentials)
                .setArtifactName(mockCdecArtifact.getName())
                .setVersion("2.10.5")
                .setInstallOptions(new CdecInstallOptions());
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
    public void testInstallErrorIfUnknownArtifact() throws Exception {
        doReturn(Collections.emptyMap()).when(mockInstallationManager).getUpdates(testCredentials.getToken());
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.7");
            }
        }).when(mockInstallationManager).getInstalledArtifacts(testCredentials.getToken());
        Request request = new Request()
                .setUserCredentials(testCredentials)
                .setArtifactName(mockCdecArtifact.getName())
                .setVersion("2.10.5")
                .setInstallOptions(new CdecInstallOptions());

        String response = installationManagerService.install(new JacksonRepresentation<>(request));
        assertEquals(response, "{\n" +
                               "  \"message\" : \"Request is incomplete: artifact name was missed.\",\n" +
                               "  \"status\" : \"ERROR\"\n" +
                               "}");
    }
}
