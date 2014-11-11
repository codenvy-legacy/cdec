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
import com.codenvy.im.response.DownloadStatusInfo;
import com.codenvy.im.response.ResponseCode;
import com.codenvy.im.restlet.InstallationManager;
import com.codenvy.im.restlet.InstallationManagerService;
import com.codenvy.im.user.UserCredentials;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;

import org.apache.commons.io.FileUtils;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.codenvy.im.utils.Commons.getPrettyPrintingJson;
import static java.lang.Thread.sleep;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Dmytro Nochevnov
 */
public class TestDownloadInstallationManagerServiceImpl {
    private InstallationManagerService installationManagerService;

    private InstallationManager                    mockInstallationManager;
    private HttpTransport                          transport;
    private Artifact                               installManagerArtifact;
    private Artifact                               cdecArtifact;
    private UserCredentials                        testCredentials;
    private Path                                   pathCDEC;
    private Path                                   pathIM;
    private JacksonRepresentation<UserCredentials> userCredentialsRep;

    @BeforeMethod
    public void init() {
        initMocks();
        installationManagerService = new InstallationManagerServiceImpl(mockInstallationManager, transport, new DownloadDescriptorHolder());

        this.pathCDEC = Paths.get("./target/cdec.zip");
        this.pathIM = Paths.get("./target/im.zip");

        userCredentialsRep = new JacksonRepresentation<>(testCredentials);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        Files.deleteIfExists(pathCDEC);
        Files.deleteIfExists(pathIM);
    }

    public void initMocks() {
        mockInstallationManager = mock(InstallationManagerImpl.class);
        transport = mock(HttpTransport.class);
        installManagerArtifact = ArtifactFactory.createArtifact(InstallManagerArtifact.NAME);
        cdecArtifact = ArtifactFactory.createArtifact(CDECArtifact.NAME);
        testCredentials = new UserCredentials("auth token", "accountId");
    }

    @Test
    public void testDownload() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(cdecArtifact, "2.10.5");
                put(installManagerArtifact, "1.0.1");
            }
        }).when(mockInstallationManager).getUpdates(testCredentials.getToken());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                FileUtils.writeByteArrayToFile(pathCDEC.toFile(), new byte[100]);
                return pathCDEC;
            }
        }).when(mockInstallationManager).download(testCredentials, cdecArtifact, "2.10.5");
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                FileUtils.writeByteArrayToFile(pathIM.toFile(), new byte[50]);
                return pathIM;
            }
        }).when(mockInstallationManager).download(testCredentials, installManagerArtifact, "1.0.1");

        doReturn(pathCDEC).when(mockInstallationManager).getPathToBinaries(cdecArtifact, "2.10.5");
        doReturn(pathIM).when(mockInstallationManager).getPathToBinaries(installManagerArtifact, "1.0.1");

        doReturn(100L).when(mockInstallationManager).getBinariesSize(cdecArtifact, "2.10.5");
        doReturn(50L).when(mockInstallationManager).getBinariesSize(installManagerArtifact, "1.0.1");

        installationManagerService.startDownload("id1", userCredentialsRep);

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status

            String response = installationManagerService.downloadStatus("id1");
            info = DownloadStatusInfo.valueOf(response);
        } while (info.getDownloadResult() == null);

        assertEquals(getPrettyPrintingJson(info.getDownloadResult()), "{\n" +
                                                                      "  \"artifacts\": [\n" +
                                                                      "    {\n" +
                                                                      "      \"artifact\": \"cdec\",\n" +
                                                                      "      \"file\": \"./target/cdec.zip\",\n" +
                                                                      "      \"status\": \"SUCCESS\",\n" +
                                                                      "      \"version\": \"2.10.5\"\n" +
                                                                      "    },\n" +
                                                                      "    {\n" +
                                                                      "      \"artifact\": " +
                                                                      "\"installation-manager\",\n" +
                                                                      "      \"file\": \"./target/im.zip\",\n" +
                                                                      "      \"status\": \"SUCCESS\",\n" +
                                                                      "      \"version\": \"1.0.1\"\n" +
                                                                      "    }\n" +
                                                                      "  ],\n" +
                                                                      "  \"status\": \"OK\"\n" +
                                                                      "}");
    }

    @Test
    public void testDownloadSpecificArtifact() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(cdecArtifact, "2.10.5");
                put(installManagerArtifact, "1.0.1");
            }
        }).when(mockInstallationManager).getUpdates(testCredentials.getToken());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                FileUtils.writeByteArrayToFile(pathCDEC.toFile(), new byte[100]);
                return pathCDEC;
            }
        }).when(mockInstallationManager).download(testCredentials, cdecArtifact, "2.10.5");
        doReturn(pathCDEC).when(mockInstallationManager).getPathToBinaries(cdecArtifact, "2.10.5");
        doReturn(100L).when(mockInstallationManager).getBinariesSize(cdecArtifact, "2.10.5");

        installationManagerService.startDownload("cdec", "id2", userCredentialsRep);

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status
            
            String response = installationManagerService.downloadStatus("id2");
            info = DownloadStatusInfo.valueOf(response);
        } while (info.getDownloadResult() == null);

        assertEquals(getPrettyPrintingJson(info.getDownloadResult()), "{\n" +
                                                                      "  \"artifacts\": [{\n" +
                                                                      "    \"artifact\": \"cdec\",\n" +
                                                                      "    \"file\": \"./target/cdec.zip\",\n" +
                                                                      "    \"status\": \"SUCCESS\",\n" +
                                                                      "    \"version\": \"2.10.5\"\n" +
                                                                      "  }],\n" +
                                                                      "  \"status\": \"OK\"\n" +
                                                                      "}");
    }

    @Test
    public void testDownloadSpecificVersionArtifact() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(cdecArtifact, "2.10.5");
                put(installManagerArtifact, "1.0.1");
            }
        }).when(mockInstallationManager).getUpdates(testCredentials.getToken());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                FileUtils.writeByteArrayToFile(pathCDEC.toFile(), new byte[100]);
                return pathCDEC;
            }
        }).when(mockInstallationManager).download(testCredentials, cdecArtifact, "2.10.5");
        doReturn(pathCDEC).when(mockInstallationManager).getPathToBinaries(cdecArtifact, "2.10.5");
        doReturn(100L).when(mockInstallationManager).getBinariesSize(cdecArtifact, "2.10.5");

        installationManagerService.startDownload("cdec", "2.10.5", "id3", userCredentialsRep);

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status
            
            String response = installationManagerService.downloadStatus("id3");
            info = DownloadStatusInfo.valueOf(response);
        } while (info.getDownloadResult() == null);

        assertEquals(getPrettyPrintingJson(info.getDownloadResult()), "{\n" +
                                                                      "  \"artifacts\": [{\n" +
                                                                      "    \"artifact\": \"cdec\",\n" +
                                                                      "    \"file\": \"./target/cdec.zip\",\n" +
                                                                      "    \"status\": \"SUCCESS\",\n" +
                                                                      "    \"version\": \"2.10.5\"\n" +
                                                                      "  }],\n" +
                                                                      "  \"status\": \"OK\"\n" +
                                                                      "}");
    }

    @Test
    public void testDownloadErrorIfUpdatesAbsent() throws Exception {
        doReturn(Collections.emptyMap()).when(mockInstallationManager).getUpdates(testCredentials.getToken());

        installationManagerService.startDownload("id4", userCredentialsRep);

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status

            String response = installationManagerService.downloadStatus("id4");
            info = DownloadStatusInfo.valueOf(response);
        } while (info.getDownloadResult() == null);

        assertEquals(getPrettyPrintingJson(info.getDownloadResult()), "{\n" +
                                                                      "  \"artifacts\": [],\n" +
                                                                      "  \"status\": \"OK\"\n" +
                                                                      "}");

        installationManagerService.startDownload("cdec", "id5", userCredentialsRep);

        do {
            sleep(100); // due to async request, wait a bit to get proper download status
            
            String response = installationManagerService.downloadStatus("id5");
            info = DownloadStatusInfo.valueOf(response);
        } while (info.getDownloadResult() == null);

        assertEquals(getPrettyPrintingJson(info.getDownloadResult()), "{\n" +
                                                                      "  \"message\": \"There is no any version of artifact 'cdec'\"," +
                                                                      "\n" +
                                                                      "  \"status\": \"ERROR\"\n" +
                                                                      "}");

        installationManagerService.startDownload("unknown", "id6", userCredentialsRep);

        do {
            sleep(100); // due to async request, wait a bit to get proper download status
            
            String response = installationManagerService.downloadStatus("id6");
            info = DownloadStatusInfo.valueOf(response);
        } while (info.getDownloadResult() == null);

        assertEquals(getPrettyPrintingJson(info.getDownloadResult()), "{\n" +
                                                                      "  \"message\": \"Artifact 'unknown' not found\",\n" +
                                                                      "  \"status\": \"ERROR\"\n" +
                                                                      "}");
    }

    @Test
    public void testDownloadErrorIfSpecificVersionArtifactAbsent() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(cdecArtifact, "2.10.5");
            }
        }).when(mockInstallationManager).getUpdates(testCredentials.getToken());

        doThrow(new ArtifactNotFoundException("cdec", "2.10.4")).when(mockInstallationManager).getPathToBinaries(cdecArtifact, "2.10.4");
        doThrow(new ArtifactNotFoundException("cdec", "2.10.4")).when(mockInstallationManager).getBinariesSize(cdecArtifact, "2.10.4");
        doThrow(new ArtifactNotFoundException("cdec", "2.10.4")).when(mockInstallationManager).download(testCredentials, cdecArtifact, "2.10.4");

        installationManagerService.startDownload("cdec", "2.10.4", "id7", userCredentialsRep);

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status
            
            String response = installationManagerService.downloadStatus("id7");
            info = DownloadStatusInfo.valueOf(response);
        } while (info.getDownloadResult() == null);

        assertEquals(getPrettyPrintingJson(info.getDownloadResult()), "{\n" +
                                                                      "  \"message\": \"Artifact 'cdec' version '2.10.4' not found\",\n" +
                                                                      "  \"status\": \"ERROR\"\n" +
                                                                      "}");
    }

    @Test
    public void testDownloadErrorIfAuthenticationFailedOnGetUpdates() throws Exception {
        when(mockInstallationManager.getUpdates(anyString())).thenThrow(new AuthenticationException());

        installationManagerService.startDownload("id8", userCredentialsRep);

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status
            
            String response = installationManagerService.downloadStatus("id8");
            info = DownloadStatusInfo.valueOf(response);
        } while (info.getDownloadResult() == null);

        assertEquals(getPrettyPrintingJson(info.getDownloadResult()), "{\n" +
                                                                      "  \"message\": \"Authentication error. Authentication token " +
                                                                      "might be expired or invalid.\",\n" +
                                                                      "  \"status\": \"ERROR\"\n" +
                                                                      "}");

    }

    @Test
    public void testDownloadErrorIfAuthenticationFailedOnDownload() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(cdecArtifact, "2.10.5");
            }
        }).when(mockInstallationManager).getUpdates(testCredentials.getToken());

        doThrow(new AuthenticationException()).when(mockInstallationManager).getPathToBinaries(cdecArtifact, "2.10.5");
        doThrow(new AuthenticationException()).when(mockInstallationManager).getBinariesSize(cdecArtifact, "2.10.5");
        doThrow(new AuthenticationException()).when(mockInstallationManager).download(testCredentials, cdecArtifact, "2.10.5");

        installationManagerService.startDownload("cdec", "2.10.5", "id9", userCredentialsRep);

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status

            String response = installationManagerService.downloadStatus("id9");
            info = DownloadStatusInfo.valueOf(response);
        } while (info.getDownloadResult() == null);

        assertEquals(getPrettyPrintingJson(info.getDownloadResult()), "{\n" +
                                                                      "  \"message\": \"Authentication error. Authentication token might be expired" +
                                                                      " or invalid.\",\n" +
                                                                      "  \"status\": \"ERROR\"\n" +
                                                                      "}");
    }

    @Test
    public void testDownloadErrorIfSubscriptionVerificationFailed() throws Exception {
        when(mockInstallationManager.getUpdates(anyString())).thenThrow(new IllegalStateException("Valid subscription is required to download cdec"));

        installationManagerService.startDownload("cdec", "id10", userCredentialsRep);

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status
            
            String response = installationManagerService.downloadStatus("id10");
            info = DownloadStatusInfo.valueOf(response);
        } while (info.getDownloadResult() == null);

        assertEquals(getPrettyPrintingJson(info.getDownloadResult()), "{\n" +
                                                                      "  \"message\": \"Valid subscription is required to download " +
                                                                      "cdec\",\n" +
                                                                      "  \"status\": \"ERROR\"\n" +
                                                                      "}");
    }

    @Test
    public void testGetDownloads() throws Exception {
        doReturn(new HashMap<Artifact, SortedMap<Version, Path>>() {{
            put(cdecArtifact, new TreeMap<Version, Path>() {{
                put(Version.valueOf("1.0.1"), Paths.get("target/file1"));
                put(Version.valueOf("1.0.2"), Paths.get("target/file2"));
            }});
        }}).when(mockInstallationManager).getDownloadedArtifacts();

        String response = installationManagerService.getDownloads(userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [\n" +
                                                      "    {\n" +
                                                      "      \"artifact\": \"cdec\",\n" +
                                                      "      \"file\": \"target/file1\",\n" +
                                                      "      \"status\": \"DOWNLOADED\",\n" +
                                                      "      \"version\": \"1.0.1\"\n" +
                                                      "    },\n" +
                                                      "    {\n" +
                                                      "      \"artifact\": \"cdec\",\n" +
                                                      "      \"file\": \"target/file2\",\n" +
                                                      "      \"status\": \"DOWNLOADED\",\n" +
                                                      "      \"version\": \"1.0.2\"\n" +
                                                      "    }\n" +
                                                      "  ],\n" +
                                                      "  \"status\": \"OK\"\n" +
                                                      "}");
    }

    @Test
    public void testGetDownloadsSpecificArtifact() throws Exception {
        doReturn(new HashMap<Artifact, SortedMap<Version, Path>>() {{
            put(cdecArtifact, new TreeMap<Version, Path>() {{
                put(Version.valueOf("1.0.1"), Paths.get("target/file1"));
                put(Version.valueOf("1.0.2"), Paths.get("target/file2"));
            }});
            put(installManagerArtifact, new TreeMap<Version, Path>() {{
                put(Version.valueOf("2.0.0"), Paths.get("target/file3"));
            }});
        }}).when(mockInstallationManager).getDownloadedArtifacts();

        String response = installationManagerService.getDownloads(installManagerArtifact.getName(), userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [{\n" +
                                                      "    \"artifact\": \"installation-manager\",\n" +
                                                      "    \"file\": \"target/file3\",\n" +
                                                      "    \"status\": \"READY_TO_INSTALL\",\n" +
                                                      "    \"version\": \"2.0.0\"\n" +
                                                      "  }],\n" +
                                                      "  \"status\": \"OK\"\n" +
                                                      "}");
    }

    @Test
    public void testGetDownloadsSpecificVersionCDECArtifact() throws Exception {
        doReturn(new HashMap<Artifact, SortedMap<Version, Path>>() {{
            put(cdecArtifact, new TreeMap<Version, Path>() {{
                put(Version.valueOf("1.0.1"), Paths.get("target/file1"));
                put(Version.valueOf("1.0.2"), Paths.get("target/file2"));
            }});
            put(installManagerArtifact, new TreeMap<Version, Path>() {{
                put(Version.valueOf("2.0.0"), Paths.get("target/file3"));
            }});
        }}).when(mockInstallationManager).getDownloadedArtifacts();

        String response = installationManagerService.getDownloads(cdecArtifact.getName(), "1.0.1", userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [{\n" +
                                                      "    \"artifact\": \"cdec\",\n" +
                                                      "    \"file\": \"target/file1\",\n" +
                                                      "    \"status\": \"DOWNLOADED\",\n" +
                                                      "    \"version\": \"1.0.1\"\n" +
                                                      "  }],\n" +
                                                      "  \"status\": \"OK\"\n" +
                                                      "}");
    }

    @Test
    public void testGetDownloadsOlderVersionInstallManagerArtifact() throws Exception {
        doReturn(new HashMap<Artifact, SortedMap<Version, Path>>() {{
            put(installManagerArtifact, new TreeMap<Version, Path>() {{
                put(Version.valueOf("1.0.0-SNAPSHOT"), Paths.get("target/file1"));  // current version of IM is stored in the test/resources/codenvy/BuildInfo.properties
            }});
        }}).when(mockInstallationManager).getDownloadedArtifacts();

        String response = installationManagerService.getDownloads(installManagerArtifact.getName(), "1.0.0-SNAPSHOT", userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [{\n" +
                                                      "    \"artifact\": \"installation-manager\",\n" +
                                                      "    \"file\": \"target/file1\",\n" +
                                                      "    \"status\": \"DOWNLOADED\",\n" +
                                                      "    \"version\": \"1.0.0-SNAPSHOT\"\n" +
                                                      "  }],\n" +
                                                      "  \"status\": \"OK\"\n" +
                                                      "}");
    }

    @Test
    public void testGetDownloadsNewVersionInstallManagerArtifact() throws Exception {
        doReturn(new HashMap<Artifact, SortedMap<Version, Path>>() {{
            put(installManagerArtifact, new TreeMap<Version, Path>() {{
                put(Version.valueOf("2.0.0-SNAPSHOT"), Paths.get("target/file1"));  // current version of IM is stored in the test/resources/codenvy/BuildInfo.properties
            }});
        }}).when(mockInstallationManager).getDownloadedArtifacts();

        String response = installationManagerService.getDownloads(userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [{\n" +
                                                      "    \"artifact\": \"installation-manager\",\n" +
                                                      "    \"file\": \"target/file1\",\n" +
                                                      "    \"status\": \"READY_TO_INSTALL\",\n" +
                                                      "    \"version\": \"2.0.0-SNAPSHOT\"\n" +
                                                      "  }],\n" +
                                                      "  \"status\": \"OK\"\n" +
                                                      "}");
    }


    @Test
    public void testGetDownloadsSpecificArtifactShouldReturnEmptyList() throws Exception {
        doReturn(new HashMap<Artifact, SortedMap<Version, Path>>() {{
            put(cdecArtifact, new TreeMap<Version, Path>() {{
                put(Version.valueOf("1.0.1"), Paths.get("target/file1"));
                put(Version.valueOf("1.0.2"), Paths.get("target/file2"));
            }});
        }}).when(mockInstallationManager).getDownloadedArtifacts();

        String response = installationManagerService.getDownloads(installManagerArtifact.getName(), userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"artifacts\": [],\n" +
                                                      "  \"status\": \"OK\"\n" +
                                                      "}");
    }


    @Test
    public void testDownloadSkipAlreadyDownloaded() throws Exception {
        doReturn(new LinkedHashMap<Artifact, String>() {
            {
                put(cdecArtifact, "2.10.5");
                put(installManagerArtifact, "1.0.1");
            }
        }).when(mockInstallationManager).getUpdates(testCredentials.getToken());
        doReturn(Paths.get("cdec.zip")).when(mockInstallationManager).download(testCredentials, cdecArtifact, "2.10.5");
        doReturn(Paths.get("im.zip")).when(mockInstallationManager).download(testCredentials, installManagerArtifact, "1.0.1");

        doReturn(Paths.get("cdec.zip")).when(mockInstallationManager).getPathToBinaries(cdecArtifact, "2.10.5");
        doReturn(Paths.get("im.zip")).when(mockInstallationManager).getPathToBinaries(installManagerArtifact, "1.0.1");

        doReturn(100L).when(mockInstallationManager).getBinariesSize(cdecArtifact, "2.10.5");
        doReturn(50L).when(mockInstallationManager).getBinariesSize(installManagerArtifact, "1.0.1");

        // mark IM as already downloaded artifact
        doReturn(new HashMap<Artifact, SortedMap<Version, Path>>() {{
            put(installManagerArtifact, new TreeMap<Version, Path>() {{
                put(Version.valueOf("1.0.1"), Paths.get("im.zip"));
            }});
        }}).when(mockInstallationManager).getDownloadedArtifacts();

        String response = installationManagerService.startDownload("id11", userCredentialsRep);
        assertTrue(ResponseCode.OK.in(response));

        DownloadStatusInfo info;
        do {
            sleep(100); // due to async request, wait a bit to get proper download status

            response = installationManagerService.downloadStatus("id11");
            assertTrue(ResponseCode.OK.in(response));

            info = DownloadStatusInfo.valueOf(response);
        } while (info.getDownloadResult() == null);

        assertEquals(getPrettyPrintingJson(info.getDownloadResult()), "{\n" +
                                                                      "  \"artifacts\": [{\n" +
                                                                      "    \"artifact\": \"cdec\",\n" +
                                                                      "    \"file\": \"cdec.zip\",\n" +
                                                                      "    \"status\": \"SUCCESS\",\n" +
                                                                      "    \"version\": \"2.10.5\"\n" +
                                                                      "  }],\n" +
                                                                      "  \"status\": \"OK\"\n" +
                                                                      "}");
    }
}
