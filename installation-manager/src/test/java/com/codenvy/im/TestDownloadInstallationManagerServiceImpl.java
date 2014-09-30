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
import com.codenvy.im.exceptions.ArtifactNotFoundException;
import com.codenvy.im.exceptions.AuthenticationException;
import com.codenvy.im.restlet.InstallationManager;
import com.codenvy.im.restlet.InstallationManagerService;
import com.codenvy.im.user.UserCredentials;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;

import org.restlet.ext.jackson.JacksonRepresentation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.codenvy.im.utils.Commons.getPrettyPrintingJson;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 */
public class TestDownloadInstallationManagerServiceImpl {
    private InstallationManagerService installationManagerService;

    private InstallationManager mockInstallationManager;
    private HttpTransport       transport;
    private Artifact            installManagerArtifact;
    private Artifact            cdecArtifact;
    private UserCredentials     testCredentials;

    @BeforeMethod
    public void init() {
        initMocks();
        installationManagerService = new InstallationManagerServiceImpl(mockInstallationManager, transport);
    }

    public void initMocks() {
        mockInstallationManager = mock(InstallationManagerImpl.class);
        transport = mock(HttpTransport.class);
        installManagerArtifact = ArtifactFactory.createArtifact(InstallManagerArtifact.NAME);
        cdecArtifact = ArtifactFactory.createArtifact(CDECArtifact.NAME);
        testCredentials = new UserCredentials("auth token", "accountId");
    }

    @Test
    public void testDownload() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(cdecArtifact, "2.10.5");
                put(installManagerArtifact, "1.0.1");
            }
        }).when(mockInstallationManager).getUpdates(testCredentials.getToken());
        doReturn(Paths.get("cdec.zip")).when(mockInstallationManager).download(testCredentials, cdecArtifact, "2.10.5");
        doReturn(Paths.get("im.zip")).when(mockInstallationManager).download(testCredentials, installManagerArtifact, "1.0.1");

        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(testCredentials);
        String response = installationManagerService.download(userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [\n" +
                                                      "    {\n" +
                                                      "      \"artifact\": \"cdec\",\n" +
                                                      "      \"file\": \"cdec.zip\",\n" +
                                                      "      \"status\": \"SUCCESS\",\n" +
                                                      "      \"version\": \"2.10.5\"\n" +
                                                      "    },\n" +
                                                      "    {\n" +
                                                      "      \"artifact\": \"installation-manager\",\n" +
                                                      "      \"file\": \"im.zip\",\n" +
                                                      "      \"status\": \"SUCCESS\",\n" +
                                                      "      \"version\": \"1.0.1\"\n" +
                                                      "    }\n" +
                                                      "  ],\n" +
                                                      "  \"status\": \"OK\"\n" +
                                                      "}");
    }
    
    @Test
    public void testDownloadSpecificArtifact() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(cdecArtifact, "2.10.5");
                put(installManagerArtifact, "1.0.1");
            }
        }).when(mockInstallationManager).getUpdates(testCredentials.getToken());
        doReturn(Paths.get("cdec.zip")).when(mockInstallationManager).download(testCredentials, cdecArtifact, "2.10.5");

        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(testCredentials);
        String response = installationManagerService.download("cdec", userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [{\n" +
                                                      "    \"artifact\": \"cdec\",\n" +
                                                      "    \"file\": \"cdec.zip\",\n" +
                                                      "    \"status\": \"SUCCESS\",\n" +
                                                      "    \"version\": \"2.10.5\"\n" +
                                                      "  }],\n" +
                                                      "  \"status\": \"OK\"\n" +
                                                      "}");
    }

    @Test
    public void testDownloadSpecificVersionArtifact() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(cdecArtifact, "2.10.5");
                put(installManagerArtifact, "1.0.1");
            }
        }).when(mockInstallationManager).getUpdates(testCredentials.getToken());
        doReturn(Paths.get("cdec.zip")).when(mockInstallationManager).download(testCredentials, cdecArtifact, "2.10.5");

        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(testCredentials);
        String response = installationManagerService.download("cdec", "2.10.5", userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [{\n" +
                                                      "    \"artifact\": \"cdec\",\n" +
                                                      "    \"file\": \"cdec.zip\",\n" +
                                                      "    \"status\": \"SUCCESS\",\n" +
                                                      "    \"version\": \"2.10.5\"\n" +
                                                      "  }],\n" +
                                                      "  \"status\": \"OK\"\n" +
                                                      "}");                                                      
    }
    
    @Test
    public void testDownloadErrorIfUpdatesAbsent() throws Exception {
        doReturn(Collections.emptyMap()).when(mockInstallationManager).getUpdates(testCredentials.getToken());

        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(testCredentials);
        
        String response = installationManagerService.download(userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [],\n" +
                                                      "  \"status\": \"OK\"\n" +
                                                      "}");

        response = installationManagerService.download("cdec", userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"message\": \"There is no any version of artifact 'cdec'\",\n" +
                                                      "  \"status\": \"ERROR\"\n" +
                                                      "}");

        response = installationManagerService.download("unknown", userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"message\": \"Artifact 'unknown' not found\",\n" +
                                                      "  \"status\": \"ERROR\"\n" +
                                                      "}");
    }
    
    @Test
    public void testDownloadErrorIfSpecificVertionArtifactAbsent() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(cdecArtifact, "2.10.5");
            }
        }).when(mockInstallationManager).getUpdates(testCredentials.getToken());

        doThrow(new ArtifactNotFoundException("cdec", "2.10.4")).when(mockInstallationManager).download(testCredentials, cdecArtifact, "2.10.4");

        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(testCredentials);
        String response = installationManagerService.download("cdec", "2.10.4", userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [{\n" +
                                                      "    \"artifact\": \"cdec\",\n" +
                                                      "    \"status\": \"FAILURE\",\n" +
                                                      "    \"version\": \"2.10.4\"\n" +
                                                      "  }],\n" +
                                                      "  \"message\": \"Artifact 'cdec' version '2.10.4' not found\",\n" +
                                                      "  \"status\": \"ERROR\"\n" +
                                                      "}");
    }
    
    @Test
    public void testDownloadErrorIfAuthenticationFailed() throws Exception {
        when(mockInstallationManager.getUpdates(anyString())).thenThrow(new AuthenticationException());        
        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(testCredentials);
        
        String response = installationManagerService.download(userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"message\": \"Authentication error. Authentication token might be expired or invalid.\",\n" +
                                                      "  \"status\": \"ERROR\"\n" +
                                                      "}");
        
        doThrow(new AuthenticationException()).when(mockInstallationManager).download(testCredentials, cdecArtifact, "2.10.5");
        response = installationManagerService.download("cdec", "2.10.5", userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [{\n" +
                                                      "    \"artifact\": \"cdec\",\n" +
                                                      "    \"status\": \"FAILURE\",\n" +
                                                      "    \"version\": \"2.10.5\"\n" +
                                                      "  }],\n" +
                                                      "  \"message\": \"Authentication error. Authentication token might be expired or invalid.\"," +
                                                      "\n" +
                                                      "  \"status\": \"ERROR\"\n" +
                                                      "}");
    }
    
    @Test
    public void testDownloadErrorIfSubscriptionVerificationFailed() throws Exception {
        when(mockInstallationManager.getUpdates(anyString())).thenThrow(new IllegalStateException("Valid subscription is required to download cdec"));
        
        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(testCredentials);
        String response = installationManagerService.download("cdec", userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"message\": \"Valid subscription is required to download cdec\",\n" +
                                                      "  \"status\": \"ERROR\"\n" +
                                                      "}");
    }

    @Test
    public void testGetDownloads() throws Exception {
        doReturn(new HashMap<Artifact, SortedMap<Version, Path>>() {{
            put(cdecArtifact, new TreeMap<Version, Path>() {{
                put(Version.valueOf("1.0.1"), Paths.get("target/file1"));
                put(Version.valueOf("1.0.2"), Paths.get("target/file2"));
            }});
        }}).when(mockInstallationManager).getDownloadedArtifacts();

        String response = installationManagerService.getDownloads();
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [\n" +
                                                      "    {\n" +
                                                      "      \"artifact\": \"cdec\",\n" +
                                                      "      \"file\": \"target/file1\",\n" +
                                                      "      \"status\": \"DOWNLOADED\",\n" +
                                                      "      \"version\": \"1.0.1\"\n" +
                                                      "    },\n" +
                                                      "    {\n" +
                                                      "      \"artifact\": \"cdec\",\n" +
                                                      "      \"file\": \"target/file2\",\n" +
                                                      "      \"status\": \"DOWNLOADED\",\n" +
                                                      "      \"version\": \"1.0.2\"\n" +
                                                      "    }\n" +
                                                      "  ],\n" +
                                                      "  \"status\": \"OK\"\n" +
                                                      "}");
    }

    @Test
    public void testGetDownloadsSpecificArtifact() throws Exception {
        doReturn(new HashMap<Artifact, SortedMap<Version, Path>>() {{
            put(cdecArtifact, new TreeMap<Version, Path>() {{
                put(Version.valueOf("1.0.1"), Paths.get("target/file1"));
                put(Version.valueOf("1.0.2"), Paths.get("target/file2"));
            }});
            put(installManagerArtifact, new TreeMap<Version, Path>() {{
                put(Version.valueOf("2.0.0"), Paths.get("target/file3"));
            }});
        }}).when(mockInstallationManager).getDownloadedArtifacts();

        String response = installationManagerService.getDownloads(installManagerArtifact.getName());
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [{\n" +
                                                      "    \"artifact\": \"installation-manager\",\n" +
                                                      "    \"file\": \"target/file3\",\n" +
                                                      "    \"status\": \"DOWNLOADED\",\n" +
                                                      "    \"version\": \"2.0.0\"\n" +
                                                      "  }],\n" +
                                                      "  \"status\": \"OK\"\n" +
                                                      "}");
    }
}