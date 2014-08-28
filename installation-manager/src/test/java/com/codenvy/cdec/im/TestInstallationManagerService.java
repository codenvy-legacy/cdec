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

import com.codenvy.cdec.ArtifactNotFoundException;
import com.codenvy.cdec.AuthenticationException;
import com.codenvy.cdec.artifacts.Artifact;
import com.codenvy.cdec.artifacts.CDECArtifact;
import com.codenvy.cdec.artifacts.InstallManagerArtifact;
import com.codenvy.cdec.restlet.InstallationManager;
import com.codenvy.cdec.restlet.InstallationManagerService;
import com.codenvy.cdec.utils.HttpTransport;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.LinkedHashMap;

import static com.codenvy.cdec.artifacts.ArtifactFactory.createArtifact;
import static com.codenvy.cdec.utils.Commons.getPrettyPrintingJson;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 */
public class TestInstallationManagerService {
    private InstallationManagerService installationManagerService;

    private InstallationManager mockInstallationManager;
    private HttpTransport       mockTransport;
    private Artifact            mockInstallManagerArtifact;
    private Artifact            mockCdecArtifact;

    @BeforeMethod
    public void init() {
        initMocks();
        installationManagerService = new InstallationManagerServiceTestImpl(mockInstallationManager);
    }

    public void initMocks() {
        mockTransport = mock(HttpTransport.class);
        mockInstallationManager = mock(InstallationManagerImpl.class);
        mockInstallManagerArtifact = spy(new InstallManagerArtifact());
        mockCdecArtifact = spy(new CDECArtifact("update/endpoint", mockTransport));
    }

    @Test
    public void testGetUpdates() throws Exception {
        when(mockInstallationManager.getUpdates(anyString())).thenReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(mockInstallManagerArtifact, "1.0.1");
                put(mockCdecArtifact, "2.10.5");
            }
        });

        String response = installationManagerService.getUpdates("");
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [\n" +
                                                      "    {\n" +
                                                      "      \"artifact\": \"installation-manager\",\n" +
                                                      "      \"version\": \"1.0.1\"\n" +
                                                      "    },\n" +
                                                      "    {\n" +
                                                      "      \"artifact\": \"cdec\",\n" +
                                                      "      \"version\": \"2.10.5\"\n" +
                                                      "    }\n" +
                                                      "  ],\n" +
                                                      "  \"status\": \"OK\"\n" +
                                                      "}");
    }

    @Test
    public void testGetUpdatesCatchesAuthenticationException() throws Exception {
        when(mockInstallationManager.getUpdates(anyString())).thenThrow(new AuthenticationException());
        String response = installationManagerService.getUpdates("incorrect-token");
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"message\": \"Incorrect login.\",\n" +
                                                      "  \"status\": \"ERROR\"\n" +
                                                      "}");
    }

    @Test
    public void testGetUpdatesCatchesArtifactNotFoundException() throws Exception {
        when(mockInstallationManager.getUpdates(anyString())).thenThrow(new ArtifactNotFoundException("cdec"));
        String response = installationManagerService.getUpdates("");
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"message\": \"There is no any version of artifact 'cdec'\",\n" +
                                                      "  \"status\": \"OK\"\n" +
                                                      "}");
    }

    @Test
    public void testGetUpdatesCatchesException() throws Exception {
        when(mockInstallationManager.getUpdates(anyString())).thenThrow(new IOException("Error"));
        String response = installationManagerService.getUpdates("incorrect-token");
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"message\": \"" + InstallationManagerServiceImpl.COMMON_ERROR_MESSAGE + "\",\n" +
                                                      "  \"status\": \"ERROR\"\n" +
                                                      "}");
    }
    
    @Test
    public void testDownloadArtifact() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(mockCdecArtifact, "2.10.5");
                put(mockInstallManagerArtifact, "1.0.1");
            }
        }).when(mockInstallationManager).getUpdates();

        String response = installationManagerService.download();
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

    public class InstallationManagerServiceTestImpl extends InstallationManagerServiceImpl {
        public InstallationManagerServiceTestImpl(InstallationManager manager) {
            this.manager = manager;
        }
    }

    @Test
    public void testInstallAllArtifactsAllNew() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(mockCdecArtifact, "2.10.5");
                put(mockInstallManagerArtifact, "1.0.1");
            }
        }).when(mockInstallationManager).getUpdates();
        doReturn(new LinkedHashMap<Artifact, String>() {
        }).when(mockInstallationManager).getInstalledArtifacts();

        String response = installationManagerService.install("");
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
    public void testInstallAllArtifactsUpdateAll() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(mockCdecArtifact, "2.10.5");
                put(mockInstallManagerArtifact, "1.0.1");
            }
        }).when(mockInstallationManager).getUpdates();
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(mockCdecArtifact, "2.10.4");
                put(mockInstallManagerArtifact, "1.0");
            }
        }).when(mockInstallationManager).getInstalledArtifacts();


        String response = installationManagerService.install("");
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
    public void testInstallAllErrorDowngradeNotSupport() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(mockCdecArtifact, "2.10.5");
                put(mockInstallManagerArtifact, "1.0.1");
            }
        }).when(mockInstallationManager).getUpdates();
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.6");
                put(createArtifact(mockInstallManagerArtifact.getName()), "1.0");
            }
        }).when(mockInstallationManager).getInstalledArtifacts();
        doThrow(new IllegalStateException(
                "Can not install the artifact '" + mockCdecArtifact.getName() + ":2.10.5', because we don't support downgrade artifacts."))
        .when(mockInstallationManager).install(mockCdecArtifact, "2.10.5");

        String response = installationManagerService.install("");
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
    public void testInstallSpecificArtifact() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.5");
            }
        }).when(mockInstallationManager).getUpdates();
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.4");
            }
        }).when(mockInstallationManager).getInstalledArtifacts();

        String response = installationManagerService.install(mockCdecArtifact.getName(), "");
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
        }).when(mockInstallationManager).getUpdates();
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.5");
            }
        }).when(mockInstallationManager).getInstalledArtifacts();


        String response = installationManagerService.install(mockCdecArtifact.getName(), "");
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
    public void testInstallErrorIfSpecificArtifactDowngradeNotSupport() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.5");
            }
        }).when(mockInstallationManager).getUpdates();
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.6");
            }
        }).when(mockInstallationManager).getInstalledArtifacts();

        Artifact artifact = createArtifact(mockCdecArtifact.getName());
        doThrow(new IllegalStateException(
                "Can not install the artifact '" + mockCdecArtifact.getName() + ":2.10.5', because we don't support downgrade artifacts."))
                .when(mockInstallationManager).install(artifact, "2.10.5");

        String response = installationManagerService.install(mockCdecArtifact.getName(), "");
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
    public void testInstallErrorIfSpecificArtifactIsNoUpdates() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
        }).when(mockInstallationManager).getUpdates();
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.6");
            }
        }).when(mockInstallationManager).getInstalledArtifacts();

        String response = installationManagerService.install(mockCdecArtifact.getName(), "");
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"message\": \"Artifact 'cdec' isn't available to update.\",\n" +
                                                      "  \"status\": \"ERROR\"\n" +
                                                      "}");
    }

    @Test
    public void testInstallNewSpecificArtifact() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.5");
            }
        }).when(mockInstallationManager).getUpdates();
        doReturn(new LinkedHashMap<Artifact, String>() {
        }).when(mockInstallationManager).getInstalledArtifacts();


        String response = installationManagerService.install(mockCdecArtifact.getName(), "");
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
    public void testInstallSpecificVersionArtifact() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.5");
            }
        }).when(mockInstallationManager).getUpdates();
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.4");
            }
        }).when(mockInstallationManager).getInstalledArtifacts();

        String response = installationManagerService.install(mockCdecArtifact.getName(), "2.10.5", "");
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
        }).when(mockInstallationManager).getUpdates();
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.5");
            }
        }).when(mockInstallationManager).getInstalledArtifacts();


        String response = installationManagerService.install(mockCdecArtifact.getName(), "2.10.5", "");
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
    public void testInstallErrorIfSpecificVersionArtifactDowngradeNotSupport() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.5");
            }
        }).when(mockInstallationManager).getUpdates();
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.6");
            }
        }).when(mockInstallationManager).getInstalledArtifacts();

        Artifact artifact = createArtifact(mockCdecArtifact.getName());
        doThrow(new IllegalStateException(
                "Can not install the artifact '" + mockCdecArtifact.getName() + ":2.10.5', because we don't support downgrade artifacts."))
                .when(mockInstallationManager).install(artifact, "2.10.5");

        String response = installationManagerService.install(mockCdecArtifact.getName(), "2.10.5", "");
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
    public void testInstallErrorIfSpecificVersionArtifactIsNoUpdates() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
        }).when(mockInstallationManager).getUpdates();
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.6");
            }
        }).when(mockInstallationManager).getInstalledArtifacts();

        Artifact artifact = createArtifact(mockCdecArtifact.getName());
        doThrow(new IllegalStateException(
                "Artifact '" + mockCdecArtifact.getName() + "'  isn't available to update."))
                .when(mockInstallationManager).install(artifact, "2.10.7");

        String response = installationManagerService.install(mockCdecArtifact.getName(), "2.10.7", "");
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
    public void testInstallNewSpecificVersionArtifact() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(createArtifact(mockCdecArtifact.getName()), "2.10.5");
            }
        }).when(mockInstallationManager).getUpdates();
        doReturn(new LinkedHashMap<Artifact, String>() {
        }).when(mockInstallationManager).getInstalledArtifacts();


        String response = installationManagerService.install(mockCdecArtifact.getName(), "2.10.5", "");
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