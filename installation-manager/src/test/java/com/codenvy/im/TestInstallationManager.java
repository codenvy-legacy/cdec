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
package com.codenvy.im;

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.install.CdecInstallOptions;
import com.codenvy.im.install.DefaultOptions;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.install.Installer;
import com.codenvy.im.restlet.InstallationManagerConfig;
import com.codenvy.im.user.UserCredentials;
import com.codenvy.im.utils.AccountUtils;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.HttpTransportConfiguration;
import com.codenvy.im.utils.Version;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
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
import static org.mockito.Mockito.mock;
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
    private HttpTransport           transport;
    private Installer               installer;

    private UserCredentials testCredentials;

    @BeforeMethod
    public void setUp() throws Exception {
        transport = mock(HttpTransport.class);
        installer = mock(Installer.class);

        installManagerArtifact = spy(new InstallManagerArtifact());
        cdecArtifact = spy(new CDECArtifact(UPDATE_ENDPOINT, transport));

        manager = spy(new InstallationManagerImpl("api/endpoint",
                                                  UPDATE_ENDPOINT,
                                                  DOWNLOAD_DIR,
                                                  new HttpTransportConfiguration("", "0"),
                                                  transport,
                                                  installer,
                                                  new HashSet<>(Arrays.asList(installManagerArtifact, cdecArtifact))));

        testCredentials = new UserCredentials("auth token", "accountId");
    }

    @AfterMethod
    public void tearDown() throws Exception {
        deleteDirectory(Paths.get("target", "download").toFile());
    }

    @Test(expectedExceptions = IllegalStateException.class,
            expectedExceptionsMessageRegExp = "Can not install the artifact 'installation-manager' version '2.10.1'.")
    public void testReInstallAlreadyInstalledArtifact() throws Exception {
        final Version version2101 = Version.valueOf("2.10.1");

        doReturn(new HashMap<Artifact, SortedMap<Version, Path>>() {{
            put(installManagerArtifact, new TreeMap<Version, Path>() {{
                put(version2101, Paths.get("target/download/installation-manager/2.10.1/file1"));
            }});
        }}).when(manager).getDownloadedArtifacts();
        doReturn(version2101).when(installManagerArtifact).getInstalledVersion(testCredentials.getToken());
        manager.install(testCredentials.getToken(), installManagerArtifact, version2101, new CdecInstallOptions());
    }

    @Test
    public void testInstallArtifact() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");
        final InstallOptions options = new CdecInstallOptions();
        final Path pathToBinaries = Paths.get("some path");

        doReturn(new TreeMap<Version, Path>() {{
            put(version100, null);
        }}).when(cdecArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        doReturn(true).when(cdecArtifact).isInstallable(version100, testCredentials.getToken());

        doNothing().when(installer).install(any(Artifact.class), any(Path.class), any(InstallOptions.class));

        doReturn(new TreeMap<Version, Path>() {{
            put(version100, pathToBinaries);
        }}).when(cdecArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        manager.install(testCredentials.getToken(), cdecArtifact, version100, options);
        verify(installer).install(cdecArtifact, pathToBinaries, options);
    }

    @Test(expectedExceptions = FileNotFoundException.class,
            expectedExceptionsMessageRegExp = "Binaries to install artifact 'installation-manager' version '2.10.1' not found")
    public void testNotInstallableUpdate() throws Exception {
        final Version version200 = Version.valueOf("2.0.0");

        doReturn(new TreeMap<Version, Path>() {{
            put(version200, null);
        }}).when(installManagerArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        manager.install("auth token", installManagerArtifact, Version.valueOf("2.10.1"), new DefaultOptions());
        doReturn(false).when(installManagerArtifact).isInstallable(version200, testCredentials.getToken());

        manager.install(testCredentials.getToken(), installManagerArtifact, version200, null);
        verify(installer, never()).install(any(Artifact.class), Paths.get("some path"), any(InstallOptions.class));
    }

    @Test(expectedExceptions = FileNotFoundException.class)
    public void testInstallArtifactErrorIfBinariesNotFound() throws Exception {
        doReturn(null).when(cdecArtifact).getInstalledVersion(testCredentials.getToken());
        manager.install("auth token", cdecArtifact, Version.valueOf("2.10.1"), new CdecInstallOptions());
    }

    @Test(expectedExceptions = IllegalStateException.class,
            expectedExceptionsMessageRegExp = "Can not install the artifact 'installation-manager' version '2.10.0'.")
    public void testUpdateIMErrorIfInstalledIMHasGreaterVersion() throws Exception {
        final Version version2100 = Version.valueOf("2.10.0");
        Version version2101 = Version.valueOf("2.10.1");

        when(transport.doOption(endsWith("api/"), anyString())).thenReturn("{\"ideVersion\":\"1.0.0\"}");
        doReturn(new HashMap<Artifact, SortedMap<Version, Path>>() {{
            put(installManagerArtifact, new TreeMap<Version, Path>() {{
                put(version2100, Paths.get("target/download/installation-manager/2.10.0/file1"));
            }});
        }}).when(manager).getDownloadedArtifacts();
        doReturn(version2101).when(installManagerArtifact).getInstalledVersion(testCredentials.getToken());

        manager.install("auth token", installManagerArtifact, version2100, new DefaultOptions());
    }

    @Test(expectedExceptions = IllegalStateException.class,
            expectedExceptionsMessageRegExp = "Can not install the artifact 'cdec' version '1.0.0'.")
    public void testUpdateCdecErrorIfInstalledCdecHasGreaterVersion() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");
        CdecInstallOptions options = new CdecInstallOptions();
        options.setCdecInstallType(CdecInstallOptions.CDECInstallType.SINGLE_NODE);
        options.setStep(1);

        doReturn(new TreeMap<Version, Path>() {{
            put(version100, Paths.get("some path"));
        }}).when(cdecArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        doReturn(false).when(cdecArtifact).isInstallable(version100, testCredentials.getToken());
        doNothing().when(installer).install(any(Artifact.class), any(Path.class), any(InstallOptions.class));

        manager.install(testCredentials.getToken(), cdecArtifact, version100, options);
    }

    @Test
    public void testInstallWithInstallOptions() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");

        doReturn(new TreeMap<Version, Path>() {{
            put(version100, Paths.get("some path"));
        }}).when(cdecArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        doReturn(true).when(cdecArtifact).isInstallable(version100, testCredentials.getToken());

        CdecInstallOptions testOptions = new CdecInstallOptions();
        testOptions.setCdecInstallType(CdecInstallOptions.CDECInstallType.SINGLE_NODE);
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

        doReturn(Collections.emptyMap()).when(manager).getInstalledArtifacts(testCredentials.getToken());
        doReturn(version2101).when(installManagerArtifact).getInstalledVersion(testCredentials.getToken());
        doNothing().when(installer).install(any(Artifact.class), any(Path.class), any(InstallOptions.class));

        manager.install("auth token", installManagerArtifact, version2102, new DefaultOptions());
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

        manager.download(testCredentials, cdecArtifact, version100);
    }

    @Test
    public void testGetUpdates() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");
        final Version version200 = Version.valueOf("2.0.0");

        doReturn(version100).when(cdecArtifact)
                            .getLatestInstallableVersionToDownload(testCredentials.getToken(), UPDATE_ENDPOINT, transport);

        doReturn(version200).when(installManagerArtifact)
                            .getLatestInstallableVersionToDownload(testCredentials.getToken(), UPDATE_ENDPOINT, transport);

        Map<Artifact, Version> updates = manager.getUpdates(testCredentials.getToken());
        assertEquals(updates.size(), 2);
        assertEquals(updates.toString(), "{cdec=1.0.0, installation-manager=2.0.0}");
    }

    @Test
    public void testGetInstalledArtifacts() throws Exception {
        Version version100 = Version.valueOf("1.0.0");
        Version version200 = Version.valueOf("2.0.0");

        doReturn(version100).when(cdecArtifact).getInstalledVersion(testCredentials.getToken());
        doReturn(version200).when(installManagerArtifact).getInstalledVersion(testCredentials.getToken());

        Map<Artifact, Version> installedArtifacts = manager.getInstalledArtifacts(testCredentials.getToken());

        assertEquals(installedArtifacts.size(), 2);
        assertEquals(installedArtifacts.toString(), "{cdec=1.0.0, installation-manager=2.0.0}");
    }

    @Test
    public void testUpdatesToDownload() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");
        final Version version200 = Version.valueOf("2.0.0");

        doReturn(new TreeMap<Artifact, Version>() {{
            put(cdecArtifact, version100);
            put(installManagerArtifact, version200);
        }}).when(manager).getUpdates(testCredentials.getToken());

        Map<Artifact, Version> artifactsToDownload = manager.getUpdatesToDownload(null, null, testCredentials.getToken());
        assertEquals(artifactsToDownload.size(), 2);
        assertEquals(artifactsToDownload.toString(), "{installation-manager=2.0.0, cdec=1.0.0}");
    }

    @Test
    public void testAlreadyDownloadedUpdatesToDownload() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");
        final Version version200 = Version.valueOf("2.0.0");

        doReturn(new TreeMap<Artifact, Version>() {{
            put(cdecArtifact, version200);
            put(installManagerArtifact, version100);
        }}).when(manager).getUpdates(testCredentials.getToken());

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
        doReturn("{\"file\":\"file1\", \"md5\":\"d41d8cd98f00b204e9800998ecf8427e\"}").when(transport).doGet(endsWith("cdec/1.0.1"));
        doReturn("{\"file\":\"file2\", \"md5\":\"d41d8cd98f00b204e9800998ecf8427e\"}").when(transport).doGet(endsWith("cdec/1.0.2"));
        doReturn("{\"file\":\"file3\", \"md5\":\"d41d8cd98f00b204e9800998ecf8427e\"}").when(transport).doGet(endsWith("installation-manager/2.0.1"));

        Path file1 = Paths.get("target", "download", cdecArtifact.getName(), "1.0.1", "file1");
        Path file2 = Paths.get("target", "download", cdecArtifact.getName(), "1.0.2", "file2");
        Path file3 = Paths.get("target", "download", installManagerArtifact.getName(), "2.0.1", "file3");
        Files.createDirectories(file1.getParent());
        Files.createDirectories(file2.getParent());
        Files.createDirectories(file3.getParent());
        Files.createFile(file1);
        Files.createFile(file2);
        Files.createFile(file3);

        doReturn(new TreeMap<Version, Path>() {{
            put(Version.valueOf("1.0.0"), Paths.get("file1"));
            put(Version.valueOf("1.0.1"), Paths.get("file2"));
        }}).when(cdecArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        doReturn(new TreeMap<Version, Path>() {{
            put(Version.valueOf("2.0.0"), Paths.get("file3"));
        }}).when(installManagerArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        Map<Artifact, SortedMap<Version, Path>> artifacts = manager.getDownloadedArtifacts();
        assertEquals(artifacts.size(), 2);

        // check order
        assertEquals(artifacts.toString(), "{cdec={" +
                                           "1.0.0=file1, " +
                                           "1.0.1=file2" +
                                           "}, " +
                                           "installation-manager={" +
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
        doNothing().when(manager).storeProperty(anyString(), anyString());
        doNothing().when(manager).validatePath(any(Path.class));

        InstallationManagerConfig config = new InstallationManagerConfig();
        config.setDownloadDir("target/new-download");
        config.setProxyPort("1000");
        config.setProxyUrl("localhost");
        manager.setConfig(config);

        Map<String, String> m = manager.getConfig();
        assertEquals(m.size(), 4);
        assertTrue(m.containsValue("target/new-download"));
        assertTrue(m.containsValue("1000"));
        assertTrue(m.containsValue("localhost"));
        assertTrue(m.containsValue("http://update.com"));

        config.setDownloadDir("target/download");
        config.setProxyPort("");
        manager.setConfig(config);

        m = manager.getConfig();
        assertEquals(m.size(), 3);
        assertTrue(m.containsValue("target/download"));
        assertTrue(m.containsValue("localhost"));
        assertTrue(m.containsValue("http://update.com"));

        config.setProxyUrl("");
        manager.setConfig(config);

        m = manager.getConfig();
        assertEquals(m.size(), 2);
        assertTrue(m.containsValue("target/download"));
        assertTrue(m.containsValue("http://update.com"));
    }

    @Test(expectedExceptions = IOException.class)
    public void testSetConfigErrorIfDirectoryCantCreateDirectory() throws Exception {
        doNothing().when(manager).storeProperty(anyString(), anyString());

        InstallationManagerConfig config = new InstallationManagerConfig();
        config.setDownloadDir("/hello/world");

        manager.setConfig(config);
    }

    @Test(expectedExceptions = IOException.class)
    public void testSetConfigErrorIfDirectoryIsNotAbsolute() throws Exception {
        doNothing().when(manager).storeProperty(anyString(), anyString());

        InstallationManagerConfig config = new InstallationManagerConfig();
        config.setDownloadDir("hello");

        manager.setConfig(config);
    }

    @Test
    public void testIsInstallable() throws IOException {
        final Version version200 = Version.valueOf("2.0.0");
        final Version version201 = Version.valueOf("2.0.1");

        doReturn(true).when(installManagerArtifact).isInstallable(version200, testCredentials.getToken());
        assertTrue(manager.isInstallable(installManagerArtifact, version200, testCredentials.getToken()));

        doReturn(false).when(installManagerArtifact).isInstallable(version201, testCredentials.getToken());
        assertFalse(manager.isInstallable(installManagerArtifact, version201, testCredentials.getToken()));
    }
}
