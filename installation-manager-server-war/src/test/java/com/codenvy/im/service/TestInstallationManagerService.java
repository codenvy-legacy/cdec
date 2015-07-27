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

import com.codenvy.api.subscription.shared.dto.SubscriptionDescriptor;
import com.codenvy.im.BaseTest;
import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.ArtifactNotFoundException;
import com.codenvy.im.artifacts.ArtifactProperties;
import com.codenvy.im.facade.IMCliFilteredFacade;
import com.codenvy.im.managers.BackupConfig;
import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.DownloadAlreadyStartedException;
import com.codenvy.im.managers.DownloadNotStartedException;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.managers.PropertiesNotFoundException;
import com.codenvy.im.managers.PropertyNotFoundException;
import com.codenvy.im.response.BackupInfo;
import com.codenvy.im.response.DownloadProgressResponse;
import com.codenvy.im.response.InstallArtifactInfo;
import com.codenvy.im.response.InstallArtifactStatus;
import com.codenvy.im.response.NodeInfo;
import com.codenvy.im.saas.SaasAccountServiceProxy;
import com.codenvy.im.saas.SaasUserCredentials;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.HttpException;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.eclipse.che.api.auth.AuthenticationException;
import org.eclipse.che.api.auth.server.dto.DtoServerImpls;
import org.eclipse.che.api.auth.shared.dto.Credentials;
import org.eclipse.che.dto.server.DtoFactory;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;
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
    private IMCliFilteredFacade mockFacade;
    @Mock
    private ConfigManager       configManager;
    @Mock
    private Principal           mockPrincipal;
    @Mock
    private Config              mockConfig;
    @Mock
    private Artifact            mockArtifact;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        service = spy(new InstallationManagerService(BACKUP_DIR, mockFacade, configManager));

        doReturn(TEST_SYSTEM_ADMIN_NAME).when(mockPrincipal).getName();
        doReturn(mockArtifact).when(service).createArtifact(anyString());

        artifact = createArtifact(ARTIFACT_NAME);
        version = Version.valueOf(VERSION_NUMBER);
    }

    @Test
    public void testStartDownload() throws Exception {
        Response result = service.startDownload(ARTIFACT_NAME, VERSION_NUMBER);
        assertEquals(result.getStatus(), Response.Status.ACCEPTED.getStatusCode());
        assertTrue(result.getEntity() instanceof DownloadToken);
        assertNotNull(((DownloadToken)result.getEntity()).getId());
        verify(mockFacade).startDownload(createArtifact("codenvy"), version);
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
        DownloadProgressResponse progressDescriptor = new DownloadProgressResponse();
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
    public void testGetUpdatesShouldReturnOkStatus() throws Exception {
        doReturn(Collections.emptyList()).when(mockFacade).getAllUpdates(any(Artifact.class));

        Response result = service.getUpdates();
        assertEquals(result.getStatus(), Response.Status.OK.getStatusCode());
    }

    @Test
    public void testGetUpdatesShouldReturnErrorStatus() throws Exception {
        doThrow(new IOException("error")).when(mockFacade).getAllUpdates(any(Artifact.class));

        Response result = service.getUpdates();
        assertEquals(result.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testGetInstalledVersionsShouldReturnOkStatus() throws Exception {
        doReturn(ImmutableList.of(new InstallArtifactInfo().withVersion("1.0.1")
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
        doReturn(ImmutableList.of("a")).when(mockFacade).getUpdateInfo(any(Artifact.class), any(InstallType.class));
        doReturn("id").when(mockFacade).update(any(Artifact.class), any(Version.class), any(InstallOptions.class));
        doReturn(Version.valueOf("3.1.0")).when(mockFacade).getLatestInstallableVersion(any(Artifact.class));

        Map<String, String> testConfigProperties = new HashMap<>();
        testConfigProperties.put("property1", "value1");
        testConfigProperties.put("property2", "value2");

        doReturn(testConfigProperties).when(configManager).prepareInstallProperties(null,
                                                                                    null,
                                                                                    InstallType.SINGLE_SERVER,
                                                                                    createArtifact(ARTIFACT_NAME),
                                                                                    Version.valueOf("3.1.0"),
                                                                                    false);

        Response result = service.updateCodenvy(0);
        assertEquals(result.getStatus(), Response.Status.ACCEPTED.getStatusCode());

        result = service.updateCodenvy(-1);
        assertEquals(result.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());

        result = service.updateCodenvy(1);
        assertEquals(result.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testGetConfig() throws Exception {
        doReturn(Collections.emptyMap()).when(mockFacade).getInstallationManagerProperties();

        Response response = service.getInstallationManagerServerConfig();
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
    }

    @Test
    public void testAddNodeShouldReturnOkResponse() throws Exception {
        doReturn(new NodeInfo()).when(mockFacade).addNode("dns");

        Response result = service.addNode("dns");
        assertEquals(result.getStatus(), Response.Status.CREATED.getStatusCode());
    }

    @Test
    public void testRemoveNodeShouldReturnOkResponse() throws Exception {
        doReturn(new NodeInfo()).when(mockFacade).removeNode("dns");

        Response result = service.removeNode("dns");
        assertEquals(result.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    public void testBackupShouldReturnOkResponse() throws Exception {
        doReturn(new BackupInfo()).when(mockFacade).backup(any(BackupConfig.class));

        Response result = service.backup(ARTIFACT_NAME);
        assertEquals(result.getStatus(), Response.Status.CREATED.getStatusCode());
    }

    @Test
    public void testRestoreShouldReturnOkResponse() throws Exception {
        doReturn(new BackupInfo()).when(mockFacade).restore(any(BackupConfig.class));

        Response result = service.restore(ARTIFACT_NAME, "");
        assertEquals(result.getStatus(), Response.Status.CREATED.getStatusCode());
    }

    @Test
    public void testAddTrialSubscriptionShouldReturnOkResponse() throws Exception {
        SaasUserCredentials testUserCredentials = new SaasUserCredentials(TEST_ACCESS_TOKEN, TEST_ACCOUNT_ID);
        service.saasUserCredentials = testUserCredentials;
        doNothing().when(mockFacade).addTrialSaasSubscription(testUserCredentials);

        Response result = service.addTrialSubscription();
        assertEquals(result.getStatus(), Response.Status.CREATED.getStatusCode());
    }

    @Test
    public void testAddTrialSubscriptionShouldReturnForbiddenResponse() throws Exception {
        SaasUserCredentials testUserCredentials = new SaasUserCredentials(TEST_ACCESS_TOKEN, TEST_ACCOUNT_ID);
        doNothing().when(mockFacade).addTrialSaasSubscription(testUserCredentials);

        Response result = service.addTrialSubscription();
        assertEquals(result.getStatus(), Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    public void testGetSubscription() throws Exception {
        SaasUserCredentials testUserCredentials = new SaasUserCredentials(TEST_ACCOUNT_ID, TEST_ACCESS_TOKEN);

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
        doReturn(descriptor).when(mockFacade).getSaasSubscription(SaasAccountServiceProxy.ON_PREMISES, testUserCredentials);

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

        doThrow(new HttpException(500, "error")).when(mockFacade).getSaasSubscription(SaasAccountServiceProxy.ON_PREMISES, testUserCredentials);
        result = service.getOnPremisesSaasSubscription();
        assertErrorResponse(result);
    }

    @Test
    public void testLoginToSaas() throws Exception {
        Credentials testSaasUsernameAndPassword = Commons.createDtoFromJson(TEST_CREDENTIALS_JSON, Credentials.class);

        doReturn(new DtoServerImpls.TokenImpl().withValue(TEST_ACCESS_TOKEN)).when(mockFacade).loginToCodenvySaaS(testSaasUsernameAndPassword);
        doReturn(new org.eclipse.che.api.account.server.dto.DtoServerImpls.AccountReferenceImpl().withId(TEST_ACCOUNT_ID).withName(TEST_ACCOUNT_NAME))
                .when(mockFacade).getAccountWhereUserIsOwner(null, TEST_ACCESS_TOKEN);

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

        doThrow(new AuthenticationException("error")).when(mockFacade).loginToCodenvySaaS(testSaasUsernameAndPassword);
        Response result = service.loginToCodenvySaaS(testSaasUsernameAndPassword);
        assertEquals(result.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());

        doReturn(new DtoServerImpls.TokenImpl().withValue(TEST_ACCESS_TOKEN)).when(mockFacade).loginToCodenvySaaS(testSaasUsernameAndPassword);
        doThrow(new HttpException(500, "Login error")).when(mockFacade).getAccountWhereUserIsOwner(null, TEST_ACCESS_TOKEN);
        result = service.loginToCodenvySaaS(testSaasUsernameAndPassword);
        assertEquals(result.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }


    @Test
    public void testLoginToSaasWhenNullAccountReference() throws Exception {
        Credentials testSaasUsernameAndPassword = Commons.createDtoFromJson(TEST_CREDENTIALS_JSON, Credentials.class);

        doReturn(new DtoServerImpls.TokenImpl().withValue(TEST_ACCESS_TOKEN)).when(mockFacade).loginToCodenvySaaS(testSaasUsernameAndPassword);
        doReturn(null).when(mockFacade).getAccountWhereUserIsOwner(null, TEST_ACCESS_TOKEN);

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
                                         + "  \"host_url\" : \"local.dev.com\",\n"
                                         + "  \"builder_host_name\" : \"builder1.dev.com\",\n"
                                         + "  \"additional_builders\" : [ ],\n"
                                         + "  \"analytics_host_name\" : \"analytics.dev.com\",\n"
                                         + "  \"additional_runners\" : [ \"runner1.dev.com\", \"runner2.dev.com\" ]\n"
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
        doReturn(new LinkedHashMap() {{
            put(ArtifactProperties.BUILD_TIME_PROPERTY, "2014-09-23 09:49:06");
            put(ArtifactProperties.ARTIFACT_PROPERTY, "codenvy");
            put(ArtifactProperties.SIZE_PROPERTY, "796346268");
            put(ArtifactProperties.VERSION_PROPERTY, "1.0.0");
        }}).when(mockArtifact).getProperties(any(Version.class));

        Response response = service.getArtifactProperties("codenvy", "1.0.1");
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        assertEquals(response.getEntity().toString(), "{build-time=2014-09-23T09:49:06.0Z, artifact=codenvy, size=796346268, version=1.0.0}");
    }

    @Test
    public void testGetArtifactPropertiesErrorIfArtifactNotFound() throws Exception {
        doThrow(new ArtifactNotFoundException("artifact")).when(service).createArtifact(anyString());
        Response response = service.getArtifactProperties("artifact", "1.3.1");
        assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testGetArtifactPropertiesErrorIfVersionInvalid() throws Exception {
        Response response = service.getArtifactProperties("codenvy", "version");
        assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testGetStoragePropertiesShouldReturnOkResponse() throws Exception {
        doReturn(Collections.emptyMap()).when(mockFacade).loadStorageProperties();

        Response response = service.getArtifactProperties("codenvy", "3.1.0");
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
    }

    @Test
    public void testGetStoragePropertiesShouldReturnErrorResponse() throws Exception {
        doThrow(new IOException("error")).when(mockFacade).loadStorageProperties();

        Response response = service.getStorageProperties();
        assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testInsertStoragePropertiesShouldReturnOkResponse() throws Exception {
        doNothing().when(mockFacade).storeStorageProperties(anyMap());

        Response response = service.insertStorageProperties(Collections.<String, String>emptyMap());
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
    }

    @Test
    public void testInsertStoragePropertiesShouldReturnErrorResponse() throws Exception {
        doThrow(new IOException("error")).when(mockFacade).storeStorageProperties(anyMap());

        Response response = service.insertStorageProperties(Collections.<String, String>emptyMap());
        assertErrorResponse(response);
    }

    @Test
    public void testGetStoragePropertyShouldReturnOkResponse() throws Exception {
        String key = "x";
        String value = "y";
        doReturn(value).when(mockFacade).loadStorageProperty(key);

        Response response = service.getStorageProperty(key);
        assertOkResponse(response);
        assertEquals(response.getEntity(), value);
    }

    @Test
    public void testGetNonExistedStorageProperty() throws Exception {
        String key = "x";
        doThrow(PropertyNotFoundException.from(key)).when(mockFacade).loadStorageProperty(key);

        Response response = service.getStorageProperty(key);
        assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
        assertEquals(response.getEntity().toString(), "{message=Property 'x' not found}");
    }

    @Test
    public void testGetStoragePropertyShouldReturnErrorResponse() throws Exception {
        String key = "x";
        doThrow(new IOException("error")).when(mockFacade).loadStorageProperty(key);

        Response response = service.getStorageProperty(key);
        assertErrorResponse(response);
    }

    @Test
    public void testUpdateStorageProperty() throws Exception {
        String key = "x";
        String value = "y";

        Response response = service.updateStorageProperty(key, value);
        assertOkResponse(response);
        verify(mockFacade).storeStorageProperty(key, value);
    }

    @Test
    public void testUpdateNonExistedStorageProperty() throws Exception {
        String key = "x";
        String value = "y";
        doThrow(PropertyNotFoundException.from(key)).when(mockFacade).storeStorageProperty(key, value);

        Response response = service.updateStorageProperty(key, value);
        assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
        assertEquals(response.getEntity().toString(), "{message=Property 'x' not found}");
    }

    @Test
    public void testUpdateStoragePropertyShouldReturnErrorResponse() throws Exception {
        String key = "x";
        String value = "y";
        doThrow(new IOException("error")).when(mockFacade).storeStorageProperty(key, value);

        Response response = service.updateStorageProperty(key, value);
        assertErrorResponse(response);
    }

    @Test
    public void testDeleteStorageProperty() throws Exception {
        String key = "x";

        Response response = service.deleteStorageProperty(key);
        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        verify(mockFacade).deleteStorageProperty(key);
    }

    @Test
    public void testDeleteNonExistedStorageProperty() throws Exception {
        String key = "x";
        doThrow(PropertyNotFoundException.from(key)).when(mockFacade).deleteStorageProperty(key);

        Response response = service.deleteStorageProperty(key);
        assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
        assertEquals(response.getEntity().toString(), "{message=Property 'x' not found}");
    }

    @Test
    public void testDeleteStoragePropertyShouldReturnErrorResponse() throws Exception {
        String key = "x";
        doThrow(new IOException("error")).when(mockFacade).deleteStorageProperty(key);

        Response response = service.deleteStorageProperty(key);
        assertErrorResponse(response);
    }

    @Test
    public void testGetCodenvyPropertiesShouldReturnOkResponse() throws Exception {
        Config testConfig = new Config(ImmutableMap.of("a", "b", "password", "c"));
        doReturn(testConfig).when(configManager).loadInstalledCodenvyConfig();

        Response response = service.getCodenvyProperties();
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        assertEquals(response.getEntity().toString(), "{a=b, password=*****}");
    }

    @Test
    public void testGetCodenvyPropertiesShouldReturnErrorResponse() throws Exception {
        doThrow(new IOException("error")).when(configManager).loadInstalledCodenvyConfig();

        Response response = service.getCodenvyProperties();
        assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testGetCodenvyPropertyShouldReturnOkResponse() throws Exception {
        Config config = new Config(ImmutableMap.of("x", "y"));
        doReturn(config).when(configManager).loadInstalledCodenvyConfig();

        Response response = service.getCodenvyProperty("x");
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        assertEquals(response.getEntity().toString(), "{x=y}");
    }

    @Test
    public void testGetNonExistedCodenvyProperty() throws Exception {
        String key = "x";
        Config testConfig = new Config(ImmutableMap.of("y", "a"));
        doReturn(testConfig).when(configManager).loadInstalledCodenvyConfig();

        Response response = service.getCodenvyProperty(key);
        assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
        assertEquals(response.getEntity().toString(), "{message=Property 'x' not found}");
    }

    @Test
    public void testGetCodenvyPropertyShouldReturnErrorResponse() throws Exception {
        doThrow(new IOException("error")).when(configManager).loadInstalledCodenvyConfig();

        Response response = service.getCodenvyProperty("x");
        assertErrorResponse(response);
    }


    @Test(dataProvider = "privateCodenvyPropertyMaskTest")
    public void testPrivateCodenvyPropertyMask(String key, boolean masked) throws IOException {
        String value = "y";
        Config testConfig = new Config(ImmutableMap.of(key, value));
        doReturn(testConfig).when(configManager).loadInstalledCodenvyConfig();

        Response response = service.getCodenvyProperty(key);
        Map returnValue = (Map)response.getEntity();
        assertEquals(returnValue.get(key), masked ? "*****" : value);
    }

    @DataProvider(name = "privateCodenvyPropertyMaskTest")
    public Object[][] getTestPrivateCodenvyPropertyMaskData() {
        return new Object[][] {
            {"test_password", true},
            {"test_password_field_name", false},
            {"test_secret", true},
            {"test_secret_field_name", false},
            {"test_pass", true},
            {"test_passenger", false},
        };
    }

    @Test
    public void testUpdateCodenvyProperty() throws Exception {
        Map<String, String> properties = ImmutableMap.of("x", "y");

        Response response = service.updateCodenvyProperties(properties);
        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
        verify(mockFacade).updateArtifactConfig(any(Artifact.class), anyMap());
    }

    @Test
    public void testUpdateCodenvyPropertyShouldReturnErrorResponse() throws Exception {
        Map<String, String> properties = ImmutableMap.of("x", "y");
        doThrow(new IOException("error")).when(mockFacade).updateArtifactConfig(any(Artifact.class), anyMap());

        Response response = service.updateCodenvyProperties(properties);
        assertErrorResponse(response);
    }

    @Test
    public void testUpdateNonexistentCodenvyProperty() throws Exception {
        List<String> nonexistentProperties = Arrays.asList("x1", "x2");
        doThrow(new PropertiesNotFoundException(nonexistentProperties)).when(mockFacade).updateArtifactConfig(any(Artifact.class), anyMap());

        Response response = service.updateCodenvyProperties(null);
        assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
        assertEquals(response.getEntity().toString(), "{message=Properties not found, properties=[x1, x2]}");
    }

    @Test
    public void testGetDownloadsShouldReturnOkResponse() throws Exception {
        doReturn("id").when(mockFacade).getDownloadIdInProgress();

        Response response = service.getDownloads();
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
    }

    @Test
    public void testGetDownloadsShouldReturnConflictResponse() throws Exception {
        doThrow(new DownloadNotStartedException()).when(mockFacade).getDownloadIdInProgress();

        Response result = service.getDownloads();
        assertEquals(result.getStatus(), Response.Status.CONFLICT.getStatusCode());
    }

    @Test
    public void testDeleteDownloadedArtifact() throws Exception {
        doReturn(artifact).when(service).createArtifact(artifact.getName());

        Response response = service.deleteDownloadedArtifact(artifact.getName(), version.toString());
        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        verify(mockFacade).deleteDownloadedArtifact(artifact, version);
    }

    @Test
    public void testDeleteNonDownloadedArtifact() throws Exception {
        doReturn(artifact).when(service).createArtifact(artifact.getName());
        doThrow(new ArtifactNotFoundException(artifact, version)).when(mockFacade).deleteDownloadedArtifact(artifact, version);

        Response response = service.deleteDownloadedArtifact(artifact.getName(), version.toString());
        assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
        assertEquals(response.getEntity().toString(), "{message=Artifact codenvy:1.0.0 not found}");
    }

    @Test
    public void testDeleteDownloadedArtifactWhenVersionIsIncorrect() throws Exception {
        doReturn(artifact).when(service).createArtifact(artifact.getName());

        Response response = service.deleteDownloadedArtifact(artifact.getName(), "incorrect");
        assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
        assertEquals(response.getEntity().toString(), "{message=Illegal version 'incorrect'}");
    }

    @Test
    public void testDeleteDownloadedArtifactShouldReturnErrorResponse() throws Exception {
        doReturn(artifact).when(service).createArtifact(artifact.getName());
        doThrow(new IOException("error")).when(mockFacade).deleteDownloadedArtifact(artifact, version);

        Response response = service.deleteDownloadedArtifact(artifact.getName(), version.toString());
        assertErrorResponse(response);
    }

    @Test
    public void testGetUpdateInfoShouldReturnOkResponse() throws Exception {
        doReturn(artifact).when(service).createArtifact(artifact.getName());
        doReturn(InstallType.SINGLE_SERVER).when(configManager).detectInstallationType();
        doReturn(ImmutableList.of("a", "b")).when(mockFacade).getInstallInfo(artifact, InstallType.SINGLE_SERVER);

        Response response = service.getUpdateInfo();
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
    }

    @Test
    public void testGetUpdateInfoShouldReturnErrorResponse() throws Exception {
        doReturn(artifact).when(service).createArtifact(artifact.getName());
        doReturn(InstallType.SINGLE_SERVER).when(configManager).detectInstallationType();
        doThrow(new IOException("error")).when(mockFacade).getUpdateInfo(artifact, InstallType.SINGLE_SERVER);

        Response response = service.getUpdateInfo();
        assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    private void assertOkResponse(Response result) throws IOException {
        assertEquals(result.getStatus(), Response.Status.OK.getStatusCode());
    }

    private void assertErrorResponse(Response result) {
        assertEquals(result.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }
}

