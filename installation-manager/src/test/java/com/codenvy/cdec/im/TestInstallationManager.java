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
import com.codenvy.cdec.utils.HttpTransport;

import org.apache.commons.io.FileUtils;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author Anatoliy Bazko
 */
public class TestInstallationManager {

    private Artifact CDEC_ARTIFACT;
    private Artifact INSTALL_MANAGER_ARTIFACT;

    private InstallationManagerImpl manager;
    private HttpTransport           transport;

    @BeforeTest
    public void setUp() throws Exception {
        transport = mock(HttpTransport.class);

        INSTALL_MANAGER_ARTIFACT = new InstallManagerArtifact();
        CDEC_ARTIFACT = new CDECArtifact("update/endpoint", transport);

        manager = spy(new InstallationManagerImpl("api/endpoint",
                                                  "update/endpoint",
                                                  "target/download",
                                                  transport,
                                                  new HashSet<>(Arrays.asList(INSTALL_MANAGER_ARTIFACT, CDEC_ARTIFACT))));

    }


    @AfterMethod
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(Paths.get("target", "download").toFile());
    }

    @Test
    public void testDownloadUpdates() throws Exception {
        final Path file1 = Paths.get("target", "download", CDEC_ARTIFACT.getName(), "file1");
        final Path file2 = Paths.get("target", "download", INSTALL_MANAGER_ARTIFACT.getName(), "file2");

        stub(transport.download(eq("update/endpoint/repository/public/download/" + InstallManagerArtifact.NAME + "/1.0.1"), any(Path.class)))
                .toAnswer(new Answer<Path>() {
                    @Override
                    public Path answer(InvocationOnMock invocationOnMock) throws Throwable {
                        Files.createDirectories(file2.getParent());
                        Files.createFile(file2);
                        return file2;
                    }
                });
        stub(transport.download(eq("update/endpoint/repository/download/" + CDECArtifact.NAME + "/2.10.5"), any(Path.class)))
                .toAnswer(new Answer<Path>() {
                    @Override
                    public Path answer(InvocationOnMock invocationOnMock) throws Throwable {
                        Files.createDirectories(file1.getParent());
                        Files.createFile(file1);
                        return file1;
                    }
                });
        when(manager.getNewVersions()).thenReturn(new HashMap<Artifact, String>() {{
            put(CDEC_ARTIFACT, "2.10.5");
            put(INSTALL_MANAGER_ARTIFACT, "1.0.1");
        }});
        doReturn(true).when(manager).isValidSubscription();

        manager.downloadUpdates();

        Map<Artifact, Path> m = manager.getDownloadedArtifacts();
        assertEquals(m.size(), 2);
        assertEquals(m.get(CDEC_ARTIFACT), file1);
        assertEquals(m.get(INSTALL_MANAGER_ARTIFACT), file2);
    }

    @Test
    public void testGetInstalledArtifacts() throws Exception {
        when(transport.doGetRequest("update/endpoint/repository/info/" + CDECArtifact.NAME)).thenReturn("{version:2.10.4}");

        Map<Artifact, String> m = manager.getInstalledArtifacts();
        assertEquals(m.get(CDEC_ARTIFACT), "2.10.4");
        assertNotNull(m.get(INSTALL_MANAGER_ARTIFACT));
    }

    @Test
    public void testGetAvailable2DownloadArtifacts() throws Exception {
        when(transport.doGetRequest("update/endpoint/repository/version/" + InstallManagerArtifact.NAME)).thenReturn("{version:1.0.1}");
        when(transport.doGetRequest("update/endpoint/repository/version/" + CDECArtifact.NAME)).thenReturn("{version:2.10.5}");
        Map<Artifact, String> m = manager.getAvailable2DownloadArtifacts();

        assertEquals(m.size(), 2);
        assertEquals(m.get(INSTALL_MANAGER_ARTIFACT), "1.0.1");
        assertEquals(m.get(CDEC_ARTIFACT), "2.10.5");
    }

    @Test
    public void testGetDownloadedArtifacts() throws Exception {
        Path file1 = Paths.get("target", "download", CDEC_ARTIFACT.getName(), "file1");
        Path file2 = Paths.get("target", "download", INSTALL_MANAGER_ARTIFACT.getName(), "file2");
        Files.createDirectories(file1.getParent());
        Files.createDirectories(file2.getParent());
        Files.createFile(file1);
        Files.createFile(file2);

        Map<Artifact, Path> m = manager.getDownloadedArtifacts();
        assertEquals(m.size(), 2);
        assertEquals(m.get(CDEC_ARTIFACT), file1);
        assertEquals(m.get(INSTALL_MANAGER_ARTIFACT), file2);
    }

    @Test
    public void testGetDownloadedArtifactsReturnsEmptyMap() throws Exception {
        Map<Artifact, Path> m = manager.getDownloadedArtifacts();
        assertTrue(m.isEmpty());
    }

    @Test(expectedExceptions = IOException.class)
    public void testGetDownloadedArtifactsErrorIfMoreThan1File() throws Exception {
        Path file1 = Paths.get("target", "download", CDEC_ARTIFACT.getName(), "file1");
        Path file2 = Paths.get("target", "download", CDEC_ARTIFACT.getName(), "file2");
        Files.createDirectories(file1.getParent());
        Files.createFile(file1);
        Files.createFile(file2);

        manager.getDownloadedArtifacts();
    }
}
