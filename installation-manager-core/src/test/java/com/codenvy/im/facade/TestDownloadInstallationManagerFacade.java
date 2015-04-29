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

import com.codenvy.im.InstallationManager;
import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.exceptions.ArtifactNotFoundException;
import com.codenvy.im.exceptions.AuthenticationException;
import com.codenvy.im.request.Request;
import com.codenvy.im.response.DownloadStatusInfo;
import com.codenvy.im.response.Response;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;

import org.apache.commons.io.FileUtils;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
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

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;
import static java.lang.Thread.sleep;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 */
public class TestDownloadInstallationManagerFacade {
    private static final String TEST_TOKEN = "auth token";

    private InstallationManagerFacade installationManagerService;

    private InstallationManager mockInstallationManager;
    private HttpTransport       mockTransport;

    private Artifact        installManagerArtifact;
    private Artifact        cdecArtifact;
    private UserCredentials testCredentials;
    private Path            pathCDEC;
    private Path            pathIM;

    @BeforeMethod
    public void init() throws Exception {
        mockInstallationManager = mock(InstallationManager.class);
        mockTransport = mock(HttpTransport.class);
        installationManagerService = new InstallationManagerFacade("update/endpoint", "api/endpoint", mockInstallationManager, mockTransport);
        MockitoAnnotations.initMocks(this);

        this.pathCDEC = Paths.get("./target/cdec.zip");
        this.pathIM = Paths.get("./target/im.zip");

        testCredentials = new UserCredentials(TEST_TOKEN, "accountId");

        installManagerArtifact = createArtifact(InstallManagerArtifact.NAME);
        cdecArtifact = createArtifact(CDECArtifact.NAME);
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
        }).when(mockInstallationManager).getUpdatesToDownload(null, null);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                FileUtils.writeByteArrayToFile(pathCDEC.toFile(), new byte[100]);
                return pathCDEC;
            }
        }).when(mockInstallationManager).download(cdecArtifact, version200);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                FileUtils.writeByteArrayToFile(pathIM.toFile(), new byte[50]);
                return pathIM;
            }
        }).when(mockInstallationManager).download(installManagerArtifact, version100);

        doReturn(pathCDEC).when(mockInstallationManager).getPathToBinaries(cdecArtifact, version200);
        doReturn(pathIM).when(mockInstallationManager).getPathToBinaries(installManagerArtifact, version100);

        doReturn(100L).when(mockInstallationManager).getBinariesSize(cdecArtifact, version200);
        doReturn(50L).when(mockInstallationManager).getBinariesSize(installManagerArtifact, version100);

        Request request = new Request().setUserCredentials(testCredentials);
        installationManagerService.startDownload(request);

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status

            String response = installationManagerService.getDownloadStatus();
            info = Response.fromJson(response).getDownloadInfo();
        } while (info.getDownloadResult() == null);

        assertEquals(info.getDownloadResult().toJson(), "{\n" +
                                                        "  \"artifacts\" : [ {\n" +
                                                        "    \"artifact\" : \"codenvy\",\n" +
                                                        "    \"version\" : \"2.0.0\",\n" +
                                                        "    \"file\" : \"./target/cdec.zip\",\n" +
                                                        "    \"status\" : \"SUCCESS\"\n" +
                                                        "  }, {\n" +
                                                        "    \"artifact\" : \"" + InstallManagerArtifact.NAME + "\",\n" +
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
        }).when(mockInstallationManager).getUpdatesToDownload(cdecArtifact, null);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                FileUtils.writeByteArrayToFile(pathCDEC.toFile(), new byte[100]);
                return pathCDEC;
            }
        }).when(mockInstallationManager).download(cdecArtifact, version200);

        doReturn(pathCDEC).when(mockInstallationManager).getPathToBinaries(cdecArtifact, version200);
        doReturn(100L).when(mockInstallationManager).getBinariesSize(cdecArtifact, version200);

        Request request = new Request().setUserCredentials(testCredentials).setArtifactName(CDECArtifact.NAME);
        installationManagerService.startDownload(request);

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status

            String response = installationManagerService.getDownloadStatus();
            info = Response.fromJson(response).getDownloadInfo();
        } while (info.getDownloadResult() == null);

        assertEquals(info.getDownloadResult().toJson(), "{\n" +
                                                        "  \"artifacts\" : [ {\n" +
                                                        "    \"artifact\" : \"codenvy\",\n" +
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
        }).when(mockInstallationManager).getUpdatesToDownload(cdecArtifact, version200);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                FileUtils.writeByteArrayToFile(pathCDEC.toFile(), new byte[100]);
                return pathCDEC;
            }
        }).when(mockInstallationManager).download(cdecArtifact, version200);

        doReturn(pathCDEC).when(mockInstallationManager).getPathToBinaries(cdecArtifact, version200);
        doReturn(100L).when(mockInstallationManager).getBinariesSize(cdecArtifact, version200);

        Request request = new Request().setUserCredentials(testCredentials).setArtifactName(CDECArtifact.NAME).setVersion("2.0.0");
        installationManagerService.startDownload(request);

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status

            String response = installationManagerService.getDownloadStatus();
            info = Response.fromJson(response).getDownloadInfo();
        } while (info.getDownloadResult() == null);

        assertEquals(info.getDownloadResult().toJson(), "{\n" +
                                                        "  \"artifacts\" : [ {\n" +
                                                        "    \"artifact\" : \"codenvy\",\n" +
                                                        "    \"version\" : \"2.0.0\",\n" +
                                                        "    \"file\" : \"./target/cdec.zip\",\n" +
                                                        "    \"status\" : \"SUCCESS\"\n" +
                                                        "  } ],\n" +
                                                        "  \"status\" : \"OK\"\n" +
                                                        "}");
    }

    @Test
    public void testDownloadErrorIfUpdatesAbsent() throws Exception {
        doReturn(Collections.emptyMap()).when(mockInstallationManager).getUpdatesToDownload(null, null);

        Request request = new Request().setUserCredentials(testCredentials);
        installationManagerService.startDownload(request);

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status

            String response = installationManagerService.getDownloadStatus();
            info = Response.fromJson(response).getDownloadInfo();
        } while (info.getDownloadResult() == null);

        assertEquals(info.getDownloadResult().toJson(), "{\n" +
                                                        "  \"artifacts\" : [ ],\n" +
                                                        "  \"status\" : \"OK\"\n" +
                                                        "}");

        /* -------------- */
        doReturn(Collections.emptyMap()).when(mockInstallationManager).getUpdatesToDownload(cdecArtifact, null);

        request.setArtifactName(CDECArtifact.NAME);
        installationManagerService.startDownload(request);

        do {
            sleep(100); // due to async request, wait a bit to get proper download status

            String response = installationManagerService.getDownloadStatus();
            info = Response.fromJson(response).getDownloadInfo();
        } while (info.getDownloadResult() == null);

        assertEquals(info.getDownloadResult().toJson(), "{\n" +
                                                        "  \"artifacts\" : [ ],\n" +
                                                        "  \"status\" : \"OK\"\n" +
                                                        "}");

        /* -------------- */
        request.setArtifactName("unknown");
        installationManagerService.startDownload(request);

        do {
            sleep(100); // due to async request, wait a bit to get proper download status

            String response = installationManagerService.getDownloadStatus();
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
                                                                        .getUpdatesToDownload(cdecArtifact, version200
                                                                        );

        doThrow(new ArtifactNotFoundException(cdecArtifact, version200))
                .when(mockInstallationManager)
                .getPathToBinaries(cdecArtifact, version200);

        doThrow(new ArtifactNotFoundException(cdecArtifact, version200))
                .when(mockInstallationManager)
                .getBinariesSize(cdecArtifact, version200);

        doThrow(new ArtifactNotFoundException(cdecArtifact, version200))
                .when(mockInstallationManager)
                .download(cdecArtifact, version200);

        Request request = new Request().setUserCredentials(testCredentials).setArtifactName(CDECArtifact.NAME).setVersion("2.0.0");
        installationManagerService.startDownload(request);

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status

            String response = installationManagerService.getDownloadStatus();
            info = Response.fromJson(response).getDownloadInfo();
        } while (info.getDownloadResult() == null);

        assertEquals(info.getDownloadResult().toJson(), "{\n" +
                                                        "  \"message\" : \"Artifact codenvy:2.0.0 not found\",\n" +
                                                        "  \"status\" : \"ERROR\"\n" +
                                                        "}");
    }

    @Test
    public void testDownloadErrorIfAuthenticationFailedOnGetUpdates() throws Exception {
        when(mockInstallationManager.getUpdatesToDownload(null, null)).thenThrow(new AuthenticationException());

        Request request = new Request().setUserCredentials(testCredentials);
        installationManagerService.startDownload(request);

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status

            String response = installationManagerService.getDownloadStatus();
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
        }).when(mockInstallationManager).getUpdatesToDownload(cdecArtifact, version200);

        doThrow(new AuthenticationException()).when(mockInstallationManager).getPathToBinaries(cdecArtifact, version200);
        doThrow(new AuthenticationException()).when(mockInstallationManager).getBinariesSize(cdecArtifact, version200);
        doThrow(new AuthenticationException()).when(mockInstallationManager).download(cdecArtifact, version200);

        Request request = new Request().setUserCredentials(testCredentials).setArtifactName(CDECArtifact.NAME).setVersion("2.0.0");
        installationManagerService.startDownload(request);

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status

            String response = installationManagerService.getDownloadStatus();
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
        when(mockInstallationManager.getUpdatesToDownload(cdecArtifact, null))
                .thenThrow(new IllegalStateException("Valid subscription is required to download cdec"));


        Request request = new Request().setUserCredentials(testCredentials).setArtifactName(CDECArtifact.NAME);
        installationManagerService.startDownload(request);

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status

            String response = installationManagerService.getDownloadStatus();
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

        doReturn(false).when(mockInstallationManager).isInstallable(cdecArtifact, version100);
        doReturn(true).when(mockInstallationManager).isInstallable(cdecArtifact, version101);
        doReturn(true).when(mockInstallationManager).isInstallable(installManagerArtifact, version200);

        doReturn(new LinkedHashMap<Artifact, SortedMap<Version, Path>>() {{
            put(cdecArtifact, new TreeMap<Version, Path>() {{
                put(version100, Paths.get("target/file1"));
                put(Version.valueOf("1.0.1"), Paths.get("target/file2"));
            }});
            put(installManagerArtifact, new TreeMap<Version, Path>() {{
                put(Version.valueOf("2.0.0"), Paths.get("target/file3"));
            }});
        }}).when(mockInstallationManager).getDownloadedArtifacts();

        Request request = new Request().setUserCredentials(testCredentials);
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
        }}).when(mockInstallationManager).getDownloadedVersions(installManagerArtifact);

        doReturn(false).when(mockInstallationManager).isInstallable(installManagerArtifact, version200);
        doReturn(true).when(mockInstallationManager).isInstallable(installManagerArtifact, version201);

        Request request = new Request().setUserCredentials(testCredentials).setArtifactName(InstallManagerArtifact.NAME);
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
        }}).when(mockInstallationManager).getDownloadedVersions(cdecArtifact);

        doReturn(true).when(mockInstallationManager).isInstallable(cdecArtifact, version200);

        Request request = new Request().setUserCredentials(testCredentials).setArtifactName(CDECArtifact.NAME).setVersion("2.0.0");
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
        doReturn(new TreeMap<>()).when(mockInstallationManager).getDownloadedVersions(installManagerArtifact);

        Request request = new Request().setUserCredentials(testCredentials).setArtifactName(InstallManagerArtifact.NAME);
        String response = installationManagerService.getDownloads(request);
        assertEquals(response, "{\n" +
                               "  \"artifacts\" : [ ],\n" +
                               "  \"status\" : \"OK\"\n" +
                               "}");
    }
}
