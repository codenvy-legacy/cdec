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
import com.codenvy.cdec.artifacts.InstallManagerArtifact;
import com.codenvy.cdec.server.InstallationManager;
import com.codenvy.cdec.utils.HttpTransport;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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

    private static final InstallManagerArtifact INSTALL_MANAGER_ARTIFACT = new InstallManagerArtifact();

    private HttpTransport       transport;
    private InstallationManager manager;

    @BeforeMethod
    public void setUp() throws Exception {
        transport = mock(HttpTransport.class);

        manager = new InstallationManagerImpl("api/endpoint",
                                              "update/endpoint",
                                              "target/download",
                                              transport,
                                              new HashSet<Artifact>(Arrays.asList(INSTALL_MANAGER_ARTIFACT)));
    }

    @Test
    public void testGetExistedArtifacts() throws Exception {
        Map<Artifact, String> m = manager.getExistedArtifacts();
        assertNotNull(m.get(INSTALL_MANAGER_ARTIFACT));
    }

    @Test
    public void testGetAvailable2DownloadArtifacts() throws Exception {
        when(transport.doGetRequest("update/endpoint/repository/version/" + InstallManagerArtifact.NAME)).thenReturn("{version:1.0.1}");
        Map<Artifact, String> m = manager.getAvailable2DownloadArtifacts();

        assertEquals(m.size(), 1);
        assertEquals(m.get(INSTALL_MANAGER_ARTIFACT), "1.0.1");
    }
}
