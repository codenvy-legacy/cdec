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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.IOException;
import java.util.LinkedHashMap;

import org.powermock.api.mockito.PowerMockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.codenvy.cdec.ArtifactNotFoundException;
import com.codenvy.cdec.AuthenticationException;
import com.codenvy.cdec.artifacts.Artifact;
import com.codenvy.cdec.artifacts.ArtifactFactory;
import com.codenvy.cdec.artifacts.CDECArtifact;
import com.codenvy.cdec.artifacts.InstallManagerArtifact;
import com.codenvy.cdec.im.InstallationManagerImpl;
import com.codenvy.cdec.im.InstallationManagerServiceImpl;
import com.codenvy.cdec.restlet.InstallationManager;
import com.codenvy.cdec.restlet.InstallationManagerService;
import com.codenvy.cdec.utils.HttpTransport;
import com.codenvy.cdec.utils.InjectorBootstrap;

/**
 * @author Dmytro Nochevnov
 */
public class TestGetUpdates {
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
        mockInstallationManager = PowerMockito.mock(InstallationManagerImpl.class);
        installManagerArtifact = ArtifactFactory.createArtifact(InstallManagerArtifact.NAME);
        cdecArtifact = ArtifactFactory.createArtifact(CDECArtifact.NAME);
    }

    @Test
    public void testGetUpdates() throws Exception {
        when(mockInstallationManager.getUpdates(anyString())).thenReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(installManagerArtifact, "1.0.1");
                put(cdecArtifact, "2.10.5");
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
    public void testGetUpdateServerUrl() {
     // TODO fix test error "'install' is a *void method* and it *cannot* be stubbed with a *return value*!"
//        PowerMockito.mockStatic(InjectorBootstrap.class);
//        PowerMockito.when(InjectorBootstrap.getProperty("codenvy.installation-manager.update_endpoint"))
//                    .thenReturn("https://codenvy-test.com/update");
        
        String response = installationManagerService.getUpdateServerUrl();
        assertNotNull(response);
        assertEquals(response, "https://codenvy.com");
    }
    
}