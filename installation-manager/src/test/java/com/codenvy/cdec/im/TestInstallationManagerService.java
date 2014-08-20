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

import com.codenvy.cdec.InstallationManager;
import com.codenvy.cdec.InstallationManagerService;
import com.codenvy.cdec.artifacts.Artifact;
import com.codenvy.cdec.artifacts.ArtifactFactory;
import com.codenvy.cdec.artifacts.CDECArtifact;
import com.codenvy.cdec.artifacts.InstallManagerArtifact;
import com.codenvy.cdec.im.service.response.ArtifactInfo;
import com.codenvy.cdec.im.service.response.Response;
import com.codenvy.cdec.im.service.response.Response.Status;
import com.codenvy.cdec.im.service.response.StatusCode;
import com.codenvy.cdec.utils.Commons;
import com.codenvy.cdec.utils.HttpTransport;

import org.restlet.ext.jackson.JacksonRepresentation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 *         TODO check
 */
public class TestInstallationManagerService {
    private InstallationManagerService installationManagerService;

    private InstallationManager mockInstallationManager;
    private Artifact            mockInstallManagerArtifact;
    private Artifact            mockCdecArtifact;

    private static final String CDEC_ARTIFACT_VERSION = "2.10.5";
    private static final String INSTALL_MANAGER_ARTIFACT_VERSION = "1.0.1";

    @BeforeMethod
    public void init() {
        initMocks();

        installationManagerService = new InstallationManagerServiceTestImpl(mockInstallationManager);
    }

    public void initMocks() {
        mockInstallationManager = mock(InstallationManagerImpl.class);

        mockCdecArtifact = ArtifactFactory.createArtifact(CDECArtifact.NAME);
        mockInstallManagerArtifact = ArtifactFactory.createArtifact(InstallManagerArtifact.NAME);
    }

    @Test
    public void testCheckUpdates() throws Exception {
        doReturn(new HashMap<Artifact, String>() {{
                put(mockCdecArtifact, CDEC_ARTIFACT_VERSION);
                put(mockInstallManagerArtifact, INSTALL_MANAGER_ARTIFACT_VERSION);
        }})
        .when(mockInstallationManager)
        .getUpdates();

        String responseJson = installationManagerService.checkUpdates();
        
        Response expectedResponse = new Response(new Status(StatusCode.OK),
                                                 Arrays.asList(new ArtifactInfo[] {
                                                     new ArtifactInfo(mockCdecArtifact.getName(), CDEC_ARTIFACT_VERSION),
                                                     new ArtifactInfo(mockInstallManagerArtifact.getName(), INSTALL_MANAGER_ARTIFACT_VERSION),                                                     
                                                 }));
        String expectedJson = Commons.getJson(expectedResponse);
                
        if (! expectedJson.equals(responseJson)) {
            // Swap artifacts
            expectedResponse = new Response(new Status(StatusCode.OK),
                                            Arrays.asList(new ArtifactInfo[] {
                                                new ArtifactInfo(mockInstallManagerArtifact.getName(), INSTALL_MANAGER_ARTIFACT_VERSION),
                                                new ArtifactInfo(mockCdecArtifact.getName(), CDEC_ARTIFACT_VERSION),
                                            }));
            expectedJson = Commons.getJson(expectedResponse);
            
            assertEquals(responseJson, expectedJson);
        }
    }

    @Test
    public void testDownloadArtifact() throws Exception {
        String olderVersion = "0." + CDEC_ARTIFACT_VERSION;
        
        doReturn(new HashMap<Artifact, String>() {{
            put(mockCdecArtifact, CDEC_ARTIFACT_VERSION);
        }})
        .when(mockInstallationManager)
        .getUpdates();
                
        doNothing()
        .when(mockInstallationManager)
        .download(mockCdecArtifact, CDEC_ARTIFACT_VERSION);

        doNothing()
        .when(mockInstallationManager)
        .download(mockCdecArtifact, olderVersion);
        
        doNothing()
        .when(mockInstallationManager)
        .download();
        
        // check download entire updates
        {
            Response expectedResponse = new Response(new Status(StatusCode.OK));
            String expectedJson = Commons.getJson(expectedResponse);
            
            String responseJson = installationManagerService.download();
            assertEquals(responseJson, expectedJson);
        }
        
        // check download update of certain artifact
        {
            Response expectedResponse = new Response(new Status(StatusCode.OK),
                                                     new ArtifactInfo(new Status(StatusCode.DOWNLOADED), 
                                                                      mockCdecArtifact.getName(), 
                                                                      CDEC_ARTIFACT_VERSION));
            String expectedJson = Commons.getJson(expectedResponse);
            
            String responseJson = installationManagerService.download(mockCdecArtifact.getName());
            assertEquals(responseJson, expectedJson);
        }
        
        // check download certain artifact of certain version
        {
            Response expectedResponse = new Response(new Status(StatusCode.OK),
                                                     new ArtifactInfo(new Status(StatusCode.DOWNLOADED), 
                                                                      mockCdecArtifact.getName(), 
                                                                      olderVersion));
            String expectedJson = Commons.getJson(expectedResponse);

            String responseJson = installationManagerService.download(mockCdecArtifact.getName(), olderVersion);
            assertEquals(responseJson, expectedJson);
        }
    }

    public class InstallationManagerServiceTestImpl extends InstallationManagerServiceImpl {
        public InstallationManagerServiceTestImpl(InstallationManager manager) {
            this.manager = manager;
        }
    }

}