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
import com.codenvy.im.facade.InstallationManagerFacade;
import com.codenvy.im.managers.BackupConfig;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.request.Request;
import com.codenvy.im.response.ResponseCode;
import com.codenvy.im.saas.SaasUserCredentials;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.HttpException;

import org.eclipse.che.api.auth.shared.dto.Credentials;
import org.eclipse.che.commons.json.JsonParseException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
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
    public static final String TEST_ACCOUNT_NAME     = "account";
    public static final String TEST_USER_NAME        = "user";
    public static final String TEST_USER_PASSWORD    = "password";

    public static final String TEST_SYSTEM_ADMIN_NAME = "admin";

    private static final String TEST_USER_ACCOUNT_REFERENCE_JSON = "{" +
                                                                   "  \"name\":\"" + TEST_ACCOUNT_NAME + "\"," +
                                                                   "  \"id\":\"" + TEST_ACCOUNT_ID + "\"," +
                                                                   "  \"links\":[]" +
                                                                   "}";
    private static final String TEST_ACCESS_TOKEN_JSON           = "{\"value\":\"" + TEST_ACCESS_TOKEN + "\"}";
    private static final String TEST_CREDENTIALS_JSON            = "{\n"
                                                                   + "  \"username\": \"" + TEST_USER_NAME + "\",\n"
                                                                   + "  \"password\": \"" + TEST_USER_PASSWORD + "\"\n"
                                                                   + "}";

    private InstallationManagerService service;

    @Mock
    private InstallationManagerFacade mockFacade;
    @Mock
    private ConfigManager             configManager;
    @Mock
    private Principal                 mockPrincipal;
    @Mock
    private SecurityContext           mockSecurityContext;

    private com.codenvy.im.response.Response mockFacadeOkResponse = new com.codenvy.im.response.Response().setStatus(ResponseCode.OK);

    private com.codenvy.im.response.Response mockFacadeErrorResponse = new com.codenvy.im.response.Response().setStatus(ResponseCode.ERROR)
                                                                                                             .setMessage("error");

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        service = spy(new InstallationManagerService(mockFacade, configManager));

        doReturn(mockPrincipal).when(mockSecurityContext).getUserPrincipal();
        doReturn(TEST_SYSTEM_ADMIN_NAME).when(mockPrincipal).getName();
    }

    @Test
    public void testStartDownload() throws Exception {
        Request testRequest = new Request().setArtifactName(CODENVY_ARTIFACT_NAME)
                                           .setVersion(TEST_VERSION);

        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).startDownload(testRequest);
        Response result = service.startDownload(CODENVY_ARTIFACT_NAME, TEST_VERSION);
        checkEmptyOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).startDownload(testRequest);
        result = service.startDownload(CODENVY_ARTIFACT_NAME, TEST_VERSION);
        checkErrorResponse(result);
    }

    @Test
    public void testStopDownload() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).stopDownload();
        Response result = service.stopDownload();
        checkEmptyOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).stopDownload();
        result = service.stopDownload();
        checkErrorResponse(result);
    }

    @Test
    public void testGetDownloadStatus() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).getDownloadStatus();
        Response result = service.getDownloadStatus();
        checkEmptyOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).getDownloadStatus();
        result = service.getDownloadStatus();
        checkErrorResponse(result);
    }

    @Test
    public void testGetUpdates() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).getUpdates();
        Response result = service.getUpdates();
        checkEmptyOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).getUpdates();
        result = service.getUpdates();
        checkErrorResponse(result);
    }

    @Test
    public void testGetDownloads() throws Exception {
        Request testRequest = new Request().setArtifactName(CODENVY_ARTIFACT_NAME);

        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).getDownloads(testRequest);
        Response result = service.getDownloads(CODENVY_ARTIFACT_NAME);
        checkEmptyOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).getDownloads(testRequest);
        result = service.getDownloads(CODENVY_ARTIFACT_NAME);
        checkErrorResponse(result);
    }

    @Test
    public void testGetInstalledVersions() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).getInstalledVersions();
        Response result = service.getInstalledVersions();
        checkEmptyOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).getInstalledVersions();
        result = service.getInstalledVersions();
        checkErrorResponse(result);
    }

    @Test
    public void testUpdateCodenvy() throws Exception {
        doReturn(InstallType.SINGLE_SERVER).when(configManager).detectInstallationType();

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
        checkEmptyOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).install(testRequest);
        result = service.updateCodenvy(testStep, testConfigProperties);
        checkErrorResponse(result);
    }

    @Test
    public void testUpdateCodenvyWhenConfigNull() throws Exception {
        doReturn(InstallType.SINGLE_SERVER).when(configManager).detectInstallationType();

        int testStep = 1;
        InstallOptions testInstallOptions = new InstallOptions().setInstallType(InstallType.SINGLE_SERVER)
                                                                .setConfigProperties(new HashMap<String, String>())
                                                                .setStep(testStep);

        Request testRequest = new Request().setArtifactName(CODENVY_ARTIFACT_NAME)
                                           .setInstallOptions(testInstallOptions);

        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).install(testRequest);
        Response result = service.updateCodenvy(testStep, null);
        checkEmptyOkResponse(result);
    }

    @Test
    public void testUpdateImCliClient() throws Exception {
        String cliUserHomeDir = "/home/test";
        InstallOptions testInstallOptions = new InstallOptions().setCliUserHomeDir(cliUserHomeDir);

        Request testRequest = new Request().setArtifactName(InstallManagerArtifact.NAME)
                                           .setInstallOptions(testInstallOptions);

        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).install(testRequest);
        Response result = service.updateImCliClient(cliUserHomeDir);
        checkEmptyOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).install(testRequest);
        result = service.updateImCliClient(cliUserHomeDir);
        checkErrorResponse(result);
    }

    @Test
    public void testGetUpdateCodenvyInfo() throws Exception {
        doReturn(InstallType.SINGLE_SERVER).when(configManager).detectInstallationType();

        InstallOptions testInstallOptions = new InstallOptions().setInstallType(InstallType.SINGLE_SERVER);

        Request testRequest = new Request().setArtifactName(CDECArtifact.NAME)
                                           .setInstallOptions(testInstallOptions);

        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).getInstallInfo(testRequest);
        Response result = service.getUpdateCodenvyInfo();
        checkEmptyOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).getInstallInfo(testRequest);
        result = service.getUpdateCodenvyInfo();
        checkErrorResponse(result);
    }

    @Test
    public void testGetConfig() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).getConfig();
        Response result = service.getConfig();
        checkEmptyOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).getConfig();
        result = service.getConfig();
        checkErrorResponse(result);
    }

    @Test
    public void testAddNode() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).addNode("dns");
        Response result = service.addNode("dns");
        checkEmptyOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).addNode("dns");
        result = service.addNode("dns");
        checkErrorResponse(result);
    }

    @Test
    public void testRemoveNode() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).removeNode("dns");
        Response result = service.removeNode("dns");
        checkEmptyOkResponse(result);

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
        checkEmptyOkResponse(result);

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
        checkEmptyOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).restore(testBackupConfig);
        result = service.restore(CODENVY_ARTIFACT_NAME, testBackupFilePath);
        checkErrorResponse(result);
    }

    @Test
    public void testAddTrialSubscription() throws Exception {
        SaasUserCredentials testUserCredentials = new SaasUserCredentials(TEST_ACCESS_TOKEN, TEST_ACCOUNT_ID);
        Request testRequest = new Request().setSaasUserCredentials(testUserCredentials);

        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).addTrialSubscription(testRequest);

        service.users.put(TEST_SYSTEM_ADMIN_NAME, testUserCredentials);

        Response result = service.addTrialSubscription(mockSecurityContext);
        checkEmptyOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).addTrialSubscription(testRequest);
        result = service.addTrialSubscription(mockSecurityContext);
        checkErrorResponse(result);
    }

    @Test
    public void testCheckSubscription() throws Exception {
        SaasUserCredentials testUserCredentials = new SaasUserCredentials(TEST_ACCOUNT_ID, TEST_ACCESS_TOKEN);
        Request testRequest = new Request().setSaasUserCredentials(testUserCredentials);

        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).checkSubscription(TEST_SUBSCRIPTION_ID, testRequest);

        service.users.put(TEST_SYSTEM_ADMIN_NAME, testUserCredentials);

        Response result = service.checkSubscription(TEST_SUBSCRIPTION_ID, mockSecurityContext);
        checkEmptyOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).checkSubscription(TEST_SUBSCRIPTION_ID, testRequest);
        result = service.checkSubscription(TEST_SUBSCRIPTION_ID, mockSecurityContext);
        checkErrorResponse(result);
    }

    @Test
    public void testHandleIncorrectFacadeResponse() throws Exception {
        doReturn("{").when(mockFacade).getConfig();
        Response result = service.getConfig();

        assertEquals(result.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        String facadeResponse = (String)result.getEntity();
        assertTrue(facadeResponse.contains("com.fasterxml.jackson.core.JsonParseException: " +
                                           "Unexpected end-of-input: expected close marker for OBJECT"));
    }

    @Test
    public void testLoginToSaas() throws IOException, JsonParseException {
        Credentials testSaasUsernameAndPassword = Commons.createDtoFromJson(TEST_CREDENTIALS_JSON, Credentials.class);
        Request testRequest = new Request().setSaasUserCredentials(new SaasUserCredentials(TEST_ACCESS_TOKEN));

        doReturn(TEST_ACCESS_TOKEN_JSON).when(mockFacade).loginToCodenvySaaS(testSaasUsernameAndPassword);
        doReturn(TEST_USER_ACCOUNT_REFERENCE_JSON).when(mockFacade).getAccountReferenceWhereUserIsOwner(null, testRequest);

        Response result = service.loginToCodenvySaas(testSaasUsernameAndPassword, mockSecurityContext);
        assertEquals(result.getEntity(), "{\n"
                                         + "  \"message\" : \"Your Codenvy account 'account' will be used to verify on-premises subscription.\",\n"
                                         + "  \"status\" : \"OK\"\n"
                                         + "}");
        assertEquals(result.getStatus(), Response.Status.OK.getStatusCode());

        assertEquals(service.users.size(), 1);
        assertTrue(service.users.containsKey(TEST_SYSTEM_ADMIN_NAME));

        SaasUserCredentials testSaasSaasUserCredentials = service.users.get(TEST_SYSTEM_ADMIN_NAME);
        assertEquals(testSaasSaasUserCredentials.getAccountId(), TEST_ACCOUNT_ID);
        assertEquals(testSaasSaasUserCredentials.getToken(), TEST_ACCESS_TOKEN);
    }

    @Test
    public void testLoginToSaasWhenHttpException() throws IOException, JsonParseException {
        Credentials testSaasUsernameAndPassword = Commons.createDtoFromJson(TEST_CREDENTIALS_JSON, Credentials.class);
        Request testRequest = new Request().setSaasUserCredentials(new SaasUserCredentials(TEST_ACCESS_TOKEN));

        doThrow(new HttpException(500, "Login error")).when(mockFacade).loginToCodenvySaaS(testSaasUsernameAndPassword);
        Response result = service.loginToCodenvySaas(testSaasUsernameAndPassword, mockSecurityContext);
        assertEquals(result.getEntity(), "{\n"
                                         + "  \"message\" : \"Login error\",\n"
                                         + "  \"status\" : \"ERROR\"\n"
                                         + "}");
        assertEquals(result.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertEquals(service.users.size(), 0);

        doReturn(TEST_ACCESS_TOKEN_JSON).when(mockFacade).loginToCodenvySaaS(testSaasUsernameAndPassword);
        doThrow(new HttpException(500, "Login error")).when(mockFacade).getAccountReferenceWhereUserIsOwner(null, testRequest);
        result = service.loginToCodenvySaas(testSaasUsernameAndPassword, mockSecurityContext);
        assertEquals(result.getEntity(), "{\n"
                                         + "  \"message\" : \"Login error\",\n"
                                         + "  \"status\" : \"ERROR\"\n"
                                         + "}");
        assertEquals(result.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertEquals(service.users.size(), 0);
    }

    @Test
    public void testLoginToSaasWhenNullToken() throws IOException, JsonParseException {
        Credentials testSaasUsernameAndPassword = Commons.createDtoFromJson(TEST_CREDENTIALS_JSON, Credentials.class);

        doReturn(null).when(mockFacade).loginToCodenvySaaS(testSaasUsernameAndPassword);
        Response result = service.loginToCodenvySaas(testSaasUsernameAndPassword, mockSecurityContext);
        assertEquals(result.getEntity(), "Login impossible.");
        assertEquals(result.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertEquals(service.users.size(), 0);
    }

    @Test
    public void testLoginToSaasWhenNullAccountReference() throws IOException, JsonParseException {
        Credentials testSaasUsernameAndPassword = Commons.createDtoFromJson(TEST_CREDENTIALS_JSON, Credentials.class);
        Request testRequest = new Request().setSaasUserCredentials(new SaasUserCredentials(TEST_ACCESS_TOKEN));

        doReturn(TEST_ACCESS_TOKEN_JSON).when(mockFacade).loginToCodenvySaaS(testSaasUsernameAndPassword);
        doReturn(null).when(mockFacade).getAccountReferenceWhereUserIsOwner(null, testRequest);

        Response result = service.loginToCodenvySaas(testSaasUsernameAndPassword, mockSecurityContext);
        assertEquals(result.getEntity(), "You are logged as a user which does not have an account/owner role in any account. This" +
                                         " likely means that you used the wrong credentials to access Codenvy.");
        assertEquals(result.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertEquals(service.users.size(), 0);
    }

    private void checkEmptyOkResponse(Response result) {
        assertEquals(result.getStatus(), Response.Status.OK.getStatusCode());
        String facadeResponse = (String) result.getEntity();
        assertEquals(facadeResponse, mockFacadeOkResponse.toJson());
    }

    private void checkErrorResponse(Response result) {
        assertEquals(result.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        String facadeResponse = (String) result.getEntity();
        assertEquals(facadeResponse, mockFacadeErrorResponse.toJson());
    }
}
