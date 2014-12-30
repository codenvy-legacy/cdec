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
package com.codenvy.im.update;

import com.codenvy.commons.user.UserImpl;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.utils.AccountUtils;
import com.codenvy.im.utils.AccountUtils.SubscriptionInfo;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.HttpTransport;
import com.jayway.restassured.response.Response;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.everrest.assured.EverrestJetty;
import org.everrest.assured.JettyHttpServer;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.codenvy.im.artifacts.ArtifactProperties.ARTIFACT_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.AUTHENTICATION_REQUIRED_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.BUILD_TIME_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.FILE_NAME_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.MD5_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.SUBSCRIPTION_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.VERSION_PROPERTY;
import static com.codenvy.im.utils.AccountUtils.ON_PREMISES;
import static com.codenvy.im.utils.AccountUtils.SUBSCRIPTION_DATE_FORMAT;
import static com.jayway.restassured.RestAssured.given;
import static java.util.Calendar.getInstance;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Anatoliy Bazko
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class TestRepositoryService extends BaseTest {

    private ArtifactStorage   artifactStorage;
    private RepositoryService repositoryService;
    private HttpTransport     transport;
    private UserManager       userManager;
    private MongoStorage      mongoStorage;

    private final Properties authenticationRequiredProperties = new Properties() {{
        put(AUTHENTICATION_REQUIRED_PROPERTY, "true");
    }};
    private final Properties subscriptionProperties           = new Properties() {{
        put(SUBSCRIPTION_PROPERTY, "OnPremises");
    }};

    @Override
    @BeforeMethod
    public void setUp() throws Exception {
        mongoStorage = spy(new MongoStorage("mongodb://localhost:12000/update", true, "target"));
        transport = mock(HttpTransport.class);
        userManager = mock(UserManager.class);
        artifactStorage = new ArtifactStorage(DOWNLOAD_DIRECTORY.toString());
        repositoryService = new RepositoryService("",
                                                  "",
                                                  "",
                                                  userManager,
                                                  artifactStorage,
                                                  mongoStorage,
                                                  transport);

        when(userManager.getCurrentUser()).thenReturn(new UserImpl("name", "id", "token", Collections.<String>emptyList(), false));
        initStorage();
        super.setUp();
    }

    private void initStorage() {
        DBCollection collection = mongoStorage.getDb().getCollection(MongoStorage.DOWNLOAD_STATISTICS);
        collection.remove(new BasicDBObject());

        mongoStorage.updateDownloadStatistics("uid1", "cdec", "1.0.1", true);
        mongoStorage.updateDownloadStatistics("uid1", "cdec", "1.0.1", true);
        mongoStorage.updateDownloadStatistics("uid1", "cdec", "1.0.1", false);
        mongoStorage.updateDownloadStatistics("uid1", "cdec", "1.0.1", false);
        mongoStorage.updateDownloadStatistics("uid1", "artifact2", "1.0.1", false);

        mongoStorage.updateDownloadStatistics("uid2", "cdec", "1.0.1", true);
        mongoStorage.updateDownloadStatistics("uid2", "cdec", "1.0.2", true);
        mongoStorage.updateDownloadStatistics("uid2", "cdec", "1.0.3", true);
        mongoStorage.updateDownloadStatistics("uid2", "artifact3", "1.0.1", false);

        collection = mongoStorage.getDb().getCollection(MongoStorage.SUBSCRIPTIONS);
        collection.remove(new BasicDBObject());
    }

    @Test
    public void testGetLatestVersion() throws Exception {
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), InstallManagerArtifact.NAME, "1.0.1", "tmp", new Properties());
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), InstallManagerArtifact.NAME, "1.0.2", "tmp", new Properties());

        Response response = given().when().get("repository/properties/" + InstallManagerArtifact.NAME);
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        Map value = Commons.asMap(response.body().asString());

        assertNotNull(value);
        assertEquals(value.size(), 3);
        assertEquals(value.get(ARTIFACT_PROPERTY), InstallManagerArtifact.NAME);
        assertEquals(value.get(VERSION_PROPERTY), "1.0.2");
        assertNull(value.get(MD5_PROPERTY));
    }

    @Test
    public void testGetArtifactProperties() throws Exception {
        Map testProperties = new HashMap<String, String>() {{
            put(AUTHENTICATION_REQUIRED_PROPERTY, "true");
            put(SUBSCRIPTION_PROPERTY, "OnPremises");
        }};

        Properties testPropertiesContainer = new Properties();
        testPropertiesContainer.putAll(testProperties);

        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), InstallManagerArtifact.NAME, "1.0.1", "tmp", testPropertiesContainer);

        Response response = given().when().get("repository/properties/" + InstallManagerArtifact.NAME + "/1.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        Map value = Commons.asMap(response.body().asString());

        assertNotNull(value);
        assertEquals(value.size(), 4);
        assertEquals(value.get(ARTIFACT_PROPERTY), InstallManagerArtifact.NAME);
        assertEquals(value.get(VERSION_PROPERTY), "1.0.1");
        assertEquals(value.get(AUTHENTICATION_REQUIRED_PROPERTY), "true");
        assertNull(value.get(MD5_PROPERTY));
    }

    @Test
    public void testDownloadPublicArtifact() throws Exception {
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), InstallManagerArtifact.NAME, "1.0.1", "tmp", new Properties());

        Response response = given().when().get("repository/public/download/" + InstallManagerArtifact.NAME + "/1.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
        assertEquals(IOUtils.toString(response.body().asInputStream()), "content");
    }

    @Test
    public void testDownloadPublicErrorWhenArtifactAbsent() throws Exception {
        Response response = given().when().get("repository/public/download/installation-manager/1.0.2");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testDownloadPublicArtifactLatestVersion() throws Exception {
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), InstallManagerArtifact.NAME, "1.0.1", "tmp", new Properties());

        Response response = given().when().get("repository/public/download/" + InstallManagerArtifact.NAME);
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
        assertEquals(IOUtils.toString(response.body().asInputStream()), "content");
    }

    @Test
    public void testDownloadPublicWithSubscription() throws Exception {
        SimpleDateFormat  subscriptionDateFormat = new SimpleDateFormat(SUBSCRIPTION_DATE_FORMAT);
        Calendar cal = getInstance();
        cal.add(Calendar.DATE, -1);
        String startDate = subscriptionDateFormat.format(cal.getTime());

        cal = getInstance();
        cal.add(Calendar.DATE, 1);
        String endDate = subscriptionDateFormat.format(cal.getTime());

        when(transport.doGet("/account", userManager.getCurrentUser().getToken()))
        .thenReturn("[{roles:[\"account/owner\"],accountReference:{id:accountId}}]");

        when(transport.doGet("/account/accountId/subscriptions", userManager.getCurrentUser().getToken()))
        .thenReturn("[{serviceId:OnPremises,id:subscriptionId}]");

        when(transport.doGet("/account/subscriptions/subscriptionId/attributes", userManager.getCurrentUser().getToken()))
        .thenReturn("{startDate:\"" + startDate + "\",endDate:\"" + endDate + "\"}");

        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), "cdec", "1.0.1", "tmp", subscriptionProperties);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get(JettyHttpServer.SECURE_PATH + "/repository/download/cdec/1.0.1/accountId");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
        assertEquals(IOUtils.toString(response.body().asInputStream()), "content");
    }

    @Test
    public void testDownloadPublicArtifactErrorWhenSubscriptionRequired() throws Exception {
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), "cdec", "1.0.1", "tmp", subscriptionProperties);

        Response response = given().when().get("/repository/public/download/cdec/1.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    public void testDownloadPrivateArtifactWithoutSubscription() throws Exception {
        when(transport.doGet("/account")).thenReturn("[{accountReference:{id:accountId}}]");
        when(transport.doGet("/account/accountId/subscriptions")).thenReturn("[]");
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), "cdec", "1.0.1", "tmp", authenticationRequiredProperties);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get(JettyHttpServer.SECURE_PATH + "/repository/download/cdec/1.0.1/accountId");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
        assertEquals(IOUtils.toString(response.body().asInputStream()), "content");
    }

    @Test
    public void testDownloadArtifactWhenAuthenticationError() throws Exception {
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), "cdec", "1.0.1", "tmp", authenticationRequiredProperties);

        Response response = given().when().get("repository/public/download/cdec/1.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    public void testDownloadPrivateWhenUserWithoutSubscriptionError() throws Exception {
        when(transport.doGet("/account", userManager.getCurrentUser().getToken()))
                 .thenReturn("[{roles:[\"account/owner\"],accountReference:{id:accountId}}]");
        when(transport.doGet("/account/accountId/subscriptions", userManager.getCurrentUser().getToken()))
                 .thenReturn("[]");
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), "cdec", "1.0.1", "tmp", subscriptionProperties);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get(JettyHttpServer.SECURE_PATH + "/repository/download/cdec/1.0.1/accountId");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    public void testDownloadPrivateErrorIfNoRolesAllowed() throws Exception {
        Response response = given().when().get("repository/download/cdec/1.0.1/accountId");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    public void testUploadDownloadSnapshotVersion() throws Exception {
        Path tmp = Paths.get("target/tmp-1.0.1.txt");
        Files.copy(new ByteArrayInputStream("content".getBytes()), tmp, StandardCopyOption.REPLACE_EXISTING);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .multiPart(tmp.toFile()).post(JettyHttpServer.SECURE_PATH + "/repository/upload/cdec/1.0.1-SNAPSHOT");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get("/repository/public/download/cdec");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
        assertEquals(IOUtils.toString(response.body().asInputStream()), "content");
    }

    @Test
    public void testUploadSnapshotVersion() throws Exception {
        Path tmp = Paths.get("target/tmp-1.0.1.txt");
        Files.copy(new ByteArrayInputStream("content".getBytes()), tmp, StandardCopyOption.REPLACE_EXISTING);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .multiPart(tmp.toFile()).post(JettyHttpServer.SECURE_PATH + "/repository/upload/cdec/1.0.1-SNAPSHOT");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        Path artifact = Paths.get("target", "download", "cdec", "1.0.1-SNAPSHOT", "tmp-1.0.1.txt");
        assertEquals(FileUtils.readFileToString(artifact.toFile()), "content");
        assertTrue(Files.exists(artifact));

        Path propertiesFile = Paths.get("target", "download", "cdec", "1.0.1-SNAPSHOT", ArtifactStorage.PROPERTIES_FILE);
        assertTrue(Files.exists(propertiesFile));

        Properties properties = new Properties();
        properties.load(Files.newInputStream(propertiesFile));
        assertEquals(properties.size(), 3);
        assertEquals(properties.get(VERSION_PROPERTY), "1.0.1-SNAPSHOT");
        assertEquals(properties.get(FILE_NAME_PROPERTY), "tmp-1.0.1.txt");
        assertEquals(properties.get(ARTIFACT_PROPERTY), "cdec");
    }


    @Test
    public void testUpload() throws Exception {
        Path tmp = Paths.get("target/tmp-1.0.1.txt");
        Files.copy(new ByteArrayInputStream("content".getBytes()), tmp, StandardCopyOption.REPLACE_EXISTING);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .multiPart(tmp.toFile()).post(JettyHttpServer.SECURE_PATH + "/repository/upload/cdec/1.0.1?revision=abcd&build-time=20140930");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        Path artifact = Paths.get("target", "download", "cdec", "1.0.1", "tmp-1.0.1.txt");
        assertEquals(FileUtils.readFileToString(artifact.toFile()), "content");
        assertTrue(Files.exists(artifact));

        Path propertiesFile = Paths.get("target", "download", "cdec", "1.0.1", ArtifactStorage.PROPERTIES_FILE);
        assertTrue(Files.exists(propertiesFile));

        Properties properties = new Properties();
        properties.load(Files.newInputStream(propertiesFile));
        assertEquals(properties.size(), 4);
        assertEquals(properties.get(VERSION_PROPERTY), "1.0.1");
        assertEquals(properties.get(FILE_NAME_PROPERTY), "tmp-1.0.1.txt");
        assertEquals(properties.get(BUILD_TIME_PROPERTY), "20140930");
        assertEquals(properties.get(ARTIFACT_PROPERTY), "cdec");
    }

    @Test
    public void testUploadErrorIfVersionHasBadFormat() throws Exception {
        Path tmp = Paths.get("target/tmp");
        Files.copy(new ByteArrayInputStream("content".getBytes()), tmp, StandardCopyOption.REPLACE_EXISTING);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .multiPart(tmp.toFile()).post(JettyHttpServer.SECURE_PATH + "/repository/upload/cdec-1.01.1/1.01.1");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testUploadErrorIfNoStream() throws Exception {
        Files.copy(new ByteArrayInputStream("content".getBytes()), Paths.get("target/tmp"), StandardCopyOption.REPLACE_EXISTING);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .post(JettyHttpServer.SECURE_PATH + "/repository/upload/cdec-1.01.1/1.01.1");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testGetDownloadStatisticByUser() throws Exception {
        Response response = given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                                   .get(JettyHttpServer.SECURE_PATH + "/repository/download/statistic/uid1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        Map m = response.as(Map.class);
        assertEquals(m.size(), 5);
        assertEquals(m.get(MongoStorage.USER_ID), "uid1");
        assertEquals(m.get(MongoStorage.TOTAL), 5D);
        assertEquals(m.get(MongoStorage.SUCCESS), 2D);
        assertEquals(m.get(MongoStorage.FAIL), 3D);
        assertNotNull(m.get(MongoStorage.ARTIFACTS));

        List l = (List)m.get(MongoStorage.ARTIFACTS);
        assertEquals(l.size(), 2);

        m = (Map)l.get(0);
        assertEquals(m.get(MongoStorage.ARTIFACT), "artifact2");
        assertEquals(m.get(MongoStorage.VERSION), "1.0.1");
        assertEquals(m.get(MongoStorage.SUCCESS), 0D);
        assertEquals(m.get(MongoStorage.FAIL), 1D);
        assertEquals(m.get(MongoStorage.TOTAL), 1D);

        m = (Map)l.get(1);
        assertEquals(m.get(MongoStorage.ARTIFACT), "cdec");
        assertEquals(m.get(MongoStorage.VERSION), "1.0.1");
        assertEquals(m.get(MongoStorage.SUCCESS), 2D);
        assertEquals(m.get(MongoStorage.FAIL), 2D);
        assertEquals(m.get(MongoStorage.TOTAL), 4D);
    }

    @Test
    public void testGetDownloadStatisticByArtifact() throws Exception {
        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get(JettyHttpServer.SECURE_PATH + "/repository/download/statistic/cdec");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        Map m = response.as(Map.class);
        assertEquals(m.size(), 5);
        assertEquals(m.get(MongoStorage.ARTIFACT), "cdec");
        assertEquals(m.get(MongoStorage.SUCCESS), 5D);
        assertEquals(m.get(MongoStorage.FAIL), 2D);
        assertEquals(m.get(MongoStorage.TOTAL), 7D);
        assertNotNull(m.get(MongoStorage.VERSIONS));

        List l = (List)m.get(MongoStorage.VERSIONS);
        assertEquals(l.size(), 3);

        m = (Map)l.get(0);
        assertEquals(m.get(MongoStorage.VERSION), "1.0.3");
        assertEquals(m.get(MongoStorage.SUCCESS), 1D);
        assertEquals(m.get(MongoStorage.FAIL), 0D);
        assertEquals(m.get(MongoStorage.TOTAL), 1D);

        m = (Map)l.get(1);
        assertEquals(m.get(MongoStorage.VERSION), "1.0.2");
        assertEquals(m.get(MongoStorage.SUCCESS), 1D);
        assertEquals(m.get(MongoStorage.FAIL), 0D);
        assertEquals(m.get(MongoStorage.TOTAL), 1D);

        m = (Map)l.get(2);
        assertEquals(m.get(MongoStorage.VERSION), "1.0.1");
        assertEquals(m.get(MongoStorage.SUCCESS), 3D);
        assertEquals(m.get(MongoStorage.FAIL), 2D);
        assertEquals(m.get(MongoStorage.TOTAL), 5D);
    }

    @Test
    public void testAddTrialSubscriptionFailedIfUserHasSubscriptionAddedByRepositoryService() throws Exception {
        doReturn("[{"
                 + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                 + "accountReference:{id:\"accountId\",name:\"name1\"}"
                 + "}]").when(transport).doGet("/account", "token");
        doReturn(Boolean.TRUE).when(mongoStorage).hasStoredSubscription("id", AccountUtils.ON_PREMISES);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .post(JettyHttpServer.SECURE_PATH + "/repository/subscription/accountId");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode());
        verify(mongoStorage, never()).addSubscriptionInfo(anyString(), any(SubscriptionInfo.class));
    }

    @Test
    public void testAddTrialSubscriptionFailedIfUserHasSubscriptionAddedByManager() throws Exception {
        doReturn("[{"
                 + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                 + "accountReference:{id:\"accountId\",name:\"name1\"}"
                 + "}]")
                .when(transport).doGet(endsWith("/account"), eq("token"));
        doReturn("{startDate:\"01/01/2014\", endDate:\"01/01/2020\"}")
                .when(transport).doGet(endsWith("/account/subscriptions/subscriptionId/attributes"), eq("token"));
        doReturn("[{serviceId:" + AccountUtils.ON_PREMISES + ",id:subscriptionId}]")
                .when(transport).doGet(endsWith("/account/accountId/subscriptions"), eq("token"));
        doReturn(Boolean.FALSE).when(mongoStorage).hasStoredSubscription("id", AccountUtils.ON_PREMISES);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .post(JettyHttpServer.SECURE_PATH + "/repository/subscription/accountId");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode());
        verify(mongoStorage, never()).addSubscriptionInfo(anyString(), any(SubscriptionInfo.class));
    }

    @Test
    public void testAddTrialSubscriptionFailedIfUserIsNotOwnerOfAccount() throws Exception {
        doReturn("[{"
                 + "roles:[\"account/member\"],"
                 + "accountReference:{id:\"accountId\",name:\"name1\"}"
                 + "}]").when(transport).doGet("/account", "token");
        doReturn("[]").when(transport).doGet(endsWith("/account/accountId/subscriptions"), eq("token"));
        doReturn(Boolean.FALSE).when(mongoStorage).hasStoredSubscription("id", AccountUtils.ON_PREMISES);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .post(JettyHttpServer.SECURE_PATH + "/repository/subscription/accountId");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertEquals(response.getBody().asString(),
                     "Unexpected error. Can't add trial subscription. User 'id' is not owner of the account 'accountId'.");
        verify(mongoStorage, never()).addSubscriptionInfo(anyString(), any(SubscriptionInfo.class));
    }

    @Test
    public void testAddTrialSubscriptionFailedIfWrongAccount() throws Exception {
        doReturn("[{"
                 + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                 + "accountReference:{id:\"anotherAccountId\",name:\"name1\"}"
                 + "}]").when(transport).doGet("/account", "token");
        doReturn("[]").when(transport).doGet(endsWith("/account/accountId/subscriptions"), eq("token"));
        doReturn(Boolean.FALSE).when(mongoStorage).hasStoredSubscription("id", AccountUtils.ON_PREMISES);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .post(JettyHttpServer.SECURE_PATH + "/repository/subscription/accountId");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertEquals(response.getBody().asString(),
                     "Unexpected error. Can't add trial subscription. User 'id' is not owner of the account 'accountId'.");
        verify(mongoStorage, never()).addSubscriptionInfo(anyString(), any(SubscriptionInfo.class));
    }

    @Test
    public void testAddTrialSubscriptionFailedIfLoginFailed() throws Exception {
        doReturn("[{"
                 + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                 + "accountReference:{id:\"accountId\",name:\"name1\"}"
                 + "}]").when(transport).doGet("/account", "token");
        doReturn("[]").when(transport).doGet(endsWith("/account/accountId/subscriptions"), eq("token"));
        doThrow(new IOException("Unexpected error.")).when(transport).doPost(endsWith("/auth/login"), any(Object.class));
        doReturn(Boolean.FALSE).when(mongoStorage).hasStoredSubscription("id", AccountUtils.ON_PREMISES);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .post(JettyHttpServer.SECURE_PATH + "/repository/subscription/accountId");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertEquals(response.getBody().asString(), "Unexpected error. Login failed. Unexpected error.");
        verify(mongoStorage, never()).addSubscriptionInfo(anyString(), any(SubscriptionInfo.class));
    }

    @Test
    public void testAddTrialSubscriptionFailedIfLoginFailedEmptyResponse() throws Exception {
        doReturn("[{"
                 + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                 + "accountReference:{id:\"accountId\",name:\"name1\"}"
                 + "}]").when(transport).doGet("/account", "token");
        doReturn("[]").when(transport).doGet(endsWith("/account/accountId/subscriptions"), eq("token"));
        doReturn("{}").when(transport).doPost(endsWith("/auth/login"), any(Object.class));
        doReturn(Boolean.FALSE).when(mongoStorage).hasStoredSubscription("id", AccountUtils.ON_PREMISES);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .post(JettyHttpServer.SECURE_PATH + "/repository/subscription/accountId");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertEquals(response.getBody().asString(), "Unexpected error. Login failed. Malformed response. 'value' key is missed");
        verify(mongoStorage, never()).addSubscriptionInfo(anyString(), any(SubscriptionInfo.class));
    }

    @Test
    public void testAddTrialSubscriptionFailedIfApiServerReturnErrorUserCase1() throws Exception {
        doReturn("[{"
                 + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                 + "accountReference:{id:\"accountId\",name:\"name1\"}"
                 + "}]").when(transport).doGet("/account", "token");
        doReturn("[]").when(transport).doGet(endsWith("/account/accountId/subscriptions"), eq("token"));
        doReturn("{\"value\":\"userToken\"}").when(transport).doPost(endsWith("/auth/login"), any(Object.class));
        doReturn(Boolean.FALSE).when(mongoStorage).hasStoredSubscription("id", AccountUtils.ON_PREMISES);
        doThrow(new IOException("Unexpected error.")).when(transport).doPost(endsWith("/account/subscriptions"), any(Object.class), eq("userToken"));

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .post(JettyHttpServer.SECURE_PATH + "/repository/subscription/accountId");
        assertEquals(response.getBody().asString(), "Unexpected error. Can't add subscription. Unexpected error.");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        verify(mongoStorage, never()).addSubscriptionInfo(anyString(), any(SubscriptionInfo.class));
    }

    @Test
    public void testAddTrialSubscriptionFailedIfApiServerReturnErrorUserCase2() throws Exception {
        doReturn("[{"
                 + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                 + "accountReference:{id:\"accountId\",name:\"name1\"}"
                 + "}]").when(transport).doGet("/account", "token");
        doReturn("[]").when(transport).doGet(endsWith("/account/accountId/subscriptions"), eq("token"));
        doReturn("{\"value\":\"userToken\"}").when(transport).doPost(endsWith("/auth/login"), any(Object.class));
        doReturn(Boolean.FALSE).when(mongoStorage).hasStoredSubscription("id", AccountUtils.ON_PREMISES);
        doReturn("{\"message\":\"Error.\"}").when(transport).doPost(endsWith("/account/subscriptions"), any(Object.class), eq("userToken"));

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .post(JettyHttpServer.SECURE_PATH + "/repository/subscription/accountId");
        assertEquals(response.getBody().asString(), "Unexpected error. Can't add subscription. Error.");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        verify(mongoStorage, never()).addSubscriptionInfo(anyString(), any(SubscriptionInfo.class));
    }

    @Test
    public void testAddTrialSubscriptionFailedIfApiServerReturnErrorUserCase3() throws Exception {
        doReturn("[{"
                 + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                 + "accountReference:{id:\"accountId\",name:\"name1\"}"
                 + "}]").when(transport).doGet("/account", "token");
        doReturn("[]").when(transport).doGet(endsWith("/account/accountId/subscriptions"), eq("token"));
        doReturn("{\"value\":\"userToken\"}").when(transport).doPost(endsWith("/auth/login"), any(Object.class));
        doReturn(Boolean.FALSE).when(mongoStorage).hasStoredSubscription("id", AccountUtils.ON_PREMISES);
        doReturn("{}").when(transport).doPost(endsWith("/account/subscriptions"), any(Object.class), eq("userToken"));

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .post(JettyHttpServer.SECURE_PATH + "/repository/subscription/accountId");
        assertEquals(response.getBody().asString(), "Unexpected error. Can't add subscription. Malformed response. 'id' key is missed.");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        verify(mongoStorage, never()).addSubscriptionInfo(anyString(), any(SubscriptionInfo.class));
    }


    @Test
    public void testAddSubscription() throws Exception {
        doReturn("[{"
                 + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                 + "accountReference:{id:\"accountId\",name:\"name1\"}"
                 + "}]").when(transport).doGet("/account", "token");
        doReturn("[]").when(transport).doGet(endsWith("/account/accountId/subscriptions"), eq("token"));
        doReturn("{\"value\":\"userToken\"}").when(transport).doPost(endsWith("/auth/login"), any(Object.class));
        doReturn("{\"id\":\"subscriptionId\"}").when(transport).doPost(endsWith("/account/subscriptions"), any(Object.class), eq("userToken"));

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .post(JettyHttpServer.SECURE_PATH + "/repository/subscription/accountId");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
        verify(mongoStorage).addSubscriptionInfo(eq("id"), any(SubscriptionInfo.class));
        assertTrue(mongoStorage.hasStoredSubscription("id", ON_PREMISES));
    }
}

