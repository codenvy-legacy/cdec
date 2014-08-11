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
import com.codenvy.cdec.server.InstallationManager;
import com.codenvy.cdec.utils.HttpTransport;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Anatoliy Bazko
 */
public class TestInstallationManager {

    private Artifact CDEC_ARTIFACT;
    private Artifact INSTALL_MANAGER_ARTIFACT;

    private InstallationManagerImpl manager;

    @BeforeTest
    public void setUp() throws Exception {
        HttpTransport transport = mock(HttpTransport.class);

        INSTALL_MANAGER_ARTIFACT = new InstallManagerArtifact();
        CDEC_ARTIFACT = new CDECArtifact("update/endpoint", transport);

        InstallationManager manager = new InstallationManagerImpl("api/endpoint",
                                                                  "update/endpoint",
                                                                  "target/download",
                                                                  transport,
                                                                  new HashSet<>(Arrays.asList(INSTALL_MANAGER_ARTIFACT, CDEC_ARTIFACT)));

        Registry registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        registry.bind(InstallationManager.class.getSimpleName(), manager);

        registry = LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
        this.manager = (InstallationManagerImpl)registry.lookup(InstallationManager.class.getSimpleName());
    }

    @Test
    public void testGetInstalledArtifacts() throws Exception {
        when(manager.transport.doGetRequest("update/endpoint/repository/info/" + CDECArtifact.NAME)).thenReturn("{version:2.10.4}");

        Map<Artifact, String> m = manager.getInstalledArtifacts();
        assertEquals(m.get(CDEC_ARTIFACT), "2.10.4");
        assertNotNull(m.get(INSTALL_MANAGER_ARTIFACT));
    }

    @Test
    public void testGetAvailable2DownloadArtifacts() throws Exception {
        when(manager.transport.doGetRequest("update/endpoint/repository/version/" + InstallManagerArtifact.NAME)).thenReturn("{version:1.0.1}");
        when(manager.transport.doGetRequest("update/endpoint/repository/version/" + CDECArtifact.NAME)).thenReturn("{version:2.10.5}");
        Map<Artifact, String> m = manager.getAvailable2DownloadArtifacts();

        assertEquals(m.size(), 2);
        assertEquals(m.get(INSTALL_MANAGER_ARTIFACT), "1.0.1");
        assertEquals(m.get(CDEC_ARTIFACT), "2.10.5");
    }
}
