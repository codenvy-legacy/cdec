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
package com.codenvy.im;

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.ArtifactProperties;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.backup.BackupConfig;
import com.codenvy.im.backup.BackupManager;
import com.codenvy.im.config.ConfigUtil;
import com.codenvy.im.facade.UserCredentials;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.install.InstallType;
import com.codenvy.im.install.Installer;
import com.codenvy.im.node.NodeConfig;
import com.codenvy.im.node.NodeManager;
import com.codenvy.im.utils.AccountUtils;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableMap;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.codenvy.im.artifacts.ArtifactProperties.AUTHENTICATION_REQUIRED_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.SUBSCRIPTION_PROPERTY;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Anatoliy Bazko
 */
public class TestInstallationManager {

    public static final String UPDATE_ENDPOINT = "http://update.com/endpoint";
    public static final String DOWNLOAD_DIR    = "target/download";
    private Artifact cdecArtifact;
    private Artifact installManagerArtifact;

    private InstallationManagerImpl manager;
    private UserCredentials         testCredentials;

    @Mock
    private HttpTransport transport;
    @Mock
    private Installer     installer;
    @Mock
    private NodeManager   mockNodeManager;
    @Mock
    private BackupManager mockBackupManager;
    @Mock
    private ConfigUtil    configUtil;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        installManagerArtifact = spy(new InstallManagerArtifact());
        cdecArtifact = spy(new CDECArtifact(transport, configUtil));

        manager = spy(new InstallationManagerImpl(
            UPDATE_ENDPOINT,
            DOWNLOAD_DIR,
            transport,
            installer,
            new HashSet<>(Arrays.asList(installManagerArtifact, cdecArtifact)),
            mockNodeManager,
            mockBackupManager));

        testCredentials = new UserCredentials("auth token", "accountId");
    }

    @AfterMethod
    public void tearDown() throws Exception {
        deleteDirectory(Paths.get("target", "download").toFile());
    }

    @Test(expectedExceptions = IOException.class)
    public void testInitializationIfDownloadDirectoryNotExist() throws IOException {
        new InstallationManagerImpl("", "/home/bla-bla", null, null, Collections.<Artifact>emptySet(), null, null);
    }

    @Test(expectedExceptions = IOException.class)
    public void testInitializationIfWrongPermission() throws Exception {
        new InstallationManagerImpl("", "/root", null, null, Collections.<Artifact>emptySet(), null, null);
    }

    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = "Can not install the artifact '" + InstallManagerArtifact.NAME + "' version '2.10.1'.")
    public void testReInstallAlreadyInstalledArtifact() throws Exception {
        final Version version2101 = Version.valueOf("2.10.1");

        doReturn(new HashMap<Artifact, SortedMap<Version, Path>>() {{
            put(installManagerArtifact, new TreeMap<Version, Path>() {{
                put(version2101, Paths.get("target/download/" + InstallManagerArtifact.NAME + "/2.10.1/file1"));
            }});
        }}).when(manager).getDownloadedArtifacts();

        doReturn(false).when(installManagerArtifact).isInstallable(version2101, UPDATE_ENDPOINT, transport);

        doReturn(version2101).when(installManagerArtifact).getInstalledVersion();

        manager.install(testCredentials.getToken(), installManagerArtifact, version2101, new InstallOptions());
    }

    @Test
    public void testInstallArtifact() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");
        final InstallOptions options = new InstallOptions();
        final Path pathToBinaries = Paths.get("some path");

        doReturn(new TreeMap<Version, Path>() {{
            put(version100, null);
        }}).when(cdecArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        doReturn(true).when(cdecArtifact).isInstallable(version100, UPDATE_ENDPOINT, transport);

        doNothing().when(installer).install(any(Artifact.class), any(Version.class), any(Path.class), any(InstallOptions.class));

        doReturn(new TreeMap<Version, Path>() {{
            put(version100, pathToBinaries);
        }}).when(cdecArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        manager.install(testCredentials.getToken(), cdecArtifact, version100, options);
        verify(installer).install(cdecArtifact, version100, pathToBinaries, options);
    }

    @Test(expectedExceptions = FileNotFoundException.class,
            expectedExceptionsMessageRegExp = "Binaries to install artifact '" + InstallManagerArtifact.NAME + "' version '2.10.1' not found")
    public void testNotInstallableUpdate() throws Exception {
        final Version version200 = Version.valueOf("2.0.0");

        doReturn(new TreeMap<Version, Path>() {{
            put(version200, null);
        }}).when(installManagerArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        manager.install("auth token", installManagerArtifact, Version.valueOf("2.10.1"), new InstallOptions());
        doReturn(false).when(installManagerArtifact).isInstallable(version200, UPDATE_ENDPOINT, transport);

        manager.install(testCredentials.getToken(), installManagerArtifact, version200, null);
        verify(installer, never()).install(any(Artifact.class), any(Version.class), Paths.get("some path"), any(InstallOptions.class));
    }

    @Test(expectedExceptions = FileNotFoundException.class)
    public void testInstallArtifactErrorIfBinariesNotFound() throws Exception {
        doReturn(null).when(cdecArtifact).getInstalledVersion();
        manager.install("auth token", cdecArtifact, Version.valueOf("2.10.1"), new InstallOptions());
    }

    @Test(expectedExceptions = IllegalStateException.class,
            expectedExceptionsMessageRegExp = "Can not install the artifact '" + InstallManagerArtifact.NAME + "' version '2.10.0'.")
    public void testUpdateIMErrorIfInstalledIMHasGreaterVersion() throws Exception {
        final Version version2100 = Version.valueOf("2.10.0");
        Version version2101 = Version.valueOf("2.10.1");

        when(transport.doOption(endsWith("api/"), anyString())).thenReturn("{\"ideVersion\":\"1.0.0\"}");
        doReturn(new HashMap<Artifact, SortedMap<Version, Path>>() {{
            put(installManagerArtifact, new TreeMap<Version, Path>() {{
                put(version2100, Paths.get("target/download/" + InstallManagerArtifact.NAME + "/2.10.0/file1"));
            }});
        }}).when(manager).getDownloadedArtifacts();
        doReturn(version2101).when(installManagerArtifact).getInstalledVersion();

        doReturn(false).when(installManagerArtifact).isInstallable(version2100, UPDATE_ENDPOINT, transport);

        manager.install("auth token", installManagerArtifact, version2100, new InstallOptions());
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = "Can not install the artifact 'codenvy' version '1.0" +
                                                                                              ".0'.")
    public void testInstallZeroInstallationStep() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");
        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.SINGLE_SERVER);
        options.setStep(0);

        doReturn(new TreeMap<Version, Path>() {{
            put(version100, Paths.get("some path"));
        }}).when(cdecArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        doReturn(false).when(cdecArtifact).isInstallable(version100, UPDATE_ENDPOINT, transport);
        doNothing().when(installer).install(any(Artifact.class), any(Version.class), any(Path.class), any(InstallOptions.class));

        manager.install(testCredentials.getToken(), cdecArtifact, version100, options);
    }

    @Test
    public void testInstallNonZeroInstallationStep() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");
        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.SINGLE_SERVER);
        options.setStep(1);

        doReturn(new TreeMap<Version, Path>() {{
            put(version100, Paths.get("some path"));
        }}).when(cdecArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        doReturn(false).when(cdecArtifact).isInstallable(version100, UPDATE_ENDPOINT, transport);
        doNothing().when(installer).install(any(Artifact.class), any(Version.class), any(Path.class), any(InstallOptions.class));

        manager.install(testCredentials.getToken(), cdecArtifact, version100, options);

        verify(installer).install(cdecArtifact, version100, Paths.get("some path"), options);
    }

    @Test
    public void testInstallWithInstallOptions() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");

        doReturn(new TreeMap<Version, Path>() {{
            put(version100, Paths.get("some path"));
        }}).when(cdecArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        doReturn(true).when(cdecArtifact).isInstallable(version100, UPDATE_ENDPOINT, transport);

        InstallOptions testOptions = new InstallOptions();
        testOptions.setInstallType(InstallType.SINGLE_SERVER);
        testOptions.setStep(1);

        when(transport.doOption(endsWith("api/"), anyString())).thenReturn("{\"ideVersion\":\"3.0.0\"}");

        manager.install("auth token", cdecArtifact, version100, testOptions);
    }

    @Test
    public void testCheckEnoughDiskSpace() throws Exception {
        manager.checkEnoughDiskSpace(100);
    }

    @Test(expectedExceptions = IOException.class,
            expectedExceptionsMessageRegExp = "Not enough disk space. Required [0-9]* bytes but available only [0-9]* bytes")
    public void testCheckEnoughDiskSpaceThrowIOException() throws Exception {
        manager.checkEnoughDiskSpace(Long.MAX_VALUE);
    }

    @Test
    public void testInstallArtifactNewlyArtifact() throws Exception {
        final Version version2102 = Version.valueOf("2.10.2");
        final Version version2101 = Version.valueOf("2.10.1");

        doReturn(new HashMap<Artifact, SortedMap<Version, Path>>() {{
            put(installManagerArtifact, new TreeMap<Version, Path>() {{
                put(version2102, Paths.get("target/download/installation-manager/2.10.2/file1"));
            }});
        }}).when(manager).getDownloadedArtifacts();

        doReturn(Collections.emptyMap()).when(manager).getInstalledArtifacts();

        doReturn(version2101).when(installManagerArtifact).getInstalledVersion();

        doReturn(true).when(installManagerArtifact).isInstallable(version2102, UPDATE_ENDPOINT, transport);

        doNothing().when(installer).install(any(Artifact.class), any(Version.class), any(Path.class), any(InstallOptions.class));

        manager.install("auth token", installManagerArtifact, version2102, new InstallOptions());
    }

    @Test
    public void testDownloadVersion() throws Exception {
        Version version100 = Version.valueOf("1.0.0");
        when(transport.doGet("api/endpoint/account", testCredentials.getToken()))
                .thenReturn("[{"
                            + "\"roles\":[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                            + "\"accountReference\":{\"id\":\"" + testCredentials.getAccountId() + "\"}"
                            + "}]");

        when(transport.doGet("api/endpoint/account/" + testCredentials.getAccountId() + "/subscriptions", testCredentials.getToken()))
                .thenReturn("[{serviceId:\"OnPremises\"}]");

        when(transport.doGet(endsWith("repository/properties/" + cdecArtifact.getName() + "/" + version100.toString())))
                .thenReturn(String.format("{\"%s\": \"true\", \"%s\":\"OnPremises\"}", AUTHENTICATION_REQUIRED_PROPERTY, SUBSCRIPTION_PROPERTY));

        manager.download(testCredentials.getToken(), cdecArtifact, version100);
    }

    @Test
    public void testGetUpdates() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");
        final Version version200 = Version.valueOf("2.0.0");

        doReturn(version100).when(cdecArtifact)
                            .getLatestInstallableVersion(UPDATE_ENDPOINT, transport);

        doReturn(version200).when(installManagerArtifact)
                            .getLatestInstallableVersion(UPDATE_ENDPOINT, transport);

        Map<Artifact, Version> updates = manager.getUpdates();
        assertEquals(updates.size(), 2);
        assertEquals(updates.toString(), "{codenvy=1.0.0, " + InstallManagerArtifact.NAME + "=2.0.0}");
    }

    @Test
    public void testGetInstalledArtifacts() throws Exception {
        Version version100 = Version.valueOf("1.0.0");
        Version version200 = Version.valueOf("2.0.0");

        doReturn(version100).when(cdecArtifact).getInstalledVersion();
        doReturn(version200).when(installManagerArtifact).getInstalledVersion();

        Map<Artifact, Version> installedArtifacts = manager.getInstalledArtifacts();

        assertEquals(installedArtifacts.size(), 2);
        assertEquals(installedArtifacts.toString(), "{codenvy=1.0.0, " + InstallManagerArtifact.NAME + "=2.0.0}");
    }

    @Test
    public void testGetUpdatesToDownload() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");
        final Version version200 = Version.valueOf("2.0.0");

        doReturn(new TreeMap<Artifact, Version>() {{
            put(cdecArtifact, version100);
            put(installManagerArtifact, version200);
        }}).when(manager).getUpdates();

        Map<Artifact, Version> artifactsToDownload = manager.getUpdatesToDownload(null, null, testCredentials.getToken());
        assertEquals(artifactsToDownload.size(), 2);
        assertEquals(artifactsToDownload.toString(), "{" + InstallManagerArtifact.NAME + "=2.0.0, codenvy=1.0.0}");
    }

    @Test
    public void testGetUpdatesToDownloadForSpecificArtifact() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");

        doReturn(version100).when(cdecArtifact).getLatestInstallableVersion(UPDATE_ENDPOINT, transport);

        Map<Artifact, Version> artifactsToDownload = manager.getUpdatesToDownload(cdecArtifact, null, testCredentials.getToken());
        assertEquals(artifactsToDownload.size(), 1);
        assertEquals(artifactsToDownload.toString(), "{codenvy=1.0.0}");
    }

    @Test
    public void testAlreadyDownloadedUpdatesToDownload() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");
        final Version version200 = Version.valueOf("2.0.0");

        doReturn(new TreeMap<Artifact, Version>() {{
            put(cdecArtifact, version200);
            put(installManagerArtifact, version100);
        }}).when(manager).getUpdates();

        doReturn(new TreeMap<Version, Path>() {{
            put(version200, null);
        }}).when(cdecArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        doReturn(new TreeMap<Version, Path>() {{
            put(version100, null);
        }}).when(installManagerArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        Map<Artifact, Version> artifactsToDownload = manager.getUpdatesToDownload(null, null, testCredentials.getToken());
        assertEquals(artifactsToDownload.size(), 0);
    }

    @Test
    public void testGetDownloadedArtifacts() throws Exception {
        doReturn(new TreeMap<Version, Path>(new Version.ReverseOrder()) {{
            put(Version.valueOf("1.0.0"), Paths.get("file1"));
            put(Version.valueOf("1.0.1"), Paths.get("file2"));
        }}).when(cdecArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        doReturn(new TreeMap<Version, Path>() {{
            put(Version.valueOf("2.0.0"), Paths.get("file3"));
        }}).when(installManagerArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        Map<Artifact, SortedMap<Version, Path>> artifacts = manager.getDownloadedArtifacts();
        assertEquals(artifacts.size(), 2);

        // check order
        assertEquals(artifacts.toString(), "{codenvy={" +
                                           "1.0.1=file2, " +
                                           "1.0.0=file1" +
                                           "}, " +
                                           InstallManagerArtifact.NAME + "={" +
                                           "2.0.0=file3" +
                                           "}}");
    }

    @Test
    public void testGetDownloadedArtifactsReturnsEmptyMap() throws Exception {
        doReturn(new TreeMap<Version, Path>()).when(cdecArtifact)
                                              .getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        doReturn(new TreeMap<Version, Path>()).when(installManagerArtifact)
                                              .getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        Map<Artifact, SortedMap<Version, Path>> m = manager.getDownloadedArtifacts();
        assertTrue(m.isEmpty());
    }

    @Test
    public void testSetAndGetConfig() throws Exception {
        doNothing().when(manager).validatePath(any(Path.class));

        Map<String, String> m = manager.getConfig();
        assertEquals(m.size(), 2);
        assertTrue(m.containsValue("target/download"));
        assertTrue(m.containsValue("http://update.com"));
    }

    @Test
    public void testIsInstallable() throws IOException {
        final Version version200 = Version.valueOf("2.0.0");
        final Version version201 = Version.valueOf("2.0.1");

        doReturn(true).when(installManagerArtifact).isInstallable(version200, UPDATE_ENDPOINT, transport);
        assertTrue(manager.isInstallable(installManagerArtifact, version200, testCredentials.getToken()));

        doReturn(false).when(installManagerArtifact).isInstallable(version201, UPDATE_ENDPOINT, transport);
        assertFalse(manager.isInstallable(installManagerArtifact, version201, testCredentials.getToken()));
    }

    @Test
    public void testGetPathToBinaries() throws Exception {
        Version version = Version.valueOf("1.0.1");
        doReturn(ImmutableMap.of(ArtifactProperties.FILE_NAME_PROPERTY, "binaries")).when(cdecArtifact)
                                                                                    .getProperties(version, UPDATE_ENDPOINT, transport);

        Path path = manager.getPathToBinaries(cdecArtifact, version);
        assertEquals(path, Paths.get(DOWNLOAD_DIR + "/codenvy/1.0.1/binaries"));
    }

    @Test
    public void testGetBinariesSize() throws Exception {
        Version version = Version.valueOf("1.0.1");
        doReturn(ImmutableMap.of(ArtifactProperties.SIZE_PROPERTY, "100")).when(cdecArtifact).getProperties(version, UPDATE_ENDPOINT, transport);

        Long binariesSize = manager.getBinariesSize(cdecArtifact, version);
        assertEquals(binariesSize.intValue(), 100);
    }

    @Test
    public void testAddNode() throws IOException {
        final NodeConfig TEST_BUILDER_NODE = new NodeConfig(NodeConfig.NodeType.BUILDER, "builder.node.com", null);
        doReturn(TEST_BUILDER_NODE).when(mockNodeManager).add("builder.node.com");
        NodeConfig result = manager.addNode("builder.node.com");
        assertEquals(result, TEST_BUILDER_NODE);
    }

    @Test(expectedExceptions = IOException.class,
            expectedExceptionsMessageRegExp = "error")
    public void testAddNodeException() throws IOException {
        doThrow(new IOException("error")).when(mockNodeManager).add("node");
        manager.addNode("node");
    }

    @Test
    public void testRemoveNode() throws IOException {
        final NodeConfig TEST_BUILDER_NODE = new NodeConfig(NodeConfig.NodeType.BUILDER, "builder.node.com", null);
        doReturn(TEST_BUILDER_NODE).when(mockNodeManager).remove("builder.node.com");
        NodeConfig result = manager.removeNode("builder.node.com");
        assertEquals(result, TEST_BUILDER_NODE);
    }

    @Test(expectedExceptions = IOException.class,
            expectedExceptionsMessageRegExp = "error")
    public void testRemoveNodeException() throws IOException {
        final String TEST_NODE_DNS = "builder.node.com";
        doThrow(new IOException("error")).when(mockNodeManager).remove(TEST_NODE_DNS);
        manager.removeNode(TEST_NODE_DNS);
    }

    @Test
    public void testBackup() throws IOException {
        String testBackupDirectory = "test/backup/directory";
        BackupConfig testBackupConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                          .setBackupDirectory(testBackupDirectory);

        BackupConfig resultBackupConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                            .setBackupDirectory(testBackupDirectory)
                                                            .setBackupFile(testBackupConfig.getBackupFile());

        doReturn(resultBackupConfig).when(mockBackupManager).backup(testBackupConfig);
        BackupConfig result = manager.backup(testBackupConfig);
        assertEquals(result, resultBackupConfig);
    }

    @Test(expectedExceptions = IOException.class,
            expectedExceptionsMessageRegExp = "error")
    public void testBackupException() throws IOException {
        BackupConfig testBackupConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME);
        doThrow(new IOException("error")).when(mockBackupManager).backup(testBackupConfig);
        mockBackupManager.backup(testBackupConfig);
    }

    @Test
    public void testRestore() throws IOException {
        String testBackupFile = "test/backup/directory/backup.tar.gz";
        BackupConfig testBackupConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                          .setBackupFile(testBackupFile);

        manager.restore(testBackupConfig);
        verify(mockBackupManager).restore(testBackupConfig);
    }

    @Test(expectedExceptions = IOException.class,
            expectedExceptionsMessageRegExp = "error")
    public void testRestoreException() throws IOException {
        String testBackupFile = "test/backup/directory/backup.tar.gz";
        BackupConfig testBackupConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                          .setBackupFile(testBackupFile);

        doThrow(new IOException("error")).when(mockBackupManager).restore(testBackupConfig);
        mockBackupManager.restore(testBackupConfig);
    }
}
