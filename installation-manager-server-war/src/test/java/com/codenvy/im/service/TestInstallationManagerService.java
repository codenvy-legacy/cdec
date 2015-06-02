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

import com.codenvy.im.BaseTest;
import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.ArtifactFactory;
import com.codenvy.im.artifacts.ArtifactNotFoundException;
import com.codenvy.im.artifacts.ArtifactProperties;
import com.codenvy.im.facade.InstallationManagerFacade;
import com.codenvy.im.managers.BackupConfig;
import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.DownloadAlreadyStartedException;
import com.codenvy.im.managers.DownloadNotStartedException;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.managers.PropertyNotFoundException;
import com.codenvy.im.request.Request;
import com.codenvy.im.response.DownloadProgressDescriptor;
import com.codenvy.im.response.InstallArtifactResult;
import com.codenvy.im.response.InstallArtifactStatus;
import com.codenvy.im.response.ResponseCode;
import com.codenvy.im.saas.SaasAccountServiceProxy;
import com.codenvy.im.saas.SaasUserCredentials;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.HttpException;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.eclipse.che.api.account.shared.dto.SubscriptionDescriptor;
import org.eclipse.che.api.auth.AuthenticationException;
import org.eclipse.che.api.auth.server.dto.DtoServerImpls;
import org.eclipse.che.api.auth.shared.dto.Credentials;
import org.eclipse.che.dto.server.DtoFactory;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Dmytro Nochevnov
 */
public class TestInstallationManagerService extends BaseTest {

    public static final String ARTIFACT_NAME      = "codenvy";
    public static final String VERSION_NUMBER     = "1.0.0";
    public static final String TEST_ACCESS_TOKEN  = "accessToken";
    public static final String TEST_ACCOUNT_ID    = "accountId";
    public static final String TEST_ACCOUNT_NAME  = "account";
    public static final String TEST_USER_NAME     = "user";
    public static final String TEST_USER_PASSWORD = "password";

    public Version  version;
    public Artifact artifact;

    public static final String TEST_SYSTEM_ADMIN_NAME = "admin";

    private static final String TEST_CREDENTIALS_JSON = "{\n"
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
    private Config                    mockConfig;
    @Mock
    private Artifact                  mockArtifact;

    private com.codenvy.im.response.Response mockFacadeOkResponse;
    private com.codenvy.im.response.Response mockFacadeErrorResponse;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mockFacadeOkResponse = new com.codenvy.im.response.Response().setStatus(ResponseCode.OK);
        mockFacadeErrorResponse = new com.codenvy.im.response.Response().setStatus(ResponseCode.ERROR).setMessage("error");
        service = spy(new InstallationManagerService(mockFacade, configManager));

        doReturn(TEST_SYSTEM_ADMIN_NAME).when(mockPrincipal).getName();
        doReturn(mockArtifact).when(service).getArtifact(anyString());

        artifact = ArtifactFactory.createArtifact(ARTIFACT_NAME);
        version = Version.valueOf(VERSION_NUMBER);
    }

    @Test
    public void testStartDownload() throws Exception {
        Response result = service.startDownload(ARTIFACT_NAME, VERSION_NUMBER);

        assertEquals(result.getStatus(), Response.Status.ACCEPTED.getStatusCode());
        assertTrue(result.getEntity() instanceof DownloadToken);
        assertNotNull(((DownloadToken)result.getEntity()).getId());
        verify(mockFacade).startDownload(ArtifactFactory.createArtifact("codenvy"), version);
    }

    @Test
    public void testStartDownloadShouldReturnBadRequestIfArtifactInvalid() throws Exception {
        Response result = service.startDownload("no_name", null);
        assertEquals(result.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testStartDownloadShouldReturnBadRequestIfVersionInvalid() throws Exception {
        Response result = service.startDownload(ARTIFACT_NAME, "1");
        assertEquals(result.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testStartDownloadShouldReturnConflictWhenDownloadStarted() throws Exception {
        doThrow(new DownloadAlreadyStartedException()).when(mockFacade).startDownload(artifact, version);

        Response result = service.startDownload(ARTIFACT_NAME, VERSION_NUMBER);

        assertEquals(result.getStatus(), Response.Status.CONFLICT.getStatusCode());
        verify(mockFacade).startDownload(artifact, version);
    }

    @Test
    public void testStopDownload() throws Exception {
        Response result = service.stopDownload("id");
        assertEquals(result.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    public void testStopDownloadShouldReturnConflictWhenDownloadNotStarted() throws Exception {
        doThrow(new DownloadNotStartedException()).when(mockFacade).stopDownload();

        Response result = service.stopDownload("id");

        assertEquals(result.getStatus(), Response.Status.CONFLICT.getStatusCode());
    }

    @Test
    public void testGetDownloadProgress() throws Exception {
        DownloadProgressDescriptor progressDescriptor = new DownloadProgressDescriptor();
        doReturn(progressDescriptor).when(mockFacade).getDownloadProgress();

        Response result = service.getDownloadProgress("id");

        assertEquals(result.getStatus(), Response.Status.OK.getStatusCode());
        assertEquals(result.getEntity(), progressDescriptor);
    }

    @Test
    public void testGetDownloadProgressShouldReturnConflictWhenDownloadNotStarted() throws Exception {
        doThrow(new DownloadNotStartedException()).when(mockFacade).getDownloadProgress();

        Response result = service.getDownloadProgress("id");

        assertEquals(result.getStatus(), Response.Status.CONFLICT.getStatusCode());
    }

    @Test
    public void testGetUpdates() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).getUpdates();
        Response result = service.getUpdates();
        assertOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).getUpdates();
        result = service.getUpdates();
        assertErrorResponse(result);
    }

    @Test
    public void testGetDownloads() throws Exception {
        Request testRequest = new Request().setArtifactName(ARTIFACT_NAME);

        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).getDownloads(testRequest);
        Response result = service.getDownloads(ARTIFACT_NAME);
        assertOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).getDownloads(testRequest);
        result = service.getDownloads(ARTIFACT_NAME);
        assertErrorResponse(result);
    }

    @Test
    public void testGetInstalledVersionsShouldReturnOkStatus() throws Exception {
        doReturn(ImmutableList.of(new InstallArtifactResult().withVersion("1.0.1")
                                                             .withArtifact("codenvy")
                                                             .withStatus(InstallArtifactStatus.SUCCESS))).when(mockFacade).getInstalledVersions();

        Response result = service.getInstalledVersions();

        assertEquals(result.getStatus(), Response.Status.OK.getStatusCode());
    }

    @Test
    public void testGetInstalledVersionsShouldReturnErrorStatus() throws Exception {
        doThrow(new IOException("error")).when(mockFacade).getInstalledVersions();

        Response result = service.getInstalledVersions();

        assertEquals(result.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testUpdateCodenvy() throws Exception {
        doReturn(InstallType.SINGLE_SERVER).when(configManager).detectInstallationType();
        doReturn(Version.valueOf("3.1.0")).when(mockFacade).getLatestInstallableVersion(any(Artifact.class));
        doReturn(Collections.emptyList()).when(mockFacade).getUpdateInfo(any(Artifact.class), any(InstallType.class));

        Map<String, String> testConfigProperties = new HashMap<>();
        testConfigProperties.put("property1", "value1");
        testConfigProperties.put("property2", "value2");

        doReturn(testConfigProperties).when(configManager).prepareInstallProperties(null,
                                                                                    InstallType.SINGLE_SERVER,
                                                                                    ArtifactFactory.createArtifact(ARTIFACT_NAME),
                                                                                    Version.valueOf("3.1.0"));

        Response result = service.updateCodenvy();
        assertEquals(result.getStatus(), Response.Status.CREATED.getStatusCode());
    }

    @Test
    public void testGetConfig() throws Exception {
        doReturn(Collections.emptyMap()).when(mockFacade).getInstallationManagerProperties();
        Response response = service.getInstallationManagerServerConfig();
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
    }

    @Test
    public void testAddNode() throws Exception {
        doReturn(mockFacadeOkResponse).when(mockFacade).addNode("dns");
        Response result = service.addNode("dns");
        assertOkResponse(result);

        doReturn(mockFacadeErrorResponse).when(mockFacade).addNode("dns");
        result = service.addNode("dns");
        assertErrorResponse(result);
    }

    @Test
    public void testRemoveNode() throws Exception {
        doReturn(mockFacadeOkResponse).when(mockFacade).removeNode("dns");
        Response result = service.removeNode("dns");
        assertOkResponse(result);

        doReturn(mockFacadeErrorResponse).when(mockFacade).removeNode("dns");
        result = service.removeNode("dns");
        assertErrorResponse(result);
    }

    @Test
    public void testBackup() throws Exception {
        String testBackupDirectoryPath = "test/path";
        BackupConfig testBackupConfig = new BackupConfig().setArtifactName(ARTIFACT_NAME)
                                                          .setBackupDirectory(testBackupDirectoryPath);

        doReturn(mockFacadeOkResponse).when(mockFacade).backup(testBackupConfig);
        Response result = service.backup(ARTIFACT_NAME, testBackupDirectoryPath);
        assertOkResponse(result);

        doReturn(mockFacadeErrorResponse).when(mockFacade).backup(testBackupConfig);
        result = service.backup(ARTIFACT_NAME, testBackupDirectoryPath);
        assertErrorResponse(result);
    }

    @Test
    public void testRestore() throws Exception {
        String testBackupFilePath = "test/path/backup";
        BackupConfig testBackupConfig = new BackupConfig().setArtifactName(ARTIFACT_NAME)
                                                          .setBackupFile(testBackupFilePath);

        doReturn(mockFacadeOkResponse).when(mockFacade).restore(testBackupConfig);
        Response result = service.restore(ARTIFACT_NAME, testBackupFilePath);
        assertOkResponse(result);

        doReturn(mockFacadeErrorResponse).when(mockFacade).restore(testBackupConfig);
        result = service.restore(ARTIFACT_NAME, testBackupFilePath);
        assertErrorResponse(result);
    }

    @Test
    public void testAddTrialSubscription() throws Exception {
        SaasUserCredentials testUserCredentials = new SaasUserCredentials(TEST_ACCESS_TOKEN, TEST_ACCOUNT_ID);
        Request testRequest = new Request().setSaasUserCredentials(testUserCredentials);

        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).addTrialSaasSubscription(testRequest);

        service.saasUserCredentials = testUserCredentials;

        Response result = service.addTrialSubscription();
        assertOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).addTrialSaasSubscription(testRequest);
        result = service.addTrialSubscription();
        assertErrorResponse(result);
    }

    @Test
    public void testGetSubscription() throws Exception {
        SaasUserCredentials testUserCredentials = new SaasUserCredentials(TEST_ACCOUNT_ID, TEST_ACCESS_TOKEN);
        Request testRequest = new Request().setSaasUserCredentials(testUserCredentials);

        SimpleDateFormat subscriptionDateFormat = new SimpleDateFormat(SaasAccountServiceProxy.SUBSCRIPTION_DATE_FORMAT);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        String startDate = subscriptionDateFormat.format(cal.getTime());
        cal.add(Calendar.DATE, 2);
        String endDate = subscriptionDateFormat.format(cal.getTime());

        String testSubscriptionDescriptorJson = "{\n"
                                                + "  \"description\": \"On-Prem Commercial 25 Users\",\n"
                                                + "  \"startDate\" : \"" + startDate + "\",\n"
                                                + "  \"endDate\" : \"" + endDate + "\",\n"
                                                + "  \"links\": [\n"
                                                + "    {\n"
                                                + "      \"href\": \"https://codenvy-stg"
                                                + ".com/api/account/subscriptions/subscriptionoxmrh93dw3ceuegk\",\n"
                                                + "      \"rel\": \"get subscription by id\",\n"
                                                + "      \"produces\": \"application/json\",\n"
                                                + "      \"parameters\": [],\n"
                                                + "      \"method\": \"GET\"\n"
                                                + "    }\n"
                                                + "  ],\n"
                                                + "  \"properties\": {\n"
                                                + "    \"Users\": \"25\",\n"
                                                + "    \"Package\": \"Commercial\"\n"
                                                + "  },\n"
                                                + "  \"id\": \"subscriptionoxmrh93dw3ceuegk\",\n"
                                                + "  \"state\": \"ACTIVE\"\n"
                                                + "}";
        SubscriptionDescriptor descriptor = DtoFactory.getInstance().createDtoFromJson(testSubscriptionDescriptorJson, SubscriptionDescriptor.class);
        doReturn(descriptor).when(mockFacade).getSubscription(SaasAccountServiceProxy.ON_PREMISES, testRequest);

        service.saasUserCredentials = testUserCredentials;

        Response result = service.getOnPremisesSaasSubscription();
        assertEquals(result.getStatus(), Response.Status.OK.getStatusCode());
        SubscriptionDescriptor subscription = Commons.createDtoFromJson((String)result.getEntity(), SubscriptionDescriptor.class);
        assertEquals(subscription.getDescription(), "On-Prem Commercial 25 Users");
        assertEquals(subscription.getStartDate(), startDate);
        assertEquals(subscription.getEndDate(), endDate);
        assertTrue(subscription.getLinks().isEmpty());
        assertEquals(subscription.getId(), "subscriptionoxmrh93dw3ceuegk");
        assertEquals(subscription.getState().name(), "ACTIVE");
        assertEquals(subscription.getProperties().get("Users"), "25");
        assertEquals(subscription.getProperties().get("Package"), "Commercial");

        doThrow(new HttpException(500, "error")).when(mockFacade).getSubscription(SaasAccountServiceProxy.ON_PREMISES, testRequest);
        result = service.getOnPremisesSaasSubscription();
        assertErrorResponse(result);
    }

    @Test
    public void testLoginToSaas() throws Exception {
        Credentials testSaasUsernameAndPassword = Commons.createDtoFromJson(TEST_CREDENTIALS_JSON, Credentials.class);
        Request testRequest = new Request().setSaasUserCredentials(new SaasUserCredentials(TEST_ACCESS_TOKEN));

        doReturn(new DtoServerImpls.TokenImpl().withValue(TEST_ACCESS_TOKEN)).when(mockFacade).loginToCodenvySaaS(testSaasUsernameAndPassword);
        doReturn(new org.eclipse.che.api.account.server.dto.DtoServerImpls.AccountReferenceImpl().withId(TEST_ACCOUNT_ID).withName(TEST_ACCOUNT_NAME))
                .when(mockFacade).getAccountWhereUserIsOwner(null, testRequest);

        Response result = service.loginToCodenvySaaS(testSaasUsernameAndPassword);
        assertEquals(result.getStatus(), Response.Status.OK.getStatusCode());

        assertNotNull(service.saasUserCredentials);

        SaasUserCredentials testSaasSaasUserCredentials = service.saasUserCredentials;
        assertEquals(testSaasSaasUserCredentials.getAccountId(), TEST_ACCOUNT_ID);
        assertEquals(testSaasSaasUserCredentials.getToken(), TEST_ACCESS_TOKEN);
    }

    @Test
    public void testLoginToSaasWhenHttpException() throws Exception {
        Credentials testSaasUsernameAndPassword = Commons.createDtoFromJson(TEST_CREDENTIALS_JSON, Credentials.class);
        Request testRequest = new Request().setSaasUserCredentials(new SaasUserCredentials(TEST_ACCESS_TOKEN));

        doThrow(new AuthenticationException("error")).when(mockFacade).loginToCodenvySaaS(testSaasUsernameAndPassword);
        Response result = service.loginToCodenvySaaS(testSaasUsernameAndPassword);
        assertEquals(result.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());

        doReturn(new DtoServerImpls.TokenImpl().withValue(TEST_ACCESS_TOKEN)).when(mockFacade).loginToCodenvySaaS(testSaasUsernameAndPassword);
        doThrow(new HttpException(500, "Login error")).when(mockFacade).getAccountWhereUserIsOwner(null, testRequest);
        result = service.loginToCodenvySaaS(testSaasUsernameAndPassword);
        assertEquals(result.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }


    @Test
    public void testLoginToSaasWhenNullAccountReference() throws Exception {
        Credentials testSaasUsernameAndPassword = Commons.createDtoFromJson(TEST_CREDENTIALS_JSON, Credentials.class);
        Request testRequest = new Request().setSaasUserCredentials(new SaasUserCredentials(TEST_ACCESS_TOKEN));

        doReturn(new DtoServerImpls.TokenImpl().withValue(TEST_ACCESS_TOKEN)).when(mockFacade).loginToCodenvySaaS(testSaasUsernameAndPassword);
        doReturn(null).when(mockFacade).getAccountWhereUserIsOwner(null, testRequest);

        Response response = service.loginToCodenvySaaS(testSaasUsernameAndPassword);
        assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testGetNodeConfigForMultiNode() throws IOException {
        Config testConfig = new Config(new LinkedHashMap<>(ImmutableMap.of(
                "builder_host_name", "builder1.dev.com",
                "additional_runners", "http://runner1.dev.com:8080/runner/internal/runner,http://runner2.dev.com:8080/runner/internal/runner",
                "analytics_host_name", "analytics.dev.com",
                "additional_builders", "",
                "host_url", "local.dev.com"
                                                                          )));
        doReturn(testConfig).when(configManager).loadInstalledCodenvyConfig(InstallType.MULTI_SERVER);
        doReturn(InstallType.MULTI_SERVER).when(configManager).detectInstallationType();

        Response result = service.getNodesList();
        assertEquals(result.getStatus(), Response.Status.OK.getStatusCode());
        assertEquals(result.getEntity(), "{\n"
                                         + "  \"builder_host_name\" : \"builder1.dev.com\",\n"
                                         + "  \"additional_runners\" : [ \"runner1.dev.com\", \"runner2.dev.com\" ],\n"
                                         + "  \"analytics_host_name\" : \"analytics.dev.com\",\n"
                                         + "  \"additional_builders\" : [ ],\n"
                                         + "  \"host_url\" : \"local.dev.com\"\n"
                                         + "}");
    }

    @Test
    public void testGetNodeConfigWhenPropertiesIsAbsence() throws IOException {
        Config testConfig = new Config(new LinkedHashMap<String, String>());
        doReturn(testConfig).when(configManager).loadInstalledCodenvyConfig(InstallType.MULTI_SERVER);
        doReturn(InstallType.MULTI_SERVER).when(configManager).detectInstallationType();

        Response result = service.getNodesList();
        assertEquals(result.getStatus(), Response.Status.OK.getStatusCode());
        assertEquals(result.getEntity(), "{ }");
    }

    @Test
    public void testGetNodeConfigWhenSingleNode() throws IOException {
        Config config = mock(Config.class);
        doReturn("local").when(config).getHostUrl();
        doReturn(null).when(config).getAllValues("additional_builders");
        doReturn(null).when(config).getAllValues("additional_runners");

        doReturn(InstallType.SINGLE_SERVER).when(configManager).detectInstallationType();
        doReturn(config).when(configManager).loadInstalledCodenvyConfig(InstallType.SINGLE_SERVER);

        Response result = service.getNodesList();
        assertEquals(result.getStatus(), Response.Status.OK.getStatusCode());
        assertEquals(result.getEntity(), "{\n" +
                                         "  \"host_url\" : \"local\"\n" +
                                         "}");
    }

    @Test
    public void testGetNodeConfigError() throws IOException {
        doThrow(new RuntimeException("error")).when(configManager).detectInstallationType();

        Response result = service.getNodesList();
        assertEquals(result.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertEquals(result.getEntity().toString(), "{message=error}");
    }

    @Test
    public void testGetArtifactProperties() throws Exception {
        doReturn(new HashMap() {{
            put(ArtifactProperties.BUILD_TIME_PROPERTY, "2014-09-23 09:49:06");
            put(ArtifactProperties.ARTIFACT_PROPERTY, "codenvy");
            put(ArtifactProperties.SIZE_PROPERTY, "796346268");
            put(ArtifactProperties.VERSION_PROPERTY, "1.0.0");
        }}).when(mockArtifact).getProperties(any(Version.class));

        Response response = service.getArtifactProperties("codenvy", "1.0.1");
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        assertEquals(response.getEntity().toString(), "{build-time=2014-09-23T09:49:06.0Z, artifact=codenvy, version=1.0.0, size=796346268}");
    }

    @Test
    public void testGetArtifactPropertiesErrorIfArtifactNotFound() throws Exception {
        doThrow(new ArtifactNotFoundException("artifact")).when(service).getArtifact(anyString());
        Response response = service.getArtifactProperties("artifact", "1.3.1");
        assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testGetArtifactPropertiesErrorIfVersionInvalid() throws Exception {
        Response response = service.getArtifactProperties("codenvy", "version");
        assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testGetPropertiesShouldReturnOkResponse() throws Exception {
        doReturn(Collections.emptyMap()).when(mockFacade).loadProperties();
        Response response = service.getArtifactProperties("codenvy", "3.1.0");
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
    }

    @Test
    public void testGetPropertiesShouldReturnErrorResponse() throws Exception {
        doThrow(new IOException("error")).when(mockFacade).loadProperties();

        Response response = service.getProperties();

        assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testInsertPropertiesShouldReturnOkResponse() throws Exception {
        doNothing().when(mockFacade).storeProperties(anyMap());

        Response response = service.insertProperties(Collections.<String, String>emptyMap());

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
    }

    @Test
    public void testInsertPropertiesShouldReturnErrorResponse() throws Exception {
        doThrow(new IOException("error")).when(mockFacade).storeProperties(anyMap());

        Response response = service.insertProperties(Collections.<String, String>emptyMap());

        assertErrorResponse(response);
    }

    @Test
    public void testGetPropertyShouldReturnOkResponse() throws Exception {
        String key = "x";
        String value = "y";
        doReturn(value).when(mockFacade).loadProperty(key);

        Response response = service.getProperty(key);
        assertOkResponse(response);
        assertEquals(response.getEntity(), value);

    }

    @Test
    public void testGetNonExistedProperty() throws Exception {
        String key = "x";
        doThrow(PropertyNotFoundException.from(key)).when(mockFacade).loadProperty(key);

        Response response = service.getProperty(key);
        assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
        assertEquals(response.getEntity().toString(), "{message=Property 'x' not found}");
    }

    @Test
    public void testGetPropertyShouldReturnErrorResponse() throws Exception {
        String key = "x";
        doThrow(new IOException("error")).when(mockFacade).loadProperty(key);

        Response response = service.getProperty(key);
        assertErrorResponse(response);
    }

    @Test
    public void testUpdateProperty() throws Exception {
        String key = "x";
        String value = "y";

        Response response = service.updateProperty(key, value);
        assertOkResponse(response);

        verify(mockFacade).storeProperty(key, value);
    }

    @Test
    public void testUpdateNonExistedProperty() throws Exception {
        String key = "x";
        String value = "y";
        doThrow(PropertyNotFoundException.from(key)).when(mockFacade).storeProperty(key, value);

        Response response = service.updateProperty(key, value);
        assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
        assertEquals(response.getEntity().toString(), "{message=Property 'x' not found}");
    }

    @Test
    public void testUpdatePropertyShouldReturnErrorResponse() throws Exception {
        String key = "x";
        String value = "y";
        doThrow(new IOException("error")).when(mockFacade).storeProperty(key, value);

        Response response = service.updateProperty(key, value);
        assertErrorResponse(response);
    }

    @Test
    public void testDeleteProperty() throws Exception {
        String key = "x";

        Response response = service.deleteProperty(key);
        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        verify(mockFacade).deleteProperty(key);
    }

    @Test
    public void testDeleteNonExistedProperty() throws Exception {
        String key = "x";
        doThrow(PropertyNotFoundException.from(key)).when(mockFacade).deleteProperty(key);

        Response response = service.deleteProperty(key);
        assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
        assertEquals(response.getEntity().toString(), "{message=Property 'x' not found}");
    }

    @Test
    public void testDeletePropertyShouldReturnErrorResponse() throws Exception {
        String key = "x";
        doThrow(new IOException("error")).when(mockFacade).deleteProperty(key);

        Response response = service.deleteProperty(key);
        assertErrorResponse(response);
    }

    private void assertOkResponse(Response result) throws IOException {
        assertEquals(result.getStatus(), Response.Status.OK.getStatusCode());
    }

    private void assertErrorResponse(Response result) {
        assertEquals(result.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }
}

