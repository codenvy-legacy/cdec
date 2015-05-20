/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
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
package com.codenvy.im.artifacts;

import com.codenvy.im.commands.Command;
import com.codenvy.im.managers.BackupConfig;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;

import org.apache.commons.io.FileUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;


/** @author Dmytro Nochevnov */
public class TestAbstractArtifact {
    private AbstractArtifact spyTestArtifact;

    private static final String  UPDATE_ENDPOINT     = "update/endpoint";
    private static final String  TEST_ARTIFACT_NAME  = "test_artifact_name";
    private static final String  TEST_TOKEN          = "auth token";
    private static final String  TEST_VERSION_STRING = "1.0.0";
    private static final Version TEST_VERSION        = Version.valueOf(TEST_VERSION_STRING);

    @Mock
    private HttpTransport mockTransport;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        spyTestArtifact = spy(new TestedAbstractArtifact(TEST_ARTIFACT_NAME, mockTransport, ""));
    }

    @AfterMethod
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(Paths.get("target", "download").toFile());
    }

    @Test
    public void testGetArtifactProperties() throws Exception {
        doReturn("{\"file\":\"file1\", \"md5\":\"a\"}").when(mockTransport).doGet(endsWith(TEST_ARTIFACT_NAME + "/" + TEST_VERSION_STRING));

        Map m = spyTestArtifact.getProperties(TEST_VERSION);
        assertTrue(m.containsKey("file"));
        assertTrue(m.containsKey("md5"));
        assertEquals(m.get("file"), "file1");
        assertEquals(m.get("md5"), "a");
    }

    @Test
    public void testGetLatestVersion() throws IOException {
        doReturn("{\"version\":\"" + TEST_VERSION_STRING + "\"}")
            .when(mockTransport)
            .doGet(endsWith("repository/properties/" + TEST_ARTIFACT_NAME));

        Version version = spyTestArtifact.getLatestVersion(UPDATE_ENDPOINT, mockTransport);
        assertEquals(version.toString(), TEST_VERSION_STRING);
    }

    @Test
    public void testIsInstallableWhenPreviousVersionOfNewArtifactIsUnknown() throws IOException {
        Version newVersion = Version.valueOf("2.0.0");

        doReturn(TEST_VERSION).when(spyTestArtifact).getInstalledVersion();
        doReturn("{}").when(mockTransport).doGet(endsWith(TEST_ARTIFACT_NAME + "/" + TEST_VERSION));  // allowed previous version of new artifact is unknown
        doReturn("{}").when(mockTransport).doGet(endsWith(TEST_ARTIFACT_NAME + "/" + newVersion));  // allowed previous version of new artifact is unknown

        assertFalse(spyTestArtifact.isInstallable(TEST_VERSION));
        assertTrue(spyTestArtifact.isInstallable(newVersion));
    }

    @Test
    public void testIsInstallableWhenThereIsNoInstalledArtifact() throws IOException {
        doReturn(null).when(spyTestArtifact).getInstalledVersion();
        doReturn("{\"previous-version\":\"" + TEST_VERSION + "\"}").when(mockTransport).doGet(endsWith(TEST_ARTIFACT_NAME + "/" + Version.valueOf("2.0.0")));

        assertTrue(spyTestArtifact.isInstallable(TEST_VERSION));
        assertTrue(spyTestArtifact.isInstallable(Version.valueOf("2.0.0")));
    }

    @Test
    public void testIsInstallableBaseOnPreviousVersion() throws IOException {
        String versionToInstall = "2.0.0";
        doReturn(TEST_VERSION).when(spyTestArtifact).getInstalledVersion();

        doReturn("{\"previous-version\":\"" + TEST_VERSION + "\"}").when(mockTransport).doGet(endsWith(TEST_ARTIFACT_NAME + "/" + versionToInstall));
        assertTrue(spyTestArtifact.isInstallable(Version.valueOf(versionToInstall)));

        doReturn(Version.valueOf("0.0.1")).when(spyTestArtifact).getInstalledVersion();
        assertFalse(spyTestArtifact.isInstallable(Version.valueOf(versionToInstall)));
    }

    @Test
    public void testGetLatestInstallableVersion() throws Exception {
        String newVersion = "1.0.1";
        doReturn(TEST_VERSION).when(spyTestArtifact).getInstalledVersion();

        doReturn("{\"version\":\"" + newVersion + "\"}")
                .when(mockTransport)
                .doGet(endsWith("repository/properties/" + TEST_ARTIFACT_NAME));

        doReturn("{\"previous-version\":\"" + TEST_VERSION + "\"}")
            .when(mockTransport)
            .doGet(contains("repository/properties/" + TEST_ARTIFACT_NAME + "/" + newVersion));

        Version version = spyTestArtifact.getLatestInstallableVersion();

        assertNotNull(version);
        assertEquals(version.toString(), newVersion);
    }

    @Test
    public void testGetNullLatestInstallableVersion() throws Exception {
        doReturn(TEST_VERSION).when(spyTestArtifact).getInstalledVersion();

        doReturn("{\"version\":\"" + TEST_VERSION_STRING + "\"}")
                .when(mockTransport)
                .doGet(endsWith("repository/properties/" + TEST_ARTIFACT_NAME));

        doReturn("{\"previous-version\":\"0.0.1\"}")
            .when(mockTransport)
            .doGet(endsWith("repository/properties/" + TEST_ARTIFACT_NAME + "/" + TEST_VERSION));

        Version version = spyTestArtifact.getLatestInstallableVersion();
        assertNull(version);
    }

    @Test
    public void testGetDownloadedVersions() throws IOException {
        doReturn("{\"file\":\"file1\", \"md5\":\"d41d8cd98f00b204e9800998ecf8427e\"}").when(mockTransport)
                                                                                      .doGet(endsWith(TEST_ARTIFACT_NAME + "/1.0.1"));
        doReturn("{\"file\":\"file2\", \"md5\":\"d41d8cd98f00b204e9800998ecf8427e\"}").when(mockTransport)
                                                                                      .doGet(endsWith(TEST_ARTIFACT_NAME + "/1.0.2"));

        Path file1 = Paths.get("target", "download", spyTestArtifact.getName(), "1.0.1", "file1");
        Path file2 = Paths.get("target", "download", spyTestArtifact.getName(), "1.0.2", "file2");
        Files.createDirectories(file1.getParent());
        Files.createDirectories(file2.getParent());
        Files.createFile(file1);
        Files.createFile(file2);

        SortedMap<Version, Path> versions = spyTestArtifact.getDownloadedVersions(Paths.get("target/download")
                                                                                 );
        assertEquals(versions.size(), 2);
        assertEquals(versions.toString(), "{1.0.2=target/download/" + TEST_ARTIFACT_NAME + "/1.0.2/file2, " +
                                          "1.0.1=target/download/" + TEST_ARTIFACT_NAME + "/1.0.1/file1" +
                                          "}");
    }


    @Test(expectedExceptions = ArtifactNotFoundException.class,
            expectedExceptionsMessageRegExp = "Artifact 'test_artifact_name' not found")
    public void testGetDownloadedVersionsWhenPropertiesAbsent() throws Exception {
        Path file1 = Paths.get("target", "download", spyTestArtifact.getName(), "1.0.1", "file1");
        Files.createDirectories(file1.getParent());
        Files.createFile(file1);

        doThrow(new ArtifactNotFoundException(spyTestArtifact)).when(spyTestArtifact)
                                                               .getProperties(any(Version.class));

        spyTestArtifact.getDownloadedVersions(Paths.get("target/download"));
    }

    private static class TestedAbstractArtifact extends AbstractArtifact {
        public TestedAbstractArtifact(String name, HttpTransport transport, String updateEndpoint) {
            super(name, transport, updateEndpoint);
        }

        @Nullable
        @Override
        public Version getInstalledVersion() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getPriority() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> getInstallInfo(InstallOptions installOptions) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> getUpdateInfo(InstallOptions installOptions) throws IOException {
            return null;
        }

        @Override
        public Command getInstallCommand(Version versionToInstall, Path pathToBinaries, InstallOptions installOptions) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Command getUpdateCommand(Version versionToUpdate, Path pathToBinaries, InstallOptions installOptions) throws IOException {
            return null;
        }

        @Override
        public Command getBackupCommand(BackupConfig backupConfig) throws IOException {
            return null;
        }

        @Override
        public Command getRestoreCommand(BackupConfig backupConfig) throws IOException {
            return null;
        }
    }
}
