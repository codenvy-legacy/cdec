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
import com.codenvy.cdec.user.UserCredentials;
import com.codenvy.cdec.utils.AccountUtils;
import com.codenvy.cdec.utils.HttpTransport;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.codenvy.cdec.artifacts.ArtifactProperties.AUTHENTICATION_REQUIRED_PROPERTY;
import static com.codenvy.cdec.artifacts.ArtifactProperties.SUBSCRIPTION_PROPERTY;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author Anatoliy Bazko
 */
public class TestInstallationManager {

    private Artifact cdecArtifact;
    private Artifact installManagerArtifact;

    private InstallationManagerImpl manager;
    private HttpTransport           transport;
    
    private UserCredentials         testCredentials;

    @BeforeMethod
    public void setUp() throws Exception {
        transport = mock(HttpTransport.class);

        installManagerArtifact = spy(new InstallManagerArtifact());
        cdecArtifact = spy(new CDECArtifact("update/endpoint", transport));

        manager = spy(new InstallationManagerImpl("api/endpoint",
                                                  "update/endpoint",
                                                  "target/download",
                                                  transport,
                                                  new HashSet<>(Arrays.asList(installManagerArtifact, cdecArtifact))));

        testCredentials = new UserCredentials("auth token", "accountId");
    }

    @AfterMethod
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(Paths.get("target", "download").toFile());
    }

    @Test
    public void testInstallArtifactDoNothingIfArtifactInstalled() throws Exception {
        doReturn(new HashMap<Artifact, Path>() {{
            put(installManagerArtifact, Paths.get("target/download/installation-manager/2.10.1/file1"));
        }}).when(manager).getDownloadedArtifacts();
        doReturn("2.10.1").when(installManagerArtifact).getCurrentVersion(testCredentials.getToken());

        manager.install(testCredentials.getToken(), installManagerArtifact, "2.10.1");

        verify(installManagerArtifact, never()).install(any(Path.class));
    }

    @Test(expectedExceptions = FileNotFoundException.class)
    public void testInstallArtifactErrorIfBinariesNotFound() throws Exception {
        doReturn(null).when(cdecArtifact).getCurrentVersion(testCredentials.getToken());

        manager.install(testCredentials.getToken(), cdecArtifact, "2.10.1");
    }

    @Test
    public void testInstallArtifact() throws Exception {
        doReturn(new HashMap<Artifact, Path>() {{
            put(cdecArtifact, Paths.get("target/download/cdec/1.0.1/file1"));
        }}).when(manager).getDownloadedArtifacts();
        doNothing().when(cdecArtifact).install(any(Path.class));
        doReturn(null).when(cdecArtifact).getCurrentVersion(testCredentials.getToken());

        manager.install(testCredentials.getToken(), cdecArtifact, "1.0.1");
        verify(cdecArtifact).install(any(Path.class));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testInstallArtifactErrorIfArtifactInstalledNewly() throws Exception {
        doReturn(new HashMap<Artifact, Path>() {{
            put(installManagerArtifact, Paths.get("target/download/installation-manager/2.10.0/file1"));
        }}).when(manager).getDownloadedArtifacts();
        doReturn("2.10.1").when(installManagerArtifact).getCurrentVersion(testCredentials.getToken());

        manager.install(testCredentials.getToken(), installManagerArtifact, "2.10.0");
    }

    @Test
    public void testInstallArtifactNewlyArtifact() throws Exception {
        doReturn(new HashMap<Artifact, Path>() {{
            put(installManagerArtifact, Paths.get("target/download/installation-manager/2.10.2/file1"));
        }}).when(manager).getDownloadedArtifacts();
        doReturn(Collections.emptyMap()).when(manager).getInstalledArtifacts(testCredentials.getToken());
        doReturn("2.10.1").when(installManagerArtifact).getCurrentVersion(testCredentials.getToken());
        doNothing().when(installManagerArtifact).install(any(Path.class));

        manager.install(testCredentials.getToken(), installManagerArtifact, "2.10.2");
    }

    @Test
    public void testDownloadVersion() throws Exception {
        String version = "1.0.0";        
        when(transport.doGetRequest("api/endpoint/account", testCredentials.getToken()))
             .thenReturn("[{"
                         + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                         + "accountReference:{id:\"" + testCredentials.getAccountId() + "\"}"
                         + "}]");

        when(transport.doGetRequest("api/endpoint/account/" + testCredentials.getAccountId() + "/subscriptions", testCredentials.getToken()))
            .thenReturn("[{serviceId:\"On-Premises\"}]");

        when(transport.doGetRequest("update/endpoint/repository/properties/" + cdecArtifact.getName() + "/" + version))
                .thenReturn(String.format("{\"%s\": \"true\", \"%s\":\"On-Premises\"}", AUTHENTICATION_REQUIRED_PROPERTY, SUBSCRIPTION_PROPERTY));

        manager.download(testCredentials, cdecArtifact, version);

    }

    @Test
    public void testGetUpdates() throws Exception {
        doReturn(new HashMap<Artifact, String>() {{
            put(cdecArtifact, "2.10.5");
            put(installManagerArtifact, "1.0.0");
        }}).when(manager).getInstalledArtifacts(testCredentials.getToken());
        doReturn(new HashMap<Artifact, String>() {{
            put(cdecArtifact, "2.10.5");
            put(installManagerArtifact, "1.0.1");
        }}).when(manager).getLatestVersionsToDownload();

        Map<Artifact, String> m = manager.getUpdates(testCredentials.getToken());
        assertEquals(m.size(), 1);
        assertEquals(m.get(installManagerArtifact), "1.0.1");
    }

    @Test
    public void testGetInstalledArtifacts() throws Exception {
        when(transport.doGetRequest("update/endpoint/repository/installationinfo/" + CDECArtifact.NAME, testCredentials.getToken()))
                .thenReturn("{version:2.10.4}");

        Map<Artifact, String> m = manager.getInstalledArtifacts(testCredentials.getToken());
        assertNull(m.get(cdecArtifact));
        assertNotNull(m.get(installManagerArtifact));
    }

    @Test
    public void testGetLatestVersionsToDownload() throws Exception {
        when(transport.doGetRequest("update/endpoint/repository/properties/" + InstallManagerArtifact.NAME)).thenReturn("{version:1.0.1}");
        when(transport.doGetRequest("update/endpoint/repository/properties/" + CDECArtifact.NAME)).thenReturn("{version:2.10.5}");
        Map<Artifact, String> m = manager.getLatestVersionsToDownload();

        assertEquals(m.size(), 2);
        assertEquals(m.get(installManagerArtifact), "1.0.1");
        assertEquals(m.get(cdecArtifact), "2.10.5");
    }

    @Test
    public void testGetDownloadedArtifacts() throws Exception {
        Path file1 = Paths.get("target", "download", cdecArtifact.getName(), "1.0.1", "file1");
        Path file2 = Paths.get("target", "download", installManagerArtifact.getName(), "1.0.2", "file2");
        Files.createDirectories(file1.getParent());
        Files.createDirectories(file2.getParent());
        Files.createFile(file1);
        Files.createFile(file2);

        Map<Artifact, Path> m = manager.getDownloadedArtifacts();
        assertEquals(m.size(), 2);
        assertEquals(m.get(cdecArtifact), file1);
        assertEquals(m.get(installManagerArtifact), file2);
    }

    @Test
    public void testGetDownloadedArtifactsSeveralVersions() throws Exception {
        Path file1 = Paths.get("target", "download", cdecArtifact.getName(), "1.0.1", "file1");
        Path file2 = Paths.get("target", "download", cdecArtifact.getName(), "1.0.2", "file2");
        Files.createDirectories(file1.getParent());
        Files.createDirectories(file2.getParent());
        Files.createFile(file1);
        Files.createFile(file2);

        Map<Artifact, Path> m = manager.getDownloadedArtifacts();
        assertEquals(m.size(), 1);
        assertEquals(m.get(cdecArtifact), file2);
    }

    @Test
    public void testGetDownloadedArtifactsReturnsEmptyMap() throws Exception {
        Map<Artifact, Path> m = manager.getDownloadedArtifacts();
        assertTrue(m.isEmpty());
    }

    @Test(expectedExceptions = IOException.class)
    public void testGetDownloadedArtifactsErrorIfMoreThan1File() throws Exception {
        Path file1 = Paths.get("target", "download", cdecArtifact.getName(), "1.0.1", "file1");
        Path file2 = Paths.get("target", "download", cdecArtifact.getName(), "1.0.1", "file2");
        Files.createDirectories(file1.getParent());
        Files.createFile(file1);
        Files.createFile(file2);

        manager.getDownloadedArtifacts();
    }
}
