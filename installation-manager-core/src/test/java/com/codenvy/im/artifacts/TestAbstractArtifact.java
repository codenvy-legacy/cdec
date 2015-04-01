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

import com.codenvy.im.backup.BackupConfig;
import com.codenvy.im.command.Command;
import com.codenvy.im.config.ConfigUtil;
import com.codenvy.im.exceptions.ArtifactNotFoundException;
import com.codenvy.im.install.InstallOptions;
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
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Mockito.doNothing;
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

        spyTestArtifact = spy(new TestedAbstractArtifact(TEST_ARTIFACT_NAME));
    }

    @AfterMethod
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(Paths.get("target", "download").toFile());
    }

    @Test
    public void testGetArtifactProperties() throws Exception {
        doNothing().when(spyTestArtifact).validateProperties(anyMap());
        doReturn("{\"file\":\"file1\", \"md5\":\"a\"}").when(mockTransport).doGet(endsWith(TEST_ARTIFACT_NAME + "/" + TEST_VERSION_STRING));

        Map m = spyTestArtifact.getProperties(TEST_VERSION, UPDATE_ENDPOINT, mockTransport);
        assertTrue(m.containsKey("file"));
        assertTrue(m.containsKey("md5"));
        assertEquals(m.get("file"), "file1");
        assertEquals(m.get("md5"), "a");
    }

    @Test
    public void testGetLatestVersion() throws IOException {
        doNothing().when(spyTestArtifact).validateProperties(anyMap());
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
        doNothing().when(spyTestArtifact).validateProperties(anyMap());
        doReturn("{}").when(mockTransport).doGet(endsWith(TEST_ARTIFACT_NAME + "/" + TEST_VERSION));  // possible previous version of new artifact is unknown
        doReturn("{}").when(mockTransport).doGet(endsWith(TEST_ARTIFACT_NAME + "/" + newVersion));  // possible previous version of new artifact is unknown

        assertFalse(spyTestArtifact.isInstallable(TEST_VERSION, UPDATE_ENDPOINT, mockTransport));
        assertTrue(spyTestArtifact.isInstallable(newVersion, UPDATE_ENDPOINT, mockTransport));
    }

    @Test
    public void testIsInstallableWhenThereIsNoInstalledArtifact() throws IOException {
        doReturn(null).when(spyTestArtifact).getInstalledVersion();
        doNothing().when(spyTestArtifact).validateProperties(anyMap());
        doReturn("{\"previous-version\":\"" + TEST_VERSION + "\"}").when(mockTransport).doGet(endsWith(TEST_ARTIFACT_NAME + "/" + Version.valueOf("2.0.0")));

        assertTrue(spyTestArtifact.isInstallable(TEST_VERSION, UPDATE_ENDPOINT, mockTransport));
        assertTrue(spyTestArtifact.isInstallable(Version.valueOf("2.0.0"), UPDATE_ENDPOINT, mockTransport));
    }

    @Test
    public void testIsInstallableBaseOnPreviousVersion() throws IOException {
        String versionToInstall = "2.0.0";
        doReturn(TEST_VERSION).when(spyTestArtifact).getInstalledVersion();
        doNothing().when(spyTestArtifact).validateProperties(anyMap());

        doReturn("{\"previous-version\":\"" + TEST_VERSION + "\"}").when(mockTransport).doGet(endsWith(TEST_ARTIFACT_NAME + "/" + versionToInstall));
        assertTrue(spyTestArtifact.isInstallable(Version.valueOf(versionToInstall), UPDATE_ENDPOINT, mockTransport));

        doReturn(Version.valueOf("0.0.1")).when(spyTestArtifact).getInstalledVersion();
        assertFalse(spyTestArtifact.isInstallable(Version.valueOf(versionToInstall), UPDATE_ENDPOINT, mockTransport));
    }

    @Test
    public void testGetLatestInstallableVersion() throws Exception {
        String newVersion = "1.0.1";
        doReturn(TEST_VERSION).when(spyTestArtifact).getInstalledVersion();

        doNothing().when(spyTestArtifact).validateProperties(anyMap());
        doReturn("{\"version\":\"" + newVersion + "\"}")
                .when(mockTransport)
                .doGet(endsWith("repository/properties/" + TEST_ARTIFACT_NAME));

        doReturn("{\"previous-version\":\"" + TEST_VERSION + "\"}")
            .when(mockTransport)
            .doGet(contains("repository/properties/" + TEST_ARTIFACT_NAME + "/" + newVersion));

        Version version = spyTestArtifact.getLatestInstallableVersion(TEST_TOKEN, UPDATE_ENDPOINT, mockTransport);

        assertNotNull(version);
        assertEquals(version.toString(), newVersion);
    }

    @Test
    public void testGetNullLatestInstallableVersion() throws Exception {
        doReturn(TEST_VERSION).when(spyTestArtifact).getInstalledVersion();

        doNothing().when(spyTestArtifact).validateProperties(anyMap());
        doReturn("{\"version\":\"" + TEST_VERSION_STRING + "\"}")
                .when(mockTransport)
                .doGet(endsWith("repository/properties/" + TEST_ARTIFACT_NAME));

        doReturn("{\"previous-version\":\"0.0.1\"}")
            .when(mockTransport)
            .doGet(endsWith("repository/properties/" + TEST_ARTIFACT_NAME + "/" + TEST_VERSION));

        Version version = spyTestArtifact.getLatestInstallableVersion(TEST_TOKEN, UPDATE_ENDPOINT, mockTransport);
        assertNull(version);
    }

    @Test
    public void testValidateProperties() throws Exception {
        doReturn(TEST_VERSION).when(spyTestArtifact).getInstalledVersion();

        String artifactProperties =
            "{\"version\":\"1.0.0\", \"previous-version\":\"0.0.1\", \"file\":\"file1\", \"md5\":\"a\", \"build-time\":\"01.01.01\", \"artifact\":\"" + TEST_ARTIFACT_NAME +
            "\", \"size\":456, \"authentication-required\":false}";

        doReturn(artifactProperties)
                .when(mockTransport)
                .doGet(endsWith("repository/properties/" + TEST_ARTIFACT_NAME));

        doReturn(artifactProperties)
            .when(mockTransport)
            .doGet(endsWith("repository/properties/" + TEST_ARTIFACT_NAME + "/" + TEST_VERSION));

        Version version = spyTestArtifact.getLatestInstallableVersion(TEST_TOKEN, UPDATE_ENDPOINT, mockTransport);
        assertNull(version);
    }

    @Test(expectedExceptions = IOException.class,
            expectedExceptionsMessageRegExp = "Can't get artifact property: previous-version")
    public void testValidatePropertiesFail() throws Exception {
        doReturn("{\"version\":\"" + TEST_VERSION_STRING + "\"}")
                .when(mockTransport)
                .doGet(endsWith("repository/properties/" + TEST_ARTIFACT_NAME + "/" + TEST_VERSION));

        spyTestArtifact.getProperties(TEST_VERSION, UPDATE_ENDPOINT, mockTransport);
    }

    @Test
    public void testGetDownloadedVersions() throws IOException {
        doReturn("{\"file\":\"file1\", \"md5\":\"d41d8cd98f00b204e9800998ecf8427e\"}").when(mockTransport)
                                                                                      .doGet(endsWith(TEST_ARTIFACT_NAME + "/1.0.1"));
        doReturn("{\"file\":\"file2\", \"md5\":\"d41d8cd98f00b204e9800998ecf8427e\"}").when(mockTransport)
                                                                                      .doGet(endsWith(TEST_ARTIFACT_NAME + "/1.0.2"));

        doNothing().when(spyTestArtifact).validateProperties(anyMap());

        Path file1 = Paths.get("target", "download", spyTestArtifact.getName(), "1.0.1", "file1");
        Path file2 = Paths.get("target", "download", spyTestArtifact.getName(), "1.0.2", "file2");
        Files.createDirectories(file1.getParent());
        Files.createDirectories(file2.getParent());
        Files.createFile(file1);
        Files.createFile(file2);

        SortedMap<Version, Path> versions = spyTestArtifact.getDownloadedVersions(Paths.get("target/download"), UPDATE_ENDPOINT,
                                                                                  mockTransport);
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
                                                               .getProperties(any(Version.class), anyString(), any(HttpTransport.class));

        spyTestArtifact.getDownloadedVersions(Paths.get("target/download"), UPDATE_ENDPOINT, mockTransport);
    }

    private static class TestedAbstractArtifact extends AbstractArtifact {
        public TestedAbstractArtifact(String name) {
            super(name);
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
        public Command getBackupCommand(BackupConfig backupConfig, ConfigUtil codenvyConfigUtil) throws IOException {
            return null;
        }

        @Override
        public Command getRestoreCommand(BackupConfig backupConfig, ConfigUtil codenvyConfigUtil) throws IOException {
            return null;
        }
    }
}
