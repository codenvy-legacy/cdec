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
package com.codenvy.im.facade;

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.managers.BackupManager;
import com.codenvy.im.managers.DownloadManager;
import com.codenvy.im.managers.InstallManager;
import com.codenvy.im.managers.NodeManager;
import com.codenvy.im.managers.PasswordManager;
import com.codenvy.im.managers.StorageManager;
import com.codenvy.im.request.Request;
import com.codenvy.im.saas.SaasAccountServiceProxy;
import com.codenvy.im.saas.SaasAuthServiceProxy;
import com.codenvy.im.saas.SaasUserCredentials;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;

import org.mockito.Mock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 */
public class TestDownloadInstallationManagerFacade {
    private static final String TEST_TOKEN = "auth token";

    private InstallationManagerFacade installationManagerService;

    @Mock
    private SaasAuthServiceProxy    saasAuthServiceProxy;
    @Mock
    private SaasAccountServiceProxy saasAccountServiceProxy;
    @Mock
    private HttpTransport           transport;
    @Mock
    private PasswordManager         passwordManager;
    @Mock
    private NodeManager             nodeManager;
    @Mock
    private BackupManager           backupManager;
    @Mock
    private StorageManager          storageManager;
    @Mock
    private InstallManager          installManager;
    @Mock
    private DownloadManager         downloadManager;

    private Artifact            installManagerArtifact;
    private Artifact            cdecArtifact;
    private SaasUserCredentials testCredentials;
    private Path                pathCDEC;
    private Path                pathIM;

    @BeforeMethod
    public void init() throws Exception {
        initMocks(this);
        installationManagerService = new InstallationManagerFacade("target/download",
                                                                   "update/endpoint",
                                                                   transport,
                                                                   saasAuthServiceProxy,
                                                                   saasAccountServiceProxy,
                                                                   passwordManager,
                                                                   nodeManager,
                                                                   backupManager,
                                                                   storageManager,
                                                                   installManager,
                                                                   downloadManager);
        this.pathCDEC = Paths.get("./target/cdec.zip");
        this.pathIM = Paths.get("./target/im.zip");

        testCredentials = new SaasUserCredentials(TEST_TOKEN, "accountId");

        installManagerArtifact = createArtifact(InstallManagerArtifact.NAME);
        cdecArtifact = createArtifact(CDECArtifact.NAME);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        Files.deleteIfExists(pathCDEC);
        Files.deleteIfExists(pathIM);
    }


    @Test
    public void testGetDownloads() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");
        final Version version101 = Version.valueOf("1.0.1");
        final Version version200 = Version.valueOf("2.0.0");

        doReturn(false).when(installManager).isInstallable(cdecArtifact, version100);
        doReturn(true).when(installManager).isInstallable(cdecArtifact, version101);
        doReturn(true).when(installManager).isInstallable(installManagerArtifact, version200);

        doReturn(new LinkedHashMap<Artifact, SortedMap<Version, Path>>() {{
            put(cdecArtifact, new TreeMap<Version, Path>() {{
                put(version100, Paths.get("target/file1"));
                put(Version.valueOf("1.0.1"), Paths.get("target/file2"));
            }});
            put(installManagerArtifact, new TreeMap<Version, Path>() {{
                put(Version.valueOf("2.0.0"), Paths.get("target/file3"));
            }});
        }}).when(downloadManager).getDownloadedArtifacts();

        Request request = new Request().setSaasUserCredentials(testCredentials);
        String response = installationManagerService.getDownloads(request);
        assertEquals(response, "{\n" +
                               "  \"artifacts\" : [ {\n" +
                               "    \"artifact\" : \"codenvy\",\n" +
                               "    \"version\" : \"1.0.0\",\n" +
                               "    \"file\" : \"target/file1\",\n" +
                               "    \"status\" : \"DOWNLOADED\"\n" +
                               "  }, {\n" +
                               "    \"artifact\" : \"codenvy\",\n" +
                               "    \"version\" : \"1.0.1\",\n" +
                               "    \"file\" : \"target/file2\",\n" +
                               "    \"status\" : \"READY_TO_INSTALL\"\n" +
                               "  }, {\n" +
                               "    \"artifact\" : \"" + InstallManagerArtifact.NAME + "\",\n" +
                               "    \"version\" : \"2.0.0\",\n" +
                               "    \"file\" : \"target/file3\",\n" +
                               "    \"status\" : \"READY_TO_INSTALL\"\n" +
                               "  } ],\n" +
                               "  \"status\" : \"OK\"\n" +
                               "}");
    }

    @Test
    public void testGetDownloadsSpecificArtifact() throws Exception {
        final Version version200 = Version.valueOf("2.0.0");
        final Version version201 = Version.valueOf("2.0.1");

        doReturn(new TreeMap<Version, Path>() {{
            put(version200, Paths.get("target/file1"));
            put(version201, Paths.get("target/file2"));
        }}).when(downloadManager).getDownloadedVersions(installManagerArtifact);

        doReturn(false).when(installManager).isInstallable(installManagerArtifact, version200);
        doReturn(true).when(installManager).isInstallable(installManagerArtifact, version201);

        Request request = new Request().setSaasUserCredentials(testCredentials).setArtifactName(InstallManagerArtifact.NAME);
        String response = installationManagerService.getDownloads(request);
        assertEquals(response, "{\n" +
                               "  \"artifacts\" : [ {\n" +
                               "    \"artifact\" : \"" + InstallManagerArtifact.NAME + "\",\n" +
                               "    \"version\" : \"2.0.0\",\n" +
                               "    \"file\" : \"target/file1\",\n" +
                               "    \"status\" : \"DOWNLOADED\"\n" +
                               "  }, {\n" +
                               "    \"artifact\" : \"" + InstallManagerArtifact.NAME + "\",\n" +
                               "    \"version\" : \"2.0.1\",\n" +
                               "    \"file\" : \"target/file2\",\n" +
                               "    \"status\" : \"READY_TO_INSTALL\"\n" +
                               "  } ],\n" +
                               "  \"status\" : \"OK\"\n" +
                               "}");
    }

    @Test
    public void testGetDownloadsOlderVersionInstallManagerArtifact() throws Exception {
        final Version version200 = Version.valueOf("2.0.0");
        doReturn(new TreeMap<Version, Path>() {{
            put(version200, Paths.get("target/file1"));
        }}).when(downloadManager).getDownloadedVersions(cdecArtifact);

        doReturn(true).when(installManager).isInstallable(cdecArtifact, version200);

        Request request = new Request().setSaasUserCredentials(testCredentials).setArtifactName(CDECArtifact.NAME).setVersion("2.0.0");
        String response = installationManagerService.getDownloads(request);
        assertEquals(response, "{\n" +
                               "  \"artifacts\" : [ {\n" +
                               "    \"artifact\" : \"codenvy\",\n" +
                               "    \"version\" : \"2.0.0\",\n" +
                               "    \"file\" : \"target/file1\",\n" +
                               "    \"status\" : \"READY_TO_INSTALL\"\n" +
                               "  } ],\n" +
                               "  \"status\" : \"OK\"\n" +
                               "}");
    }

    @Test
    public void testGetDownloadsSpecificArtifactShouldReturnEmptyList() throws Exception {
        doReturn(new TreeMap<>()).when(downloadManager).getDownloadedVersions(installManagerArtifact);

        Request request = new Request().setSaasUserCredentials(testCredentials).setArtifactName(InstallManagerArtifact.NAME);
        String response = installationManagerService.getDownloads(request);
        assertEquals(response, "{\n" +
                               "  \"artifacts\" : [ ],\n" +
                               "  \"status\" : \"OK\"\n" +
                               "}");
    }
}
