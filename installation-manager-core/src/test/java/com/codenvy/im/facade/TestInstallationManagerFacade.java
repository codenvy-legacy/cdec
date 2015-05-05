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
import com.codenvy.im.backup.BackupConfig;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.node.NodeConfig;
import com.codenvy.im.request.Request;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.HttpException;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;

import org.eclipse.che.api.auth.shared.dto.Credentials;
import org.eclipse.che.commons.json.JsonParseException;
import org.eclipse.che.dto.server.JsonStringMapImpl;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author Dmytro Nochevnov
 */
public class TestInstallationManagerFacade {
    private InstallationManagerFacade installationManagerFacade;

    private InstallationManager mockInstallationManager;
    private HttpTransport       mockTransport;
    private Artifact            cdecArtifact;
    private UserCredentials     testCredentials;

    @BeforeMethod
    public void setUp() throws Exception {
        mockInstallationManager = mock(InstallationManager.class);
        mockTransport = mock(HttpTransport.class);
        cdecArtifact = createArtifact(CDECArtifact.NAME);
        installationManagerFacade = new InstallationManagerFacade("update/endpoint", "api/endpoint", mockInstallationManager, mockTransport);
        testCredentials = new UserCredentials("auth token");
    }

    @Test
    public void testInstall() throws Exception {
        InstallOptions installOptions = new InstallOptions();
        Request request = new Request().setUserCredentials(testCredentials)
                                       .setArtifactName(cdecArtifact.getName())
                                       .setInstallOptions(installOptions);
        Version version = Version.valueOf("2.10.5");

        doReturn(version).when(mockInstallationManager).getLatestInstallableVersion(testCredentials.getToken(), cdecArtifact);
        doNothing().when(mockInstallationManager).install(testCredentials.getToken(), cdecArtifact, version, installOptions);


        String response = installationManagerFacade.install(request);
        assertEquals(response, "{\n" +
                               "  \"artifacts\" : [ {\n" +
                               "    \"artifact\" : \"codenvy\",\n" +
                               "    \"version\" : \"2.10.5\",\n" +
                               "    \"status\" : \"SUCCESS\"\n" +
                               "  } ],\n" +
                               "  \"status\" : \"OK\"\n" +
                               "}");
    }

    @Test
    public void testInstallError() throws Exception {
        InstallOptions installOptions = new InstallOptions();
        Request request = new Request().setUserCredentials(testCredentials)
                                       .setArtifactName(cdecArtifact.getName())
                                       .setVersion("1.0.1")
                                       .setInstallOptions(installOptions);

        doThrow(new IOException("I/O error")).when(mockInstallationManager)
                                             .install(testCredentials.getToken(), cdecArtifact, Version.valueOf("1.0.1"), installOptions);


        String response = installationManagerFacade.install(request);
        assertEquals(response, "{\n" +
                               "  \"artifacts\" : [ {\n" +
                               "    \"artifact\" : \"codenvy\",\n" +
                               "    \"version\" : \"1.0.1\",\n" +
                               "    \"status\" : \"FAILURE\"\n" +
                               "  } ],\n" +
                               "  \"message\" : \"I/O error\",\n" +
                               "  \"status\" : \"ERROR\"\n" +
                               "}");
    }

    @Test
    public void testGetVersionToInstallVersionSetExplicitly() throws Exception {
        Request request = new Request().setUserCredentials(testCredentials).setArtifactName(cdecArtifact.getName()).setVersion("1.0.1");

        doReturn(Version.valueOf("1.0.2")).when(mockInstallationManager).getLatestInstallableVersion(testCredentials.getToken(), cdecArtifact);
        doReturn(ImmutableSortedMap.of(Version.valueOf("1.0.3"), Paths.get("some path"))).when(mockInstallationManager)
                                                                                         .getDownloadedVersions(cdecArtifact);

        Version version = installationManagerFacade.doGetVersionToInstall(request);
        assertEquals(Version.valueOf("1.0.1"), version);
    }

    @Test
    public void testGetVersionToInstallFirstInstallStep() throws Exception {
        Request request = new Request().setUserCredentials(testCredentials).setArtifactName(cdecArtifact.getName());

        doReturn(Version.valueOf("1.0.2")).when(mockInstallationManager).getLatestInstallableVersion(testCredentials.getToken(), cdecArtifact);
        doReturn(ImmutableSortedMap.of(Version.valueOf("1.0.3"), Paths.get("some path"))).when(mockInstallationManager)
                                                                                         .getDownloadedVersions(cdecArtifact);

        Version version = installationManagerFacade.doGetVersionToInstall(request);
        assertEquals(Version.valueOf("1.0.2"), version);
    }

    @Test
    public void testGetVersionToInstallInstallInProgress() throws Exception {
        InstallOptions installOptionsWithStep1 = new InstallOptions();
        installOptionsWithStep1.setStep(1);

        Request request = new Request().setUserCredentials(testCredentials)
                                       .setArtifactName(cdecArtifact.getName())
                                       .setInstallOptions(installOptionsWithStep1);

        doReturn(Version.valueOf("1.0.4")).when(mockInstallationManager).getLatestInstallableVersion(testCredentials.getToken(), cdecArtifact);
        doReturn(ImmutableSortedMap.of(Version.valueOf("1.0.3"), Paths.get("some path"))).when(mockInstallationManager)
                                                                                         .getDownloadedVersions(cdecArtifact);

        Version version = installationManagerFacade.doGetVersionToInstall(request);
        assertEquals(Version.valueOf("1.0.3"), version);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testGetVersionToInstallErrorFirstInstallStep() throws Exception {
        Request request = new Request().setUserCredentials(testCredentials).setArtifactName(cdecArtifact.getName());

        doReturn(null).when(mockInstallationManager).getLatestInstallableVersion(testCredentials.getToken(), cdecArtifact);
        doReturn(ImmutableSortedMap.of(Version.valueOf("1.0.3"), Paths.get("some path"))).when(mockInstallationManager)
                                                                                         .getDownloadedVersions(cdecArtifact);

        installationManagerFacade.doGetVersionToInstall(request);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testGetVersionToInstallErrorInstallInProgress() throws Exception {
        InstallOptions installOptionsWithStep1 = new InstallOptions();
        installOptionsWithStep1.setStep(1);

        Request request = new Request().setUserCredentials(testCredentials)
                                       .setArtifactName(cdecArtifact.getName())
                                       .setInstallOptions(installOptionsWithStep1);

        doReturn(Version.valueOf("1.0.4")).when(mockInstallationManager).getLatestInstallableVersion(testCredentials.getToken(), cdecArtifact);
        doReturn(ImmutableSortedMap.of()).when(mockInstallationManager).getDownloadedVersions(cdecArtifact);

        installationManagerFacade.doGetVersionToInstall(request);
    }

    @Test
    public void testAddNode() throws IOException {
        doReturn(new NodeConfig(NodeConfig.NodeType.BUILDER, "builder.node.com", null)).when(mockInstallationManager).addNode("builder.node.com");
        assertEquals(installationManagerFacade.addNode("builder.node.com"), "{\n"
                                                                             + "  \"node\" : {\n"
                                                                             + "    \"type\" : \"BUILDER\",\n"
                                                                             + "    \"host\" : \"builder.node.com\",\n"
                                                                             + "    \"status\" : \"SUCCESS\"\n"
                                                                             + "  },\n"
                                                                             + "  \"status\" : \"OK\"\n"
                                                                             + "}");
    }


    @Test
    public void testAddNodeException() throws IOException {
        doThrow(new IOException("error")).when(mockInstallationManager).addNode("builder.node.com");

        assertEquals(installationManagerFacade.addNode("builder.node.com"), "{\n"
                                                                             + "  \"message\" : \"error\",\n"
                                                                             + "  \"status\" : \"ERROR\"\n"
                                                                             + "}");
    }

    @Test
    public void testRemoveNode() throws IOException {
        final String TEST_NODE_DNS = "builder.node.com";
        final NodeConfig TEST_NODE = new NodeConfig(NodeConfig.NodeType.BUILDER, TEST_NODE_DNS, null);
        doReturn(TEST_NODE).when(mockInstallationManager).removeNode(TEST_NODE_DNS);

        assertEquals(installationManagerFacade.removeNode(TEST_NODE_DNS), "{\n"
                                                                           + "  \"node\" : {\n"
                                                                           + "    \"type\" : \"BUILDER\",\n"
                                                                           + "    \"host\" : \"builder.node.com\",\n"
                                                                           + "    \"status\" : \"SUCCESS\"\n"
                                                                           + "  },\n"
                                                                           + "  \"status\" : \"OK\"\n"
                                                                           + "}");
    }

    @Test
    public void testRemoveNodeException() throws IOException {
        final String TEST_NODE_DNS = "builder.node.com";
        doThrow(new IOException("error")).when(mockInstallationManager).removeNode(TEST_NODE_DNS);

        assertEquals(installationManagerFacade.removeNode(TEST_NODE_DNS), "{\n"
                                                                           + "  \"message\" : \"error\",\n"
                                                                           + "  \"status\" : \"ERROR\"\n"
                                                                           + "}");
    }

    @Test
    public void testBackup() throws IOException {
        Path testBackupDirectory = Paths.get("test/backup/directory");
        Path testBackupFile = testBackupDirectory.resolve("backup.tar.gz");
        BackupConfig testBackupConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                          .setBackupDirectory(testBackupDirectory.toString());

        doReturn(testBackupConfig.setBackupFile(testBackupFile.toString())
                                 .setArtifactVersion("1.0.0"))
            .when(mockInstallationManager).backup(testBackupConfig);
        assertEquals(installationManagerFacade.backup(testBackupConfig), "{\n"
                                                                          + "  \"backup\" : {\n"
                                                                          + "    \"file\" : \"test/backup/directory/backup.tar.gz\",\n"
                                                                          + "    \"artifactInfo\" : {\n"
                                                                          + "      \"artifact\" : \"codenvy\",\n"
                                                                          + "      \"version\" : \"1.0.0\"\n"
                                                                          + "    },\n"
                                                                          + "    \"status\" : \"SUCCESS\"\n"
                                                                          + "  },\n"
                                                                          + "  \"status\" : \"OK\"\n"
                                                                          + "}");
    }


    @Test
    public void testBackupException() throws IOException {
        String testBackupDirectory = "test/backup/directory";
        BackupConfig testBackupConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                          .setBackupDirectory(testBackupDirectory);

        doThrow(new IOException("error")).when(mockInstallationManager).backup(testBackupConfig);

        assertEquals(installationManagerFacade.backup(testBackupConfig), "{\n"
                                                                          + "  \"backup\" : {\n"
                                                                          + "    \"artifactInfo\" : {\n"
                                                                          + "      \"artifact\" : \"codenvy\"\n"
                                                                          + "    },\n"
                                                                          + "    \"status\" : \"FAILURE\"\n"
                                                                          + "  },\n"
                                                                          + "  \"message\" : \"error\",\n"
                                                                          + "  \"status\" : \"ERROR\"\n"
                                                                          + "}");
    }

    @Test
    public void testRestore() throws IOException {
        String testBackupFile = "test/backup/directory/backup.tar.gz";
        BackupConfig testBackupConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                          .setBackupFile(testBackupFile);

        assertEquals(installationManagerFacade.restore(testBackupConfig), "{\n"
                                                                           + "  \"backup\" : {\n"
                                                                           + "    \"file\" : \"test/backup/directory/backup.tar.gz\",\n"
                                                                           + "    \"artifactInfo\" : {\n"
                                                                           + "      \"artifact\" : \"codenvy\"\n"
                                                                           + "    },\n"
                                                                           + "    \"status\" : \"SUCCESS\"\n"
                                                                           + "  },\n"
                                                                           + "  \"status\" : \"OK\"\n"
                                                                           + "}");
    }


    @Test
    public void testRestoreException() throws IOException {
        String testBackupFile = "test/backup/directory/backup.tar.gz";
        BackupConfig testBackupConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                          .setBackupFile(testBackupFile);

        doThrow(new IOException("error")).when(mockInstallationManager).restore(testBackupConfig);

        assertEquals(installationManagerFacade.restore(testBackupConfig), "{\n"
                                                                           + "  \"backup\" : {\n"
                                                                           + "    \"file\" : \"test/backup/directory/backup.tar.gz\",\n"
                                                                           + "    \"artifactInfo\" : {\n"
                                                                           + "      \"artifact\" : \"codenvy\"\n"
                                                                           + "    },\n"
                                                                           + "    \"status\" : \"FAILURE\"\n"
                                                                           + "  },\n"
                                                                           + "  \"message\" : \"error\",\n"
                                                                           + "  \"status\" : \"ERROR\"\n"
                                                                           + "}");
    }

    @Test
    public void testLoginToSaas() throws IOException, JsonParseException {
        final String TEST_USER_NAME = "user";
        final String TEST_USER_PASSWORD = "password";
        final String TEST_ACCESS_TOKEN = "accessToken";

        final String TEST_ACCESS_TOKEN_JSON = "{\"value\":\"" + TEST_ACCESS_TOKEN + "\"}";
        final String TEST_CREDENTIALS_JSON = "{\n"
                                             + "  \"username\": \"" + TEST_USER_NAME + "\",\n"
                                             + "  \"password\": \"" + TEST_USER_PASSWORD + "\"\n"
                                             + "}";

        Credentials testSaasUsernameAndPassword = Commons.createDtoFromJson(TEST_CREDENTIALS_JSON, Credentials.class);

        Object body = new JsonStringMapImpl<>(ImmutableMap.of("username", TEST_USER_NAME,
                                                              "password", TEST_USER_PASSWORD));

        doReturn(TEST_ACCESS_TOKEN_JSON).when(mockTransport).doPost("api/endpoint/auth/login", body, null);

        String result = installationManagerFacade.login(testSaasUsernameAndPassword);
        assertEquals(result, "{\n"
                             + "  \"value\" : \"accessToken\"\n"
                             + "}");
    }

    @Test
    public void testLoginToSaasWhenTokenNull() throws IOException, JsonParseException {
        final String TEST_USER_NAME = "user";
        final String TEST_USER_PASSWORD = "password";
        final String TEST_CREDENTIALS_JSON = "{\n"
                                             + "  \"username\": \"" + TEST_USER_NAME + "\",\n"
                                             + "  \"password\": \"" + TEST_USER_PASSWORD + "\"\n"
                                             + "}";

        Credentials testSaasUsernameAndPassword = Commons.createDtoFromJson(TEST_CREDENTIALS_JSON, Credentials.class);

        Object body = new JsonStringMapImpl<>(ImmutableMap.of("username", TEST_USER_NAME,
                                                              "password", TEST_USER_PASSWORD));

        doReturn(null).when(mockTransport).doPost("api/endpoint/auth/login", body, null);

        String result = installationManagerFacade.login(testSaasUsernameAndPassword);
        assertNull(result);
    }

    @Test(expectedExceptions = HttpException.class,
          expectedExceptionsMessageRegExp = "error")
    public void testLoginToSaasWhenException() throws IOException, JsonParseException {
        final String TEST_USER_NAME = "user";
        final String TEST_USER_PASSWORD = "password";
        final String TEST_CREDENTIALS_JSON = "{\n"
                                             + "  \"username\": \"" + TEST_USER_NAME + "\",\n"
                                             + "  \"password\": \"" + TEST_USER_PASSWORD + "\"\n"
                                             + "}";

        Credentials testSaasUsernameAndPassword = Commons.createDtoFromJson(TEST_CREDENTIALS_JSON, Credentials.class);

        Object body = new JsonStringMapImpl<>(ImmutableMap.of("username", TEST_USER_NAME,
                                                              "password", TEST_USER_PASSWORD));

        doThrow(new HttpException(500, "error")).when(mockTransport).doPost("api/endpoint/auth/login", body, null);

        installationManagerFacade.login(testSaasUsernameAndPassword);
    }

    @Test
    public void testChangeAdminPassword() throws Exception {
        byte[] pwd = "password".getBytes("UTF-8");
        assertEquals(installationManagerFacade.changeAdminPassword(pwd), "{\n" +
                                                                         "  \"status\" : \"OK\"\n" +
                                                                         "}");
    }

    @Test
    public void testChangeAdminPasswordError() throws Exception {
        byte[] pwd = "password".getBytes("UTF-8");
        doThrow(IOException.class).when(mockInstallationManager).changeAdminPassword(pwd);

        assertEquals(installationManagerFacade.changeAdminPassword(pwd), "{\n" +
                                                                         "  \"status\" : \"ERROR\"\n" +
                                                                         "}");
    }
}
