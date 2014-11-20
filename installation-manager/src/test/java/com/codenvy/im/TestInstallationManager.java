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
import com.codenvy.im.installer.InstallOptions;
import com.codenvy.im.installer.InstallStartedException;
import com.codenvy.im.installer.Installer;
import com.codenvy.im.restlet.InstallationManagerConfig;
import com.codenvy.im.user.UserCredentials;
import com.codenvy.im.utils.AccountUtils;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.HttpTransportConfiguration;
import com.codenvy.im.utils.Version;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.codenvy.im.artifacts.ArtifactProperties.AUTHENTICATION_REQUIRED_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.SUBSCRIPTION_PROPERTY;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author Anatoliy Bazko
 */
public class TestInstallationManager {

    public static final String UPDATE_ENDPOINT = "http://update.com/endpoint";
    public static final String DOWNLOAD_DIR = "target/download";
    private Artifact cdecArtifact;
    private Artifact installManagerArtifact;

    private InstallationManagerImpl manager;
    private HttpTransport           transport;

    private UserCredentials testCredentials;

    @BeforeMethod
    public void setUp() throws Exception {
        transport = mock(HttpTransport.class);

        installManagerArtifact = spy(new InstallManagerArtifact());
        cdecArtifact = spy(new CDECArtifact(UPDATE_ENDPOINT, transport));

        manager = spy(new InstallationManagerImpl("api/endpoint",
                                                  UPDATE_ENDPOINT,
                                                  DOWNLOAD_DIR,
                                                  new HttpTransportConfiguration("", "0"),
                                                  transport,
                                                  new HashSet<>(Arrays.asList(installManagerArtifact, cdecArtifact))));

        testCredentials = new UserCredentials("auth token", "accountId");
    }

    @Test
    public void testInstallArtifact() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");

        doReturn(new TreeMap<Version, Path>() {{
            put(version100, null);
        }}).when(cdecArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        doReturn(true).when(cdecArtifact).isInstallable(version100, testCredentials.getToken());

        doNothing().when(cdecArtifact).install(any(Path.class), any(InstallOptions.class));

        doReturn(new TreeMap<Version, Path>(){{
            put(version100, null);
        }}).when(cdecArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        manager.install(testCredentials.getToken(), cdecArtifact, version100, null);
        verify(cdecArtifact).install(any(Path.class), any(InstallOptions.class));
    }

    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = "Can not install the artifact 'installation-manager' version '2.0.0'.")
    public void testNotInstallableUpdate() throws Exception {
        final Version version200 = Version.valueOf("2.0.0");

        doReturn(new TreeMap<Version, Path>() {{
            put(version200, null);
        }}).when(installManagerArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        doReturn(false).when(installManagerArtifact).isInstallable(version200, testCredentials.getToken());

        manager.install(testCredentials.getToken(), installManagerArtifact, version200, null);
        verify(installManagerArtifact, never()).install(any(Path.class), null);
    }

    @Test(expectedExceptions = FileNotFoundException.class)
    public void testInstallArtifactErrorIfBinariesNotFound() throws Exception {
        doReturn(null).when(cdecArtifact).getInstalledVersion(testCredentials.getToken());

        manager.install(testCredentials.getToken(), cdecArtifact, Version.valueOf("2.0.0"), null);
    }

    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = "Can not install the artifact 'cdec' version '1.0.0'.")
    public void testUpdateCdecErrorIfRequestedVersionIsNotInstallable() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");
        InstallOptions options = new InstallOptions().setType(Installer.Type.CDEC_SINGLE_NODE_WITH_PUPPET_MASTER);

        doReturn(new TreeMap<Version, Path>() {{
            put(version100, null);
        }}).when(cdecArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        doReturn(false).when(cdecArtifact).isInstallable(version100, testCredentials.getToken());

        doNothing().when(cdecArtifact).install(null, options);

        manager.install(testCredentials.getToken(), cdecArtifact, version100, options);
    }

    @Test
    public void testInstallWithInstallOptions() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");

        doReturn(new TreeMap<Version, Path>() {{
            put(version100, null);
        }}).when(cdecArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        doReturn(true).when(cdecArtifact).isInstallable(version100, testCredentials.getToken());

        InstallOptions testOptions = new InstallOptions()
            .setType(Installer.Type.CDEC_SINGLE_NODE_WITH_PUPPET_MASTER);

        doThrow(new InstallStartedException(testOptions)).when(cdecArtifact).install(null, testOptions);

        try {
            manager.install(testCredentials.getToken(), cdecArtifact, version100, testOptions);
        } catch(InstallStartedException e) {
            assertEquals(e.getInstallOptions(), testOptions);
            return;
        }

        fail();
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

        doReturn(new TreeMap<Version, Path>(){{
            put(version200, null);
        }}).when(cdecArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        doReturn(new TreeMap<Version, Path>(){{
            put(version100, null);
        }}).when(installManagerArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        Map<Artifact, Version> artifactsToDownload = manager.getUpdatesToDownload(null, null, testCredentials.getToken());
        assertEquals(artifactsToDownload.size(), 0);
    }

    @Test
    public void testGetDownloadedArtifacts() throws Exception {
        doReturn(new TreeMap<Version, Path>(){{
            put(Version.valueOf("1.0.0"), Paths.get("file1"));
            put(Version.valueOf("1.0.1"), Paths.get("file2"));
        }}).when(cdecArtifact).getDownloadedVersions(any(Path.class), anyString(), any(HttpTransport.class));

        doReturn(new TreeMap<Version, Path>(){{
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
