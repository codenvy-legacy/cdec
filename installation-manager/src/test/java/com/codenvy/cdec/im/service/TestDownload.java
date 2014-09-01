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
package com.codenvy.cdec.im.service;

import static com.codenvy.cdec.utils.Commons.getPrettyPrintingJson;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.util.LinkedHashMap;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.codenvy.cdec.ArtifactNotFoundException;
import com.codenvy.cdec.AuthenticationException;
import com.codenvy.cdec.artifacts.Artifact;
import com.codenvy.cdec.artifacts.ArtifactFactory;
import com.codenvy.cdec.artifacts.CDECArtifact;
import com.codenvy.cdec.artifacts.InstallManagerArtifact;
import com.codenvy.cdec.im.InstallationManagerImpl;
import com.codenvy.cdec.restlet.InstallationManager;
import com.codenvy.cdec.restlet.InstallationManagerService;

/**
 * @author Dmytro Nochevnov
 */
public class TestDownload {
    private InstallationManagerService installationManagerService;

    private InstallationManager mockInstallationManager;
    private Artifact            installManagerArtifact;
    private Artifact            cdecArtifact;

    @BeforeMethod
    public void init() {
        initMocks();
        installationManagerService = new InstallationManagerServiceTestImpl(mockInstallationManager);
    }

    public void initMocks() {
        mockInstallationManager = mock(InstallationManagerImpl.class);
        installManagerArtifact = ArtifactFactory.createArtifact(InstallManagerArtifact.NAME);
        cdecArtifact = ArtifactFactory.createArtifact(CDECArtifact.NAME);
    }
    
    @Test
    public void testDownload() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(cdecArtifact, "2.10.5");
                put(installManagerArtifact, "1.0.1");
            }
        }).when(mockInstallationManager).getUpdates("");

        String response = installationManagerService.download("");
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
    public void testDownloadArtifact() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(cdecArtifact, "2.10.5");
                put(installManagerArtifact, "1.0.1");
            }
        }).when(mockInstallationManager).getUpdates("");

        String response = installationManagerService.download("", "cdec");
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
    public void testDownloadVersionOfArtifact() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(cdecArtifact, "2.10.5");
                put(installManagerArtifact, "1.0.1");
            }
        }).when(mockInstallationManager).getUpdates("");

        String response = installationManagerService.download("", "cdec", "2.10.5");
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
    public void testDownloadNonExistsArtifact() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>())
        .when(mockInstallationManager)
        .getUpdates("");

        String response = installationManagerService.download("");
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [],\n" +
                                                      "  \"status\": \"OK\"\n" +
                                                      "}");
        
        response = installationManagerService.download("", "cdec");
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"message\": \"There is no any version of artifact 'cdec'\",\n" +
                                                      "  \"status\": \"ERROR\"\n" +
                                                      "}");
        
        response = installationManagerService.download("", "unknown");
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"message\": \"Artifact 'unknown' not found\",\n" +
                                                      "  \"status\": \"ERROR\"\n" +
                                                      "}");
    }
    
    @Test
    public void testDownloadNonExistsVersion() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(cdecArtifact, "2.10.5");
            }
        }).when(mockInstallationManager).getUpdates("");
        
        doThrow(new ArtifactNotFoundException("cdec", "2.10.4")).when(mockInstallationManager).download("", cdecArtifact, "2.10.4");        
        
        String response = installationManagerService.download("", "cdec", "2.10.4");
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"message\": \"Artifact 'cdec' version '2.10.4' not found\",\n" +
                                                      "  \"status\": \"ERROR\"\n" +
                                                      "}");        
    }
    
    @Test
    public void testDownloadCatchesAuthenticationException() throws Exception {
        when(mockInstallationManager.getUpdates(anyString())).thenThrow(new AuthenticationException());
        String response = installationManagerService.download("");
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"message\": \"Incorrect login.\",\n" +
                                                      "  \"status\": \"ERROR\"\n" +
                                                      "}");
        
        doThrow(AuthenticationException.class).when(mockInstallationManager).download("", cdecArtifact, "2.10.5");
        response = installationManagerService.download("", "cdec", "2.10.5");
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"message\": \"Incorrect login.\",\n" +
                                                      "  \"status\": \"ERROR\"\n" +
                                                      "}");
    }
}