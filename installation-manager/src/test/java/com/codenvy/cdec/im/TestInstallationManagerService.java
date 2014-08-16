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
import static org.testng.Assert.assertNotNull;

import java.util.HashMap;

import org.json.JSONArray;
import org.restlet.ext.json.JsonRepresentation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.codenvy.cdec.InstallationManager;
import com.codenvy.cdec.InstallationManagerService;
import com.codenvy.cdec.artifacts.Artifact;
import com.codenvy.cdec.artifacts.CDECArtifact;
import com.codenvy.cdec.artifacts.InstallManagerArtifact;
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
                 put(mockCdecArtifact, "2.10.5");
                 put(mockInstallManagerArtifact, "1.0.1");
             }
        })
        .when(mockInstallationManager)
        .getNewVersions();

        doNothing()
        .when(mockInstallationManager)
        .checkNewVersions();
        
        JsonRepresentation response = installationManagerService.checkUpdates();
        assertNotNull(response);
        
        JSONArray updates = response.getJsonArray();        
        assertNotNull(updates);        
        assertEquals(updates.length(), 2);

        if (updates.getJSONObject(0).get("artifact").equals(mockInstallManagerArtifact.getName())) {
            assertEquals(updates.getJSONObject(0).toString(), "{\"artifact\":\"installation-manager\",\"version\":\"1.0.1\"}");
            assertEquals(updates.getJSONObject(1).toString(), "{\"artifact\":\"cdec\",\"version\":\"2.10.5\"}");            
        } else {
            assertEquals(updates.getJSONObject(0).toString(), "{\"artifact\":\"cdec\",\"version\":\"2.10.5\"}");
            assertEquals(updates.getJSONObject(1).toString(), "{\"artifact\":\"installation-manager\",\"version\":\"1.0.1\"}");
        }
    }
    
    public class InstallationManagerServiceTestImpl extends InstallationManagerServiceImpl {
        public InstallationManagerServiceTestImpl(InstallationManager manager) {
            this.manager = manager;
        }
    }
}