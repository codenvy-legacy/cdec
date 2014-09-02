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

import com.codenvy.cdec.artifacts.Artifact;
import com.codenvy.cdec.artifacts.CDECArtifact;
import com.codenvy.cdec.artifacts.InstallManagerArtifact;
import com.codenvy.cdec.restlet.InstallationManager;
import com.codenvy.cdec.restlet.InstallationManagerService;
import com.codenvy.cdec.utils.HttpTransport;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.LinkedHashMap;

import static com.codenvy.cdec.artifacts.ArtifactFactory.createArtifact;
import static com.codenvy.cdec.utils.Commons.getPrettyPrintingJson;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 */
public class TestInstallInstallationManagerServiceImpl {
    private InstallationManagerService installationManagerService;

    private InstallationManager mockInstallationManager;
    private HttpTransport       mockTransport;
    private Artifact            mockInstallManagerArtifact;
    private Artifact            mockCdecArtifact;

    @BeforeMethod
    public void init() {
        initMocks();
        installationManagerService = new InstallationManagerServiceImpl(mockInstallationManager);
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
        }).when(mockInstallationManager).getUpdates("auth token");
        doReturn(Collections.emptyMap()).when(mockInstallationManager).getInstalledArtifacts("auth token");

        String response = installationManagerService.install("auth token");
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
        }).when(mockInstallationManager).getUpdates("auth token");
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(mockCdecArtifact, "2.10.4");
                put(mockInstallManagerArtifact, "1.0");
            }
        }).when(mockInstallationManager).getInstalledArtifacts("auth token");

        String response = installationManagerService.install("auth token");
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
        }).when(mockInstallationManager).getUpdates("auth token");
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.6");
                put(createArtifact(mockInstallManagerArtifact.getName()), "1.0");
            }
        }).when(mockInstallationManager).getInstalledArtifacts("auth token");
        doThrow(new IllegalStateException(
                "Can not install the artifact '" + mockCdecArtifact.getName() + ":2.10.5', because we don't support downgrade artifacts."))
                .when(mockInstallationManager).install("auth token", mockCdecArtifact, "2.10.5");

        String response = installationManagerService.install("auth token");
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
        }).when(mockInstallationManager).getUpdates("auth token");
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.4");
            }
        }).when(mockInstallationManager).getInstalledArtifacts("auth token");

        String response = installationManagerService.install(mockCdecArtifact.getName(), "auth token");
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
        }).when(mockInstallationManager).getUpdates("auth token");
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.5");
            }
        }).when(mockInstallationManager).getInstalledArtifacts("auth token");


        String response = installationManagerService.install(mockCdecArtifact.getName(), "auth token");
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
        }).when(mockInstallationManager).getUpdates("auth token");
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.6");
            }
        }).when(mockInstallationManager).getInstalledArtifacts("auth token");

        Artifact artifact = createArtifact(mockCdecArtifact.getName());
        doThrow(new IllegalStateException(
                "Can not install the artifact '" + mockCdecArtifact.getName() + ":2.10.5', because we don't support downgrade artifacts."))
                .when(mockInstallationManager).install("auth token", artifact, "2.10.5");

        String response = installationManagerService.install(mockCdecArtifact.getName(), "auth token");
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
        doReturn(Collections.emptyMap()).when(mockInstallationManager).getUpdates("auth token");
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.6");
            }
        }).when(mockInstallationManager).getInstalledArtifacts("auth token");

        String response = installationManagerService.install(mockCdecArtifact.getName(), "auth token");
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
        }).when(mockInstallationManager).getUpdates("auth token");
        doReturn(new LinkedHashMap<Artifact, String>() {
        }).when(mockInstallationManager).getInstalledArtifacts("auth token");


        String response = installationManagerService.install(mockCdecArtifact.getName(), "auth token");
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
        }).when(mockInstallationManager).getUpdates("auth token");
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.4");
            }
        }).when(mockInstallationManager).getInstalledArtifacts("auth token");

        String response = installationManagerService.install(mockCdecArtifact.getName(), "2.10.5", "auth token");
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
        }).when(mockInstallationManager).getUpdates("auth token");
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.5");
            }
        }).when(mockInstallationManager).getInstalledArtifacts("auth token");


        String response = installationManagerService.install(mockCdecArtifact.getName(), "2.10.5", "auth token");
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
        }).when(mockInstallationManager).getUpdates("auth token");
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.6");
            }
        }).when(mockInstallationManager).getInstalledArtifacts("auth token");

        Artifact artifact = createArtifact(mockCdecArtifact.getName());
        doThrow(new IllegalStateException(
                "Can not install the artifact '" + mockCdecArtifact.getName() + ":2.10.5', because we don't support downgrade artifacts."))
                .when(mockInstallationManager).install("auth token", artifact, "2.10.5");

        String response = installationManagerService.install(mockCdecArtifact.getName(), "2.10.5", "auth token");
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
        doReturn(Collections.emptyMap()).when(mockInstallationManager).getUpdates("auth token");
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.6");
            }
        }).when(mockInstallationManager).getInstalledArtifacts("auth token");

        Artifact artifact = createArtifact(mockCdecArtifact.getName());
        doThrow(new IllegalStateException(
                "Artifact '" + mockCdecArtifact.getName() + "'  isn't available to update."))
                .when(mockInstallationManager).install("auth token", artifact, "2.10.7");

        String response = installationManagerService.install(mockCdecArtifact.getName(), "2.10.7", "auth token");
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
        }).when(mockInstallationManager).getUpdates("auth token");
        doReturn(Collections.emptyMap()).when(mockInstallationManager).getInstalledArtifacts("auth token");


        String response = installationManagerService.install(mockCdecArtifact.getName(), "2.10.5", "auth token");
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