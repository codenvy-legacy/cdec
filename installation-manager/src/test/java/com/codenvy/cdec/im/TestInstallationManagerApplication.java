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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.codenvy.cdec.artifacts.Artifact;
import com.codenvy.cdec.artifacts.CDECArtifact;
import com.codenvy.cdec.artifacts.InstallManagerArtifact;
import com.codenvy.cdec.restlet.RestletClientFactory;
import com.codenvy.cdec.restlet.RestletServerFactory;
import com.codenvy.cdec.server.InstallationManagerService;
import com.codenvy.cdec.utils.HttpTransport;

/**
 * @author Dmytro Nochevnov
 */
public class TestInstallationManagerApplication {    
    protected static final Logger              LOG = LoggerFactory.getLogger(TestInstallationManagerApplication.class);

    private InstallationManagerService         installationManagerSeviceProxy;

    private InstallationManagerImpl            manager;
    private HttpTransport                      transport;
    private RestletServerFactory.RestletServer installationManagerServer;
    
    @BeforeMethod
    public void setUpRestServer() throws Exception {
        installationManagerServer = RestletServerFactory.getServer(new InstallationManagerTestApplication());
        installationManagerServer.start();
        
        installationManagerSeviceProxy = RestletClientFactory.getServiceProxy(InstallationManagerService.class);        
    }

    public void setUpInstallationManager() throws IOException {        
        manager = mock(InstallationManagerImpl.class);
  
        final Artifact installManagerArtifact = spy(new InstallManagerArtifact());
        final Artifact cdecArtifact = spy(new CDECArtifact("update/endpoint", transport));
        
        when(manager.getNewVersions())
                    .thenReturn(new HashMap<Artifact, String>() {{
                                              put(cdecArtifact, "2.10.5");
                                              put(installManagerArtifact, "1.0.1");
                                          }
                                      });
        
        doNothing().when(manager).checkNewVersions();        
    }
    
    @AfterMethod
    public void tearDownRestServer() throws Exception {
        installationManagerServer.stop();
    }
    
    @AfterMethod
    public void tearDownInstallationManager() throws Exception {
        FileUtils.deleteDirectory(Paths.get("target", "download").toFile());
    }
    
//    @Test
    public void testCheckUpdates() throws Exception {        
        String expectedContent = "[{\"status\":\"downloaded\",\"version\":\"v1\"}]";
        
        try {
            JsonRepresentation response = installationManagerSeviceProxy.checkUpdates();  // Not Found (404) - Not Found. Not Found
            
            if (response == null) {
                fail();
            }
            
            JSONArray updates = response.getJsonArray();
            LOG.info(updates.toString());
            
            for (int i = 0; i < updates.length(); i++) {
                JSONObject update = updates.getJSONObject(i);
                LOG.info(update.toString());                
            }            
            
//            assertEquals(expectedContent, updates.toString());
            
        } catch (ResourceException re) {
            fail(re.getStatus().toString() + ". " + re.getMessage());
        }
    }
    
    
    public class InstallationManagerTestApplication extends Application {
        public Set<Class<?>> getClasses() {
            Set<Class<?>> rrcs = new HashSet<Class<?>>();
            rrcs.add(InstallationManagerServiceTestImpl.class);
            return rrcs;
        }
    }
    
    public class InstallationManagerServiceTestImpl extends InstallationManagerServiceImpl {
        public InstallationManagerServiceTestImpl() {
            this.manager = null;   // TODO setup mock manager
        }
    }
}