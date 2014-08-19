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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;

import org.restlet.ext.jackson.JacksonRepresentation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.codenvy.cdec.InstallationManager;
import com.codenvy.cdec.InstallationManagerService;
import com.codenvy.cdec.artifacts.Artifact;
import com.codenvy.cdec.artifacts.CDECArtifact;
import com.codenvy.cdec.artifacts.InstallManagerArtifact;
import com.codenvy.cdec.im.service.response.ArtifactInfo;
import com.codenvy.cdec.im.service.response.Response;
import com.codenvy.cdec.im.service.response.Response.Status;
import com.codenvy.cdec.im.service.response.StatusCode;
import com.codenvy.cdec.utils.HttpTransport;

/**
 * @author Dmytro Nochevnov
 */
public class TestInstallationManagerService {    
    private InstallationManagerService installationManagerService;

    private InstallationManager        mockInstallationManager;
    private HttpTransport mockTransport;
    private Artifact mockInstallManagerArtifact;
    private Artifact mockCdecArtifact;
    
    private static final String CDEC_ARTIFACT_VERSION =  "2.10.5";
    private static final String INSTALL_MANAGER_ARTIFACT_VERSION = "1.0.1";
    
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
    public void testCheckUpdates() throws Exception {
        doReturn(new HashMap<Artifact, String>() {
             {
                 put(mockCdecArtifact, CDEC_ARTIFACT_VERSION);
                 put(mockInstallManagerArtifact, INSTALL_MANAGER_ARTIFACT_VERSION);
             }
        })
        .when(mockInstallationManager)
        .getNewVersions();

        doNothing()
        .when(mockInstallationManager)
        .checkNewVersions();

        Response expectedObject = new Response(new Status(StatusCode.SUCCESS),
                                                 Arrays.asList(new ArtifactInfo[] {
                                                     new ArtifactInfo(mockCdecArtifact.getName(), CDEC_ARTIFACT_VERSION),
                                                     new ArtifactInfo(mockInstallManagerArtifact.getName(), INSTALL_MANAGER_ARTIFACT_VERSION),                                                     
                                                 }));
        JacksonRepresentation<Response> expectedRepresentation = new JacksonRepresentation<>(expectedObject);
        String expectedJson = expectedRepresentation.getText();
        
        Response expectedObjectSwopped = new Response(new Status(StatusCode.SUCCESS),
                                                Arrays.asList(new ArtifactInfo[] {
                                                    new ArtifactInfo(mockInstallManagerArtifact.getName(), INSTALL_MANAGER_ARTIFACT_VERSION),
                                                    new ArtifactInfo(mockCdecArtifact.getName(), CDEC_ARTIFACT_VERSION),
                                                }));
        JacksonRepresentation<Response> expectedRepresentationSwopped = new JacksonRepresentation<>(expectedObjectSwopped);
        String expectedJsonSwopped = expectedRepresentationSwopped.getText();
        
        
        String responseJson = installationManagerService.checkUpdates();
        
        if (! expectedJson.equals(responseJson)) {
            assertEquals(expectedJsonSwopped, responseJson);
        }
    }
    
    @Test
    public void testDownload() throws Exception {
        doReturn(new HashMap<Artifact, String>() {
             {
                 put(mockCdecArtifact, CDEC_ARTIFACT_VERSION);
                 put(mockInstallManagerArtifact, INSTALL_MANAGER_ARTIFACT_VERSION);
             }
        })
        .when(mockInstallationManager)
        .getNewVersions();

        doNothing()
        .when(mockInstallationManager)
        .checkNewVersions();
        
//        JacksonRepresentation responseRepresentation = installationManagerService.download(mockCdecArtifact.getName(), INSTALL_MANAGER_ARTIFACT_VERSION);
////        assertNotNull(response);
//        
//        responseRepresentation = installationManagerService.download(mockInstallManagerArtifact.getName(), null);
//        assertNotNull(response);
    }    
    
    public class InstallationManagerServiceTestImpl extends InstallationManagerServiceImpl {
        public InstallationManagerServiceTestImpl(InstallationManager manager) {
            this.manager = manager;
        }
    }
    
}