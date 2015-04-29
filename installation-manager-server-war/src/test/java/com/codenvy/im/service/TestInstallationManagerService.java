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
package com.codenvy.im.service;

import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.backup.BackupConfig;
import com.codenvy.im.config.ConfigUtil;
import com.codenvy.im.facade.InstallationManagerFacade;
import com.codenvy.im.facade.UserCredentials;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.install.InstallType;
import com.codenvy.im.request.Request;
import com.codenvy.im.response.ResponseCode;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Dmytro Nochevnov
 */
public class TestInstallationManagerService {

    public static final String CODENVY_ARTIFACT_NAME = "codenvy";
    public static final String TEST_VERSION          = "1.0.0";
    public static final String TEST_ACCESS_TOKEN     = "accessToken";
    public static final String TEST_ACCOUNT_ID       = "accountId";
    public static final String TEST_SUBSCRIPTION_ID  = "subscriptionId";
    private InstallationManagerService service;

    @Mock
    private InstallationManagerFacade mockFacade;

    @Mock
    private ConfigUtil configUtil;

    private com.codenvy.im.response.Response mockFacadeOkResponse = new com.codenvy.im.response.Response().setStatus(ResponseCode.OK);

    private com.codenvy.im.response.Response mockFacadeErrorResponse = new com.codenvy.im.response.Response().setStatus(ResponseCode.ERROR)
                                                                                                             .setMessage("error");

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        service = spy(new InstallationManagerService(mockFacade, configUtil));
    }

    @Test
    public void testStartDownload() throws Exception {
        Request testRequest = new Request().setArtifactName(CODENVY_ARTIFACT_NAME)
                                           .setVersion(TEST_VERSION);

        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).startDownload(testRequest);
        Response result = service.startDownload(CODENVY_ARTIFACT_NAME, TEST_VERSION);
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).startDownload(testRequest);
        result = service.startDownload(CODENVY_ARTIFACT_NAME, TEST_VERSION);
        checkErrorResponse(result);
    }

    @Test
    public void testStopDownload() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).stopDownload();
        Response result = service.stopDownload();
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).stopDownload();
        result = service.stopDownload();
        checkErrorResponse(result);
    }

    @Test
    public void testGetDownloadStatus() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).getDownloadStatus();
        Response result = service.getDownloadStatus();
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).getDownloadStatus();
        result = service.getDownloadStatus();
        checkErrorResponse(result);
    }

    @Test
    public void testGetUpdates() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).getUpdates();
        Response result = service.getUpdates();
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).getUpdates();
        result = service.getUpdates();
        checkErrorResponse(result);
    }

    @Test
    public void testGetDownloads() throws Exception {
        Request testRequest = new Request().setArtifactName(CODENVY_ARTIFACT_NAME);

        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).getDownloads(testRequest);
        Response result = service.getDownloads(CODENVY_ARTIFACT_NAME);
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).getDownloads(testRequest);
        result = service.getDownloads(CODENVY_ARTIFACT_NAME);
        checkErrorResponse(result);
    }

    @Test
    public void testGetInstalledVersions() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).getInstalledVersions();
        Response result = service.getInstalledVersions();
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).getInstalledVersions();
        result = service.getInstalledVersions();
        checkErrorResponse(result);
    }

    @Test
    public void testUpdateCodenvy() throws Exception {
        doReturn(InstallType.SINGLE_SERVER).when(configUtil).detectInstallationType();

        Map<String, String> testConfigProperties = new HashMap<>();
        testConfigProperties.put("property1", "value1");
        testConfigProperties.put("property2", "value2");

        int testStep = 1;
        InstallOptions testInstallOptions = new InstallOptions().setInstallType(InstallType.SINGLE_SERVER)
                                                                .setConfigProperties(testConfigProperties)
                                                                .setStep(testStep);

        Request testRequest = new Request().setArtifactName(CODENVY_ARTIFACT_NAME)
                                           .setInstallOptions(testInstallOptions);

        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).install(testRequest);
        Response result = service.updateCodenvy(testStep, testConfigProperties);
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).install(testRequest);
        result = service.updateCodenvy(testStep, testConfigProperties);
        checkErrorResponse(result);
    }


    @Test
    public void testUpdateImCliClient() throws Exception {
        String cliUserHomeDir = "/home/test";
        InstallOptions testInstallOptions = new InstallOptions().setCliUserHomeDir(cliUserHomeDir);

        Request testRequest = new Request().setArtifactName(InstallManagerArtifact.NAME)
                                           .setInstallOptions(testInstallOptions);

        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).install(testRequest);
        Response result = service.updateImCliClient(cliUserHomeDir);
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).install(testRequest);
        result = service.updateImCliClient(cliUserHomeDir);
        checkErrorResponse(result);
    }

    @Test
    public void testGetUpdateCodenvyInfo() throws Exception {
        doReturn(InstallType.SINGLE_SERVER).when(configUtil).detectInstallationType();

        InstallOptions testInstallOptions = new InstallOptions().setInstallType(InstallType.SINGLE_SERVER);

        Request testRequest = new Request().setArtifactName(CDECArtifact.NAME)
                                           .setInstallOptions(testInstallOptions);

        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).getInstallInfo(testRequest);
        Response result = service.getUpdateCodenvyInfo();
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).getInstallInfo(testRequest);
        result = service.getUpdateCodenvyInfo();
        checkErrorResponse(result);
    }

    @Test
    public void testGetConfig() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).getConfig();
        Response result = service.getConfig();
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).getConfig();
        result = service.getConfig();
        checkErrorResponse(result);
    }

    @Test
    public void testAddNode() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).addNode("dns");
        Response result = service.addNode("dns");
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).addNode("dns");
        result = service.addNode("dns");
        checkErrorResponse(result);
    }

    @Test
    public void testRemoveNode() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).removeNode("dns");
        Response result = service.removeNode("dns");
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).removeNode("dns");
        result = service.removeNode("dns");
        checkErrorResponse(result);
    }

    @Test
    public void testBackup() throws Exception {
        String testBackupDirectoryPath = "test/path";
        BackupConfig testBackupConfig = new BackupConfig().setArtifactName(CODENVY_ARTIFACT_NAME)
                                                          .setBackupDirectory(testBackupDirectoryPath);

        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).backup(testBackupConfig);
        Response result = service.backup(CODENVY_ARTIFACT_NAME, testBackupDirectoryPath);
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).backup(testBackupConfig);
        result = service.backup(CODENVY_ARTIFACT_NAME, testBackupDirectoryPath);
        checkErrorResponse(result);
    }

    @Test
    public void testRestore() throws Exception {
        String testBackupFilePath = "test/path/backup";
        BackupConfig testBackupConfig = new BackupConfig().setArtifactName(CODENVY_ARTIFACT_NAME)
                                                          .setBackupFile(testBackupFilePath);

        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).restore(testBackupConfig);
        Response result = service.restore(CODENVY_ARTIFACT_NAME, testBackupFilePath);
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).restore(testBackupConfig);
        result = service.restore(CODENVY_ARTIFACT_NAME, testBackupFilePath);
        checkErrorResponse(result);
    }


    @Test
    public void testAddTrialSubscription() throws Exception {
        UserCredentials testUserCredentials = new UserCredentials(TEST_ACCOUNT_ID, TEST_ACCESS_TOKEN);
        Request testRequest = new Request().setUserCredentials(testUserCredentials);

        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).addTrialSubscription(testRequest);
        Response result = service.addTrialSubscription(TEST_ACCOUNT_ID, TEST_ACCESS_TOKEN);
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).addTrialSubscription(testRequest);
        result = service.addTrialSubscription(TEST_ACCOUNT_ID, TEST_ACCESS_TOKEN);
        checkErrorResponse(result);
    }

    @Test
    public void testCheckSubscription() throws Exception {
        UserCredentials testUserCredentials = new UserCredentials(TEST_ACCOUNT_ID, TEST_ACCESS_TOKEN);
        Request testRequest = new Request().setUserCredentials(testUserCredentials);

        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).checkSubscription(TEST_SUBSCRIPTION_ID, testRequest);
        Response result = service.checkSubscription(TEST_SUBSCRIPTION_ID, TEST_ACCOUNT_ID, TEST_ACCESS_TOKEN);
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).checkSubscription(TEST_SUBSCRIPTION_ID, testRequest);
        result = service.checkSubscription(TEST_SUBSCRIPTION_ID, TEST_ACCOUNT_ID, TEST_ACCESS_TOKEN);
        checkErrorResponse(result);
    }

    @Test
    public void testHandleIncorrectFacadeResponse() throws Exception {
        doReturn("{").when(mockFacade).getConfig();
        Response result = service.getConfig();

        assertEquals(result.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        String facadeResponse = (String)result.getEntity();
        assertTrue(facadeResponse.contains("org.eclipse.che.commons.json.JsonParseException: com.fasterxml.jackson.core.JsonParseException: " +
                                           "Unexpected end-of-input: expected close marker for OBJECT"));
    }

    private void checkOkResponse(Response result) {
        assertEquals(result.getStatus(), Response.Status.OK.getStatusCode());
        com.codenvy.im.response.Response facadeResponse = (com.codenvy.im.response.Response)result.getEntity();
        assertEquals(facadeResponse.toJson(), mockFacadeOkResponse.toJson());
    }

    private void checkErrorResponse(Response result) {
        assertEquals(result.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        com.codenvy.im.response.Response facadeResponse = (com.codenvy.im.response.Response)result.getEntity();
        assertEquals(facadeResponse.toJson(), mockFacadeErrorResponse.toJson());
    }
}
