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
import com.codenvy.im.artifacts.ArtifactFactory;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.exceptions.ArtifactNotFoundException;
import com.codenvy.im.exceptions.AuthenticationException;
import com.codenvy.im.request.Request;
import com.codenvy.im.response.DownloadStatusInfo;
import com.codenvy.im.response.Response;
import com.codenvy.im.restlet.InstallationManager;
import com.codenvy.im.restlet.InstallationManagerService;
import com.codenvy.im.user.UserCredentials;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import org.apache.commons.io.FileUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.lang.Thread.sleep;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 */
public class TestDownloadInstallationManagerServiceImpl {
    private static final String TEST_TOKEN = "auth token";

    private InstallationManagerService installationManagerService;

    @Mock
    private InstallationManager                    mockInstallationManager;

    @Mock
    private HttpTransport mockTransport;

    private Artifact                               installManagerArtifact;
    private Artifact                               cdecArtifact;
    private UserCredentials                        testCredentials;
    private Path                                   pathCDEC;
    private Path                                   pathIM;
    private JacksonRepresentation<UserCredentials> userCredentialsRep;

    @BeforeMethod
    public void init() {
        MockitoAnnotations.initMocks(this);

        installationManagerService =
            new InstallationManagerServiceImpl(mockInstallationManager, mockTransport, new DownloadDescriptorHolder());

        this.pathCDEC = Paths.get("./target/cdec.zip");
        this.pathIM = Paths.get("./target/im.zip");

        testCredentials = new UserCredentials(TEST_TOKEN, "accountId");
        userCredentialsRep = new JacksonRepresentation<>(testCredentials);

        installManagerArtifact = ArtifactFactory.createArtifact(InstallManagerArtifact.NAME);
        cdecArtifact = ArtifactFactory.createArtifact(CDECArtifact.NAME);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        Files.deleteIfExists(pathCDEC);
        Files.deleteIfExists(pathIM);
    }

    @Test
    public void testDownload() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");
        final Version version200 = Version.valueOf("2.0.0");

        doReturn(new LinkedHashMap<Artifact, Version>() {
            {
                put(cdecArtifact, version200);
                put(installManagerArtifact, version100);
            }
        }).when(mockInstallationManager).getUpdatesToDownload(null, null, testCredentials.getToken());

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                FileUtils.writeByteArrayToFile(pathCDEC.toFile(), new byte[100]);
                return pathCDEC;
            }
        }).when(mockInstallationManager).download(testCredentials, cdecArtifact, version200);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                FileUtils.writeByteArrayToFile(pathIM.toFile(), new byte[50]);
                return pathIM;
            }
        }).when(mockInstallationManager).download(testCredentials, installManagerArtifact, version100);

        doReturn(pathCDEC).when(mockInstallationManager).getPathToBinaries(cdecArtifact, version200);
        doReturn(pathIM).when(mockInstallationManager).getPathToBinaries(installManagerArtifact, version100);

        doReturn(100L).when(mockInstallationManager).getBinariesSize(cdecArtifact, version200);
        doReturn(50L).when(mockInstallationManager).getBinariesSize(installManagerArtifact, version100);

        installationManagerService.startDownload("id1", userCredentialsRep);

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status

            String response = installationManagerService.downloadStatus("id1");
            info = Response.fromJson(response).getDownloadInfo();
        } while (info.getDownloadResult() == null);

        assertEquals(info.getDownloadResult().toJson(), "{\n" +
                                                        "  \"artifacts\" : [ {\n" +
                                                        "    \"artifact\" : \"cdec\",\n" +
                                                        "    \"version\" : \"2.0.0\",\n" +
                                                        "    \"file\" : \"./target/cdec.zip\",\n" +
                                                        "    \"status\" : \"SUCCESS\"\n" +
                                                        "  }, {\n" +
                                                        "    \"artifact\" : \"installation-manager\",\n" +
                                                        "    \"version\" : \"1.0.0\",\n" +
                                                        "    \"file\" : \"./target/im.zip\",\n" +
                                                        "    \"status\" : \"SUCCESS\"\n" +
                                                        "  } ],\n" +
                                                        "  \"status\" : \"OK\"\n" +
                                                        "}");
    }

    @Test
    public void testDownloadSpecificArtifact() throws Exception {
        final Version version200 = Version.valueOf("2.0.0");

        doReturn(new LinkedHashMap<Artifact, Version>() {
            {
                put(cdecArtifact, version200);
            }
        }).when(mockInstallationManager).getUpdatesToDownload(cdecArtifact, null, testCredentials.getToken());

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                FileUtils.writeByteArrayToFile(pathCDEC.toFile(), new byte[100]);
                return pathCDEC;
            }
        }).when(mockInstallationManager).download(testCredentials, cdecArtifact, version200);

        doReturn(pathCDEC).when(mockInstallationManager).getPathToBinaries(cdecArtifact, version200);
        doReturn(100L).when(mockInstallationManager).getBinariesSize(cdecArtifact, version200);

        installationManagerService.startDownload(CDECArtifact.NAME, "id2", userCredentialsRep);

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status

            String response = installationManagerService.downloadStatus("id2");
            info = Response.fromJson(response).getDownloadInfo();
        } while (info.getDownloadResult() == null);

        assertEquals(info.getDownloadResult().toJson(), "{\n" +
                                                        "  \"artifacts\" : [ {\n" +
                                                        "    \"artifact\" : \"cdec\",\n" +
                                                        "    \"version\" : \"2.0.0\",\n" +
                                                        "    \"file\" : \"./target/cdec.zip\",\n" +
                                                        "    \"status\" : \"SUCCESS\"\n" +
                                                        "  } ],\n" +
                                                        "  \"status\" : \"OK\"\n" +
                                                        "}");
    }

    @Test
    public void testDownloadSpecificVersionArtifact() throws Exception {
        final Version version200 = Version.valueOf("2.0.0");
        doReturn(new LinkedHashMap<Artifact, Version>() {
            {
                put(cdecArtifact, version200);
            }
        }).when(mockInstallationManager).getUpdatesToDownload(cdecArtifact, version200, testCredentials.getToken());

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                FileUtils.writeByteArrayToFile(pathCDEC.toFile(), new byte[100]);
                return pathCDEC;
            }
        }).when(mockInstallationManager).download(testCredentials, cdecArtifact, version200);

        doReturn(pathCDEC).when(mockInstallationManager).getPathToBinaries(cdecArtifact, version200);
        doReturn(100L).when(mockInstallationManager).getBinariesSize(cdecArtifact, version200);

        installationManagerService.startDownload(CDECArtifact.NAME, version200.toString(), "id3", userCredentialsRep);

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status

            String response = installationManagerService.downloadStatus("id3");
            info = Response.fromJson(response).getDownloadInfo();
        } while (info.getDownloadResult() == null);

        assertEquals(info.getDownloadResult().toJson(), "{\n" +
                                                        "  \"artifacts\" : [ {\n" +
                                                        "    \"artifact\" : \"cdec\",\n" +
                                                        "    \"version\" : \"2.0.0\",\n" +
                                                        "    \"file\" : \"./target/cdec.zip\",\n" +
                                                        "    \"status\" : \"SUCCESS\"\n" +
                                                        "  } ],\n" +
                                                        "  \"status\" : \"OK\"\n" +
                                                        "}");
    }

    @Test
    public void testDownloadErrorIfUpdatesAbsent() throws Exception {
        doReturn(Collections.emptyMap()).when(mockInstallationManager).getUpdatesToDownload(null, null, testCredentials.getToken());

        installationManagerService.startDownload("id4", userCredentialsRep);

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status

            String response = installationManagerService.downloadStatus("id4");
            info = Response.fromJson(response).getDownloadInfo();
        } while (info.getDownloadResult() == null);

        assertEquals(info.getDownloadResult().toJson(), "{\n" +
                                                        "  \"artifacts\" : [ ],\n" +
                                                        "  \"status\" : \"OK\"\n" +
                                                        "}");

        /* -------------- */
        doReturn(Collections.emptyMap()).when(mockInstallationManager).getUpdatesToDownload(cdecArtifact, null, testCredentials.getToken());
        installationManagerService.startDownload(CDECArtifact.NAME, "id5", userCredentialsRep);

        do {
            sleep(100); // due to async request, wait a bit to get proper download status

            String response = installationManagerService.downloadStatus("id5");
            info = Response.fromJson(response).getDownloadInfo();
        } while (info.getDownloadResult() == null);

        assertEquals(info.getDownloadResult().toJson(), "{\n" +
                                                        "  \"artifacts\" : [ ],\n" +
                                                        "  \"status\" : \"OK\"\n" +
                                                        "}");

        /* -------------- */
        installationManagerService.startDownload("unknown", "id6", userCredentialsRep);

        do {
            sleep(100); // due to async request, wait a bit to get proper download status

            String response = installationManagerService.downloadStatus("id6");
            info = Response.fromJson(response).getDownloadInfo();
        } while (info.getDownloadResult() == null);

        assertEquals(info.getDownloadResult().toJson(), "{\n" +
                                                        "  \"message\" : \"Artifact 'unknown' not found\",\n" +
                                                        "  \"status\" : \"ERROR\"\n" +
                                                        "}");
    }

    @Test
    public void testDownloadErrorIfSpecificVersionArtifactAbsent() throws Exception {
        Version version200 = Version.valueOf("2.0.0");
        doThrow(new ArtifactNotFoundException(cdecArtifact, version200)).when(mockInstallationManager)
            .getUpdatesToDownload(cdecArtifact, version200, testCredentials.getToken());

        doThrow(new ArtifactNotFoundException(cdecArtifact, version200))
            .when(mockInstallationManager)
            .getPathToBinaries(cdecArtifact, version200);

        doThrow(new ArtifactNotFoundException(cdecArtifact, version200))
            .when(mockInstallationManager)
            .getBinariesSize(cdecArtifact, version200);

        doThrow(new ArtifactNotFoundException(cdecArtifact, version200))
            .when(mockInstallationManager)
            .download(testCredentials, cdecArtifact, version200);

        installationManagerService.startDownload(CDECArtifact.NAME, version200.toString(), "id7", userCredentialsRep);

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status

            String response = installationManagerService.downloadStatus("id7");
            info = Response.fromJson(response).getDownloadInfo();
        } while (info.getDownloadResult() == null);

        assertEquals(info.getDownloadResult().toJson(), "{\n" +
                                                        "  \"message\" : \"Artifact 'cdec' version '2.0.0' not found\",\n" +
                                                        "  \"status\" : \"ERROR\"\n" +
                                                        "}");
    }

    @Test
    public void testDownloadErrorIfAuthenticationFailedOnGetUpdates() throws Exception {
        when(mockInstallationManager.getUpdatesToDownload(null, null, testCredentials.getToken())).thenThrow(new AuthenticationException());

        installationManagerService.startDownload("id8", userCredentialsRep);

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status

            String response = installationManagerService.downloadStatus("id8");
            info = Response.fromJson(response).getDownloadInfo();
        } while (info.getDownloadResult() == null);

        assertEquals(info.getDownloadResult().toJson(), "{\n" +
                                                        "  \"message\" : \"Authentication error. Authentication token " +
                                                        "might be expired or invalid.\",\n" +
                                                        "  \"status\" : \"ERROR\"\n" +
                                                        "}");

    }

    @Test
    public void testDownloadErrorIfAuthenticationFailedOnDownload() throws Exception {
        final Version version200 = Version.valueOf("2.0.0");
        doReturn(new LinkedHashMap<Artifact, Version>() {
            {
                put(cdecArtifact, version200);
            }
        }).when(mockInstallationManager).getUpdatesToDownload(cdecArtifact, version200, testCredentials.getToken());

        doThrow(new AuthenticationException()).when(mockInstallationManager).getPathToBinaries(cdecArtifact, version200);
        doThrow(new AuthenticationException()).when(mockInstallationManager).getBinariesSize(cdecArtifact, version200);
        doThrow(new AuthenticationException()).when(mockInstallationManager).download(testCredentials, cdecArtifact, version200);

        installationManagerService.startDownload(CDECArtifact.NAME, version200.toString(), "id9", userCredentialsRep);

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status

            String response = installationManagerService.downloadStatus("id9");
            info = Response.fromJson(response).getDownloadInfo();
        } while (info.getDownloadResult() == null);

        assertEquals(info.getDownloadResult().toJson(), "{\n" +
                                                        "  \"message\" : \"Authentication error. Authentication token might be expired" +
                                                        " or invalid.\",\n" +
                                                        "  \"status\" : \"ERROR\"\n" +
                                                        "}");
    }

    @Test
    public void testDownloadErrorIfSubscriptionVerificationFailed() throws Exception {
        when(mockInstallationManager.getUpdatesToDownload(cdecArtifact, null, testCredentials.getToken()))
            .thenThrow(new IllegalStateException("Valid subscription is required to download cdec"));

        installationManagerService.startDownload(CDECArtifact.NAME, "id10", userCredentialsRep);

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status

            String response = installationManagerService.downloadStatus("id10");
            info = Response.fromJson(response).getDownloadInfo();
        } while (info.getDownloadResult() == null);

        assertEquals(info.getDownloadResult().toJson(), "{\n" +
                                                        "  \"message\" : \"Valid subscription is required to download " +
                                                        "cdec\",\n" +
                                                        "  \"status\" : \"ERROR\"\n" +
                                                        "}");
    }

    @Test
    public void testGetDownloads() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");
        final Version version101 = Version.valueOf("1.0.1");
        final Version version200 = Version.valueOf("2.0.0");

        doReturn(false).when(mockInstallationManager).isInstallable(cdecArtifact, version100, TEST_TOKEN);
        doReturn(true).when(mockInstallationManager).isInstallable(cdecArtifact, version101, TEST_TOKEN);
        doReturn(true).when(mockInstallationManager).isInstallable(installManagerArtifact, version200, TEST_TOKEN);

        doReturn(new LinkedHashMap<Artifact, SortedMap<Version, Path>>() {{
            put(cdecArtifact, new TreeMap<Version, Path>() {{
                put(version100, Paths.get("target/file1"));
                put(Version.valueOf("1.0.1"), Paths.get("target/file2"));
            }});
            put(installManagerArtifact, new TreeMap<Version, Path>() {{
                put(Version.valueOf("2.0.0"), Paths.get("target/file3"));
            }});
        }}).when(mockInstallationManager).getDownloadedArtifacts();

        JacksonRepresentation<Request> requestRep = new Request()
            .setUserCredentials(testCredentials)
            .toRepresentation();

        String response = installationManagerService.getDownloads(requestRep);
        assertEquals(response, "{\n" +
                               "  \"artifacts\" : [ {\n" +
                               "    \"artifact\" : \"cdec\",\n" +
                               "    \"version\" : \"1.0.0\",\n" +
                               "    \"file\" : \"target/file1\",\n" +
                               "    \"status\" : \"DOWNLOADED\"\n" +
                               "  }, {\n" +
                               "    \"artifact\" : \"cdec\",\n" +
                               "    \"version\" : \"1.0.1\",\n" +
                               "    \"file\" : \"target/file2\",\n" +
                               "    \"status\" : \"READY_TO_INSTALL\"\n" +
                               "  }, {\n" +
                               "    \"artifact\" : \"installation-manager\",\n" +
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
        }}).when(mockInstallationManager).getDownloadedVersions(installManagerArtifact);

        doReturn(false).when(mockInstallationManager).isInstallable(installManagerArtifact, version200, testCredentials.getToken());
        doReturn(true).when(mockInstallationManager).isInstallable(installManagerArtifact, version201, testCredentials.getToken());

        JacksonRepresentation<Request> requestRep = new Request()
            .setArtifactName(installManagerArtifact.getName())
            .setUserCredentials(testCredentials)
            .toRepresentation();

        String response = installationManagerService.getDownloads(requestRep);
        assertEquals(response, "{\n" +
                               "  \"artifacts\" : [ {\n" +
                               "    \"artifact\" : \"installation-manager\",\n" +
                               "    \"version\" : \"2.0.0\",\n" +
                               "    \"file\" : \"target/file1\",\n" +
                               "    \"status\" : \"DOWNLOADED\"\n" +
                               "  }, {\n" +
                               "    \"artifact\" : \"installation-manager\",\n" +
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
        }}).when(mockInstallationManager).getDownloadedVersions(cdecArtifact);

        doReturn(true).when(mockInstallationManager).isInstallable(cdecArtifact, version200, testCredentials.getToken());

        JacksonRepresentation<Request> requestRep = new Request()
            .setArtifactName(cdecArtifact.getName())
            .setVersion("2.0.0")
            .setUserCredentials(testCredentials)
            .toRepresentation();

        String response = installationManagerService.getDownloads(requestRep);
        assertEquals(response, "{\n" +
                               "  \"artifacts\" : [ {\n" +
                               "    \"artifact\" : \"cdec\",\n" +
                               "    \"version\" : \"2.0.0\",\n" +
                               "    \"file\" : \"target/file1\",\n" +
                               "    \"status\" : \"READY_TO_INSTALL\"\n" +
                               "  } ],\n" +
                               "  \"status\" : \"OK\"\n" +
                               "}");
    }

    @Test
    public void testGetDownloadsSpecificArtifactShouldReturnEmptyList() throws Exception {
        doReturn(new LinkedHashMap<Artifact, SortedMap<Version, Path>>() {{
            put(cdecArtifact, new TreeMap<Version, Path>() {{
                put(Version.valueOf("1.0.0"), Paths.get("target/file1"));
                put(Version.valueOf("1.0.1"), Paths.get("target/file2"));
            }});
        }}).when(mockInstallationManager).getDownloadedArtifacts();

        JacksonRepresentation<Request> requestRep = new Request()
            .setArtifactName(installManagerArtifact.getName())
            .setUserCredentials(testCredentials)
            .toRepresentation();

        String response = installationManagerService.getDownloads(requestRep);
        assertEquals(response, "{\n" +
                               "  \"artifacts\" : [ ],\n" +
                               "  \"status\" : \"OK\"\n" +
                               "}");
    }
}
