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

import com.codenvy.im.BaseTest;
import com.codenvy.im.commands.Command;
import com.codenvy.im.managers.BackupConfig;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;

import org.apache.commons.io.FileUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.codenvy.im.artifacts.ArtifactProperties.PREVIOUS_VERSION_PROPERTY;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;


/** @author Dmytro Nochevnov */
public class TestAbstractArtifact extends BaseTest {
    private AbstractArtifact spyTestArtifact;

    private static final String  TEST_ARTIFACT_NAME  = "test_artifact_name";
    private static final String  TEST_VERSION_STRING = "1.0.0";
    private static final Version TEST_VERSION        = Version.valueOf(TEST_VERSION_STRING);

    @Mock
    private HttpTransport mockTransport;
    @Mock
    private ConfigManager configManager;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        spyTestArtifact = spy(new TestedAbstractArtifact(TEST_ARTIFACT_NAME, UPDATE_API_ENDPOINT, DOWNLOAD_DIR, mockTransport, configManager));
    }

    @Test
    public void testGetArtifactPropertiesFromPropertyFile() throws Exception {
        Path propertiesFile = Paths.get(DOWNLOAD_DIR, spyTestArtifact.getName(), TEST_VERSION.toString(), Artifact.ARTIFACT_PROPERTIES_FILE_NAME);
        Files.createDirectories(propertiesFile.getParent());
        FileUtils.write(propertiesFile.toFile(), "file=file1\n"
                                                 + "md5=a\n");

        Map m = spyTestArtifact.fetchPropertiesFromLocalFile(TEST_VERSION);
        assertTrue(m.containsKey("file"));
        assertTrue(m.containsKey("md5"));
        assertEquals(m.get("file"), "file1");
        assertEquals(m.get("md5"), "a");
    }

    @Test
    public void testGetArtifactPropertiesFromUpdateServer() throws Exception {
        doReturn("{\"file\":\"file1\", \"md5\":\"a\"}").when(mockTransport).doGet(endsWith(TEST_ARTIFACT_NAME + "/" + TEST_VERSION_STRING));

        Map m = spyTestArtifact.getProperties(TEST_VERSION);
        assertTrue(m.containsKey("file"));
        assertTrue(m.containsKey("md5"));
        assertEquals(m.get("file"), "file1");
        assertEquals(m.get("md5"), "a");
    }

    @Test
    public void testIsInstallableWhenPreviousVersionOfNewArtifactIsUnknown() throws IOException {
        Version newVersion = Version.valueOf("2.0.0");

        doReturn(TEST_VERSION).when(spyTestArtifact).getInstalledVersion();
        doReturn("{}").when(mockTransport)
                      .doGet(endsWith(TEST_ARTIFACT_NAME + "/" + TEST_VERSION));  // allowed previous version of new artifact is unknown
        doReturn("{}").when(mockTransport)
                      .doGet(endsWith(TEST_ARTIFACT_NAME + "/" + newVersion));  // allowed previous version of new artifact is unknown

        assertFalse(spyTestArtifact.isInstallable(TEST_VERSION));
        assertTrue(spyTestArtifact.isInstallable(newVersion));
    }

    @Test
    public void testIsInstallableWhenThereIsNoInstalledArtifact() throws IOException {
        doReturn(null).when(spyTestArtifact).getInstalledVersion();
        doReturn("{\"previous-version\":\"" + TEST_VERSION + "\"}").when(mockTransport)
                                                                   .doGet(endsWith(TEST_ARTIFACT_NAME + "/" + Version.valueOf("2.0.0")));

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
        doReturn(Version.valueOf("1.0.0")).when(spyTestArtifact).getInstalledVersion();
        doReturn(new ArrayList<Map.Entry<Artifact, Version>>() {{
            add(new AbstractMap.SimpleEntry<Artifact, Version>(spyTestArtifact, Version.valueOf("1.0.1")));
            add(new AbstractMap.SimpleEntry<Artifact, Version>(spyTestArtifact, Version.valueOf("1.0.2")));
            add(new AbstractMap.SimpleEntry<Artifact, Version>(spyTestArtifact, Version.valueOf("1.0.3")));
        }}).when(spyTestArtifact).getAllUpdates();
        doReturn("1\\.0\\.(.*)").when(spyTestArtifact).getProperty(Version.valueOf("1.0.1"), PREVIOUS_VERSION_PROPERTY);
        doReturn("1\\.0\\.(.*)").when(spyTestArtifact).getProperty(Version.valueOf("1.0.2"), PREVIOUS_VERSION_PROPERTY);
        doReturn("1\\.0\\.2").when(spyTestArtifact).getProperty(Version.valueOf("1.0.3"), PREVIOUS_VERSION_PROPERTY);

        Version version = spyTestArtifact.getLatestInstallableVersion();

        assertNotNull(version);
        assertEquals(version, Version.valueOf("1.0.2"));
    }

    @Test
    public void testGetNullLatestInstallableVersion() throws Exception {
        doReturn(Version.valueOf("1.0.0")).when(spyTestArtifact).getInstalledVersion();
        doReturn(new ArrayList<Map.Entry<Artifact, Version>>() {{
            add(new AbstractMap.SimpleEntry<Artifact, Version>(spyTestArtifact, Version.valueOf("1.0.3")));
        }}).when(spyTestArtifact).getAllUpdates();
        doReturn("1\\.0\\.2").when(spyTestArtifact).getProperty(Version.valueOf("1.0.3"), PREVIOUS_VERSION_PROPERTY);

        Version version = spyTestArtifact.getLatestInstallableVersion();

        assertNull(version);
    }

    private static class TestedAbstractArtifact extends AbstractArtifact {
        public TestedAbstractArtifact(String name,
                                      String updateEndpoint,
                                      String downloadDir,
                                      HttpTransport transport,
                                      ConfigManager configManager) {
            super(name, updateEndpoint, downloadDir, transport, configManager);
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
        public List<String> getInstallInfo(InstallType installType) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> getUpdateInfo(InstallType installType) throws IOException {
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

        @Override
        public void updateConfig(Map<String, String> properties) throws IOException {
        }
    }
}
