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

import org.mockito.Mockito;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.LinkedHashMap;

import static com.codenvy.im.utils.Commons.getPrettyPrintingJson;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 */
public class TestGetUpdatesInstallationManagerServiceImpl {
    private InstallationManagerService installationManagerService;

    private InstallationManager         mockInstallationManager;
    private HttpTransport               transport;
    private Artifact                    installManagerArtifact;
    private Artifact                    cdecArtifact;

    @BeforeMethod
    public void init() {
        initMocks();
        installationManagerService = new InstallationManagerServiceImpl(mockInstallationManager, transport, new DownloadingDescriptorHolder());
    }

    public void initMocks() {
        mockInstallationManager = mock(InstallationManagerImpl.class);
        transport = Mockito.mock(HttpTransport.class);
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

        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(new UserCredentials("auth token"));

        String response = installationManagerService.getUpdates(userCredentialsRep);
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

        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(new UserCredentials("incorrect-token"));   
        
        String response = installationManagerService.getUpdates(userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"message\": \"Authentication error. Authentication token might be expired or invalid.\",\n" +
                                                      "  \"status\": \"ERROR\"\n" +
                                                      "}");
    }

    @Test
    public void testGetUpdatesCatchesArtifactNotFoundException() throws Exception {
        when(mockInstallationManager.getUpdates(anyString())).thenThrow(new ArtifactNotFoundException("cdec"));
        
        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(new UserCredentials("auth token"));

        String response = installationManagerService.getUpdates(userCredentialsRep);

        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"message\": \"There is no any version of artifact 'cdec'\",\n" +
                                                      "  \"status\": \"ERROR\"\n" +
                                                      "}");
    }

    @Test
    public void testGetUpdatesCatchesException() throws Exception {
        when(mockInstallationManager.getUpdates(anyString())).thenThrow(new IOException("Error"));
        
        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(new UserCredentials("incorrect-token")); 
        
        String response = installationManagerService.getUpdates(userCredentialsRep);

        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"message\": \"Error\",\n" +
                                                      "  \"status\": \"ERROR\"\n" +
                                                      "}");
    }
}