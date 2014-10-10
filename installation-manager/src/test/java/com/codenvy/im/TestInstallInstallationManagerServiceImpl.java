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
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.restlet.InstallationManager;
import com.codenvy.im.restlet.InstallationManagerService;
import com.codenvy.im.user.UserCredentials;
import com.codenvy.im.utils.HttpTransport;

import org.restlet.ext.jackson.JacksonRepresentation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.LinkedHashMap;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;
import static com.codenvy.im.utils.Commons.getPrettyPrintingJson;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 */
public class TestInstallInstallationManagerServiceImpl {
    private InstallationManagerService installationManagerService;

    private InstallationManager         mockInstallationManager;
    private HttpTransport               mockTransport;
    private Artifact                    mockInstallManagerArtifact;
    private Artifact                    mockCdecArtifact;
    private UserCredentials             testCredentials;

    @BeforeMethod
    public void init() {
        initMocks();
        installationManagerService = new InstallationManagerServiceImpl(mockInstallationManager, mockTransport, new DownloadingDescriptorHolder());
        testCredentials = new UserCredentials("auth token");
    }

    public void initMocks() {
        mockTransport = mock(HttpTransport.class);
        mockInstallationManager = mock(InstallationManagerImpl.class);
        mockInstallManagerArtifact = spy(new InstallManagerArtifact());
        mockCdecArtifact = spy(new CDECArtifact("update/endpoint", mockTransport));
    }

    @Test
    public void testInstall() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(mockCdecArtifact, "2.10.5");
                put(mockInstallManagerArtifact, "1.0.1");
            }
        }).when(mockInstallationManager).getUpdates(testCredentials.getToken());
        doReturn(Collections.emptyMap()).when(mockInstallationManager).getInstalledArtifacts(testCredentials.getToken());

        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(testCredentials);
        String response = installationManagerService.install(userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [\n" +
                                                      "    {\n" +
                                                      "      \"artifact\": \"cdec\",\n" +
                                                      "      \"status\": \"SUCCESS\",\n" +
                                                      "      \"version\": \"2.10.5\"\n" +
                                                      "    },\n" +
                                                      "    {\n" +
                                                      "      \"artifact\": \"installation-manager\",\n" +
                                                      "      \"status\": \"SUCCESS\",\n" +
                                                      "      \"version\": \"1.0.1\"\n" +
                                                      "    }\n" +
                                                      "  ],\n" +
                                                      "  \"status\": \"OK\"\n" +
                                                      "}");
    }

    @Test
    public void testInstallUpdateAll() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(mockCdecArtifact, "2.10.5");
                put(mockInstallManagerArtifact, "1.0.1");
            }
        }).when(mockInstallationManager).getUpdates(testCredentials.getToken());
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(mockCdecArtifact, "2.10.4");
                put(mockInstallManagerArtifact, "1.0");
            }
        }).when(mockInstallationManager).getInstalledArtifacts(testCredentials.getToken());

        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(testCredentials);
        String response = installationManagerService.install(userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [\n" +
                                                      "    {\n" +
                                                      "      \"artifact\": \"cdec\",\n" +
                                                      "      \"status\": \"SUCCESS\",\n" +
                                                      "      \"version\": \"2.10.5\"\n" +
                                                      "    },\n" +
                                                      "    {\n" +
                                                      "      \"artifact\": \"installation-manager\",\n" +
                                                      "      \"status\": \"SUCCESS\",\n" +
                                                      "      \"version\": \"1.0.1\"\n" +
                                                      "    }\n" +
                                                      "  ],\n" +
                                                      "  \"status\": \"OK\"\n" +
                                                      "}");
    }

    @Test
    public void testInstallErrorIfTryToInstallLowerVersions() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(mockCdecArtifact, "2.10.5");
                put(mockInstallManagerArtifact, "1.0.1");
            }
        }).when(mockInstallationManager).getUpdates(testCredentials.getToken());
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.6");
                put(createArtifact(mockInstallManagerArtifact.getName()), "1.0");
            }
        }).when(mockInstallationManager).getInstalledArtifacts(testCredentials.getToken());
        doThrow(new IllegalStateException(
                "Can not install the artifact '" + mockCdecArtifact.getName() + ":2.10.5', because we don't support downgrade artifacts."))
                .when(mockInstallationManager).install(testCredentials.getToken(), mockCdecArtifact, "2.10.5");

        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(testCredentials);
        String response = installationManagerService.install(userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [{\n" +
                                                      "    \"artifact\": \"cdec\",\n" +
                                                      "    \"status\": \"FAILURE\",\n" +
                                                      "    \"version\": \"2.10.5\"\n" +
                                                      "  }],\n" +
                                                      "  \"message\": \"Can not install the artifact 'cdec:2.10.5', because we don't support downgrade artifacts.\",\n" +
                                                      "  \"status\": \"ERROR\"\n" +
                                                      "}");
    }

    @Test
    public void testInstallUpdateSpecificArtifact() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.5");
            }
        }).when(mockInstallationManager).getUpdates(testCredentials.getToken());
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.4");
            }
        }).when(mockInstallationManager).getInstalledArtifacts(testCredentials.getToken());

        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(testCredentials);
        String response = installationManagerService.install(mockCdecArtifact.getName(), userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [{\n" +
                                                      "    \"artifact\": \"cdec\",\n" +
                                                      "    \"status\": \"SUCCESS\",\n" +
                                                      "    \"version\": \"2.10.5\"\n" +
                                                      "  }],\n" +
                                                      "  \"status\": \"OK\"\n" +
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

        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(testCredentials);
        String response = installationManagerService.install(mockCdecArtifact.getName(), userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [{\n" +
                                                      "    \"artifact\": \"cdec\",\n" +
                                                      "    \"status\": \"SUCCESS\",\n" +
                                                      "    \"version\": \"2.10.5\"\n" +
                                                      "  }],\n" +
                                                      "  \"status\": \"OK\"\n" +
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
                .when(mockInstallationManager).install(testCredentials.getToken(), artifact, "2.10.5");

        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(testCredentials);
        String response = installationManagerService.install(mockCdecArtifact.getName(), userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [{\n" +
                                                      "    \"artifact\": \"cdec\",\n" +
                                                      "    \"status\": \"FAILURE\",\n" +
                                                      "    \"version\": \"2.10.5\"\n" +
                                                      "  }],\n" +
                                                      "  \"message\": \"Can not install the artifact 'cdec:2.10.5', because we don't support downgrade artifacts.\",\n" +
                                                      "  \"status\": \"ERROR\"\n" +
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

        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(testCredentials);
        String response = installationManagerService.install(mockCdecArtifact.getName(), userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"message\": \"Artifact 'cdec' isn't available to update.\",\n" +
                                                      "  \"status\": \"ERROR\"\n" +
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

        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(testCredentials);
        String response = installationManagerService.install(mockCdecArtifact.getName(), userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [{\n" +
                                                      "    \"artifact\": \"cdec\",\n" +
                                                      "    \"status\": \"SUCCESS\",\n" +
                                                      "    \"version\": \"2.10.5\"\n" +
                                                      "  }],\n" +
                                                      "  \"status\": \"OK\"\n" +
                                                      "}");
    }

    @Test
    public void testInstallUpdateSpecificVersionArtifact() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.5");
            }
        }).when(mockInstallationManager).getUpdates(testCredentials.getToken());
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.4");
            }
        }).when(mockInstallationManager).getInstalledArtifacts(testCredentials.getToken());

        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(testCredentials);
        String response = installationManagerService.install(mockCdecArtifact.getName(), "2.10.5", userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [{\n" +
                                                      "    \"artifact\": \"cdec\",\n" +
                                                      "    \"status\": \"SUCCESS\",\n" +
                                                      "    \"version\": \"2.10.5\"\n" +
                                                      "  }],\n" +
                                                      "  \"status\": \"OK\"\n" +
                                                      "}");
    }

    @Test
    public void testInstallSpecificVersionArtifactDoNothing() throws Exception {
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

        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(testCredentials);
        String response = installationManagerService.install(mockCdecArtifact.getName(), "2.10.5", userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [{\n" +
                                                      "    \"artifact\": \"cdec\",\n" +
                                                      "    \"status\": \"SUCCESS\",\n" +
                                                      "    \"version\": \"2.10.5\"\n" +
                                                      "  }],\n" +
                                                      "  \"status\": \"OK\"\n" +
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
                .when(mockInstallationManager).install(testCredentials.getToken(), artifact, "2.10.5");

        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(testCredentials);
        String response = installationManagerService.install(mockCdecArtifact.getName(), "2.10.5", userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [{\n" +
                                                      "    \"artifact\": \"cdec\",\n" +
                                                      "    \"status\": \"FAILURE\",\n" +
                                                      "    \"version\": \"2.10.5\"\n" +
                                                      "  }],\n" +
                                                      "  \"message\": \"Can not install the artifact 'cdec:2.10.5', because we don't support downgrade artifacts.\",\n" +
                                                      "  \"status\": \"ERROR\"\n" +
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
                .when(mockInstallationManager).install(testCredentials.getToken(), artifact, "2.10.7");

        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(testCredentials);
        String response = installationManagerService.install(mockCdecArtifact.getName(), "2.10.7", userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [{\n" +
                                                      "    \"artifact\": \"cdec\",\n" +
                                                      "    \"status\": \"FAILURE\",\n" +
                                                      "    \"version\": \"2.10.7\"\n" +
                                                      "  }],\n" +
                                                      "  \"message\": \"Artifact 'cdec'  isn't available to update.\",\n" +
                                                      "  \"status\": \"ERROR\"\n" +
                                                      "}");
    }

    @Test
    public void testInstallSpecificVersionArtifact() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.5");
            }
        }).when(mockInstallationManager).getUpdates(testCredentials.getToken());
        doReturn(Collections.emptyMap()).when(mockInstallationManager).getInstalledArtifacts(testCredentials.getToken());

        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(testCredentials);
        String response = installationManagerService.install(mockCdecArtifact.getName(), "2.10.5", userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [{\n" +
                                                      "    \"artifact\": \"cdec\",\n" +
                                                      "    \"status\": \"SUCCESS\",\n" +
                                                      "    \"version\": \"2.10.5\"\n" +
                                                      "  }],\n" +
                                                      "  \"status\": \"OK\"\n" +
                                                      "}");
    }
}