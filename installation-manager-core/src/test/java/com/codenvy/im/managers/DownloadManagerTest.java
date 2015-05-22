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
package com.codenvy.im.managers;

import com.codenvy.im.BaseTest;
import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.ArtifactProperties;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.saas.SaasAccountServiceProxy;
import com.codenvy.im.saas.SaasUserCredentials;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.codenvy.im.artifacts.ArtifactProperties.AUTHENTICATION_REQUIRED_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.SUBSCRIPTION_PROPERTY;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Anatoliy Bazko
 */
public class DownloadManagerTest extends BaseTest {

    @Mock
    private ConfigManager configManager;
    @Mock
    private HttpTransport transport;

    private CDECArtifact           cdecArtifact;
    private InstallManagerArtifact installManagerArtifact;
    private DownloadManager        downloadManager;
    private SaasUserCredentials saasUserCredentials;

    @BeforeMethod
    public void setUp() throws Exception {
        initMocks(this);
        cdecArtifact = spy(new CDECArtifact(UPDATE_API_ENDPOINT, transport, configManager));
        installManagerArtifact = spy(new InstallManagerArtifact(UPDATE_API_ENDPOINT, transport, configManager));
        downloadManager = spy(new DownloadManager(UPDATE_API_ENDPOINT,
                                                  DOWNLOAD_DIR,
                                                  transport,
                                                  ImmutableSet.<Artifact>of(cdecArtifact, installManagerArtifact)));

        saasUserCredentials = new SaasUserCredentials("auth token", "accountId");
    }

    @Test
    public void testCheckEnoughDiskSpace() throws Exception {
        downloadManager.checkEnoughDiskSpace(100);
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = "Not enough disk space.*")
    public void testCheckEnoughDiskSpaceThrowIOException() throws Exception {
        downloadManager.checkEnoughDiskSpace(Long.MAX_VALUE);
    }

    @Test(expectedExceptions = IOException.class)
    public void testInitializationIfDownloadDirectoryNotExist() throws IOException {
        new DownloadManager(UPDATE_API_ENDPOINT, "/home/bla-bla", transport, Collections.<Artifact>emptySet());
    }

    @Test(expectedExceptions = IOException.class)
    public void testInitializationIfWrongPermission() throws Exception {
        new DownloadManager(UPDATE_API_ENDPOINT, "/root", transport, Collections.<Artifact>emptySet());
    }


    @Test
    public void testGetPathToBinaries() throws Exception {
        Version version = Version.valueOf("1.0.1");
        doReturn(ImmutableMap.of(ArtifactProperties.FILE_NAME_PROPERTY, "binaries")).when(cdecArtifact).getProperties(version);

        Path path = downloadManager.getPathToBinaries(cdecArtifact, version);
        assertEquals(path, Paths.get(DOWNLOAD_DIR + "/codenvy/1.0.1/binaries"));
    }

    @Test
    public void testGetBinariesSize() throws Exception {
        Version version = Version.valueOf("1.0.1");
        doReturn(ImmutableMap.of(ArtifactProperties.SIZE_PROPERTY, "100")).when(cdecArtifact).getProperties(version);

        Long binariesSize = downloadManager.getBinariesSize(cdecArtifact, version);

        assertEquals(binariesSize.intValue(), 100);
    }

    @Test
    public void testGetDownloadedArtifacts() throws Exception {
        doReturn(new TreeMap<Version, Path>(new Version.ReverseOrder()) {{
            put(Version.valueOf("1.0.0"), Paths.get("file1"));
            put(Version.valueOf("1.0.1"), Paths.get("file2"));
        }}).when(cdecArtifact).getDownloadedVersions(any(Path.class));

        doReturn(new TreeMap<Version, Path>() {{
            put(Version.valueOf("2.0.0"), Paths.get("file3"));
        }}).when(installManagerArtifact).getDownloadedVersions(any(Path.class));

        Map<Artifact, SortedMap<Version, Path>> artifacts = downloadManager.getDownloadedArtifacts();
        assertEquals(artifacts.size(), 2);

        // check order
        assertEquals(artifacts.toString(), "{codenvy={" +
                                           "1.0.1=file2, " +
                                           "1.0.0=file1" +
                                           "}, installation-manager-cli={" +
                                           "2.0.0=file3" +
                                           "}}");
    }

    @Test
    public void testGetDownloadedArtifactsReturnsEmptyMap() throws Exception {
        doReturn(new TreeMap<Version, Path>()).when(cdecArtifact).getDownloadedVersions(any(Path.class));
        doReturn(new TreeMap<Version, Path>()).when(installManagerArtifact).getDownloadedVersions(any(Path.class));

        Map<Artifact, SortedMap<Version, Path>> m = downloadManager.getDownloadedArtifacts();
        assertTrue(m.isEmpty());
    }

    @Test
    public void testGetUpdates() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");
        final Version version200 = Version.valueOf("2.0.0");

        doReturn(version100).when(cdecArtifact)
                            .getLatestInstallableVersion();

        doReturn(version200).when(installManagerArtifact)
                            .getLatestInstallableVersion();

        Map<Artifact, Version> updates = downloadManager.getUpdates();
        assertEquals(updates.size(), 2);
        assertEquals(updates.toString(), "{codenvy=1.0.0, " + InstallManagerArtifact.NAME + "=2.0.0}");
    }

    @Test
    public void testGetUpdatesToDownload() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");
        final Version version200 = Version.valueOf("2.0.0");

        doReturn(new TreeMap<Artifact, Version>() {{
            put(cdecArtifact, version100);
            put(installManagerArtifact, version200);
        }}).when(downloadManager).getUpdates();

        Map<Artifact, Version> artifactsToDownload = downloadManager.getUpdatesToDownload(null, null);
        assertEquals(artifactsToDownload.size(), 2);
        assertEquals(artifactsToDownload.toString(), "{" + InstallManagerArtifact.NAME + "=2.0.0, codenvy=1.0.0}");
    }

    @Test
    public void testGetUpdatesToDownloadForSpecificArtifact() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");

        doReturn(version100).when(cdecArtifact).getLatestInstallableVersion();

        Map<Artifact, Version> artifactsToDownload = downloadManager.getUpdatesToDownload(cdecArtifact, null);
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
        }}).when(downloadManager).getUpdates();

        doReturn(new TreeMap<Version, Path>() {{
            put(version200, null);
        }}).when(cdecArtifact).getDownloadedVersions(any(Path.class));

        doReturn(new TreeMap<Version, Path>() {{
            put(version100, null);
        }}).when(installManagerArtifact).getDownloadedVersions(any(Path.class));

        Map<Artifact, Version> artifactsToDownload = downloadManager.getUpdatesToDownload(null, null);
        assertEquals(artifactsToDownload.size(), 0);
    }


    @Test
    public void testDownloadVersion() throws Exception {
        Version version100 = Version.valueOf("1.0.0");
        when(transport.doGet("api/endpoint/account", saasUserCredentials.getToken()))
                .thenReturn("[{"
                            + "\"roles\":[\"" + SaasAccountServiceProxy.ACCOUNT_OWNER_ROLE + "\"],"
                            + "\"accountReference\":{\"id\":\"" + saasUserCredentials.getAccountId() + "\"}"
                            + "}]");

        when(transport.doGet("api/endpoint/account/" + saasUserCredentials.getAccountId() + "/subscriptions", saasUserCredentials.getToken()))
                .thenReturn("[{serviceId:\"OnPremises\"}]");

        when(transport.doGet(endsWith("repository/properties/" + cdecArtifact.getName() + "/" + version100.toString())))
                .thenReturn(String.format("{\"%s\": \"true\", \"%s\":\"OnPremises\"}", AUTHENTICATION_REQUIRED_PROPERTY, SUBSCRIPTION_PROPERTY));

        downloadManager.download(cdecArtifact, version100);
    }
}