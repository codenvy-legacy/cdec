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
package com.codenvy.cdec.update;

import com.codenvy.cdec.artifacts.InstallManagerArtifact;
import com.codenvy.cdec.utils.Commons;
import com.codenvy.cdec.utils.AccountUtils;
import com.codenvy.cdec.utils.HttpTransport;
import com.codenvy.commons.user.UserImpl;
import com.jayway.restassured.response.Response;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.util.log.Log;
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
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import static com.jayway.restassured.RestAssured.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * @author Anatoliy Bazko
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class TestRepositoryService extends BaseTest {

    private final ArtifactStorage   artifactStorage;
    private final RepositoryService repositoryService;
    private final HttpTransport     transport;
    private final UserManager       userManager;

    private final Properties authenticationRequiredProperties = new Properties() {{
        put(ArtifactStorage.AUTHENTICATION_REQUIRED_PROPERTY, "true");
    }};
    private final Properties subscriptionRequiredProperties   = new Properties() {{
        put(ArtifactStorage.SUBSCRIPTION_REQUIRED_PROPERTY, "On-Premises");
    }};

    {
        try {
            transport = mock(HttpTransport.class);
            userManager = mock(UserManager.class);
            artifactStorage = new ArtifactStorage(DOWNLOAD_DIRECTORY.toString());
            repositoryService = new RepositoryService("",
                                                      userManager,
                                                      artifactStorage,
                                                      new MongoStorage("mongodb://localhost:12000/update", true, "target"),
                                                      transport);

        } catch (IOException e) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    @BeforeMethod
    public void setUp() throws Exception {
        when(userManager.getCurrentUser()).thenReturn(new UserImpl("name", "id", "token", Collections.<String>emptyList(), false));
        super.setUp();
    }

    //@Test
    public void testSaveInstalledInfoErrorIfInvalidUserAgent() throws Exception {
        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .post(JettyHttpServer.SECURE_PATH + "/repository/info/cdec/1.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.FORBIDDEN.getStatusCode());
    }

    //@Test
    public void testSaveInstalledInfoErrorIfVersionInvalid() throws Exception {
        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .header("user-agent", RepositoryService.VALID_USER_AGENT)
                .post(JettyHttpServer.SECURE_PATH + "/repository/info/cdec/.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    //@Test
    public void testSaveInstalledInfo() throws Exception {
        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .header("user-agent", RepositoryService.VALID_USER_AGENT).post(JettyHttpServer.SECURE_PATH + "/repository/info/cdec/1.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
    }

    //@Test
    public void testGetInstalledInfo() throws Exception {
        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .header("user-agent", RepositoryService.VALID_USER_AGENT).post(JettyHttpServer.SECURE_PATH + "/repository/info/cdec/1.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .header("user-agent", RepositoryService.VALID_USER_AGENT).get(JettyHttpServer.SECURE_PATH + "/repository/info/cdec");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        Map m = response.as(Map.class);
        assertEquals(m.size(), 4);
        assertEquals(m.get("artifact"), "cdec");
        assertEquals(m.get("version"), "1.0.1");
        assertEquals(m.get("userId"), "id");
        assertNotNull(m.get("date"));
    }

    //@Test
    public void testGetLatestVersion() throws Exception {
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), InstallManagerArtifact.NAME, "1.0.1", "tmp", new Properties());
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), InstallManagerArtifact.NAME, "1.0.2", "tmp", new Properties());

        Response response = given().when().get("repository/version/installation-manager");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        Map value = Commons.fromJson(response.body().asString(), Map.class);
        assertEquals(value.size(), 3);
        assertEquals(value.get(ArtifactStorage.ARTIFACT_PROPERTY), InstallManagerArtifact.NAME);
        assertEquals(value.get(ArtifactStorage.VERSION_PROPERTY), "1.0.2");
        assertEquals(value.get(ArtifactStorage.FILE_NAME_PROPERTY), "tmp");
    }

    //@Test
    public void testDownloadPublicArtifact() throws Exception {
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), InstallManagerArtifact.NAME, "1.0.1", "tmp", new Properties());

        Response response = given().when().get("repository/public/download/" + InstallManagerArtifact.NAME + "/1.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
        assertEquals(IOUtils.toString(response.body().asInputStream()), "content");
    }

    //@Test
    public void testDownloadPublicErrorIfArtifactAbsent() throws Exception {
        Response response = given().when().get("repository/public/download/installation-manager/1.0.2");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.NOT_FOUND.getStatusCode());
    }

    //@Test
    public void testDownloadPublicArtifactLatestVersion() throws Exception {
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), InstallManagerArtifact.NAME, "1.0.1", "tmp", new Properties());

        Response response = given().when().get("repository/public/download/" + InstallManagerArtifact.NAME);
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
        assertEquals(IOUtils.toString(response.body().asInputStream()), "content");
    }

    //@Test
    public void testDownloadPublicArtifactErrorIfAuthenticationRequired() throws Exception {
        when(transport.doGetRequest("/account")).thenReturn("[{accountReference:{id:accountId}}]");
        when(transport.doGetRequest("/account/accountId/subscriptions")).thenReturn("[{serviceId:On-Premises}]");
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), "cdec", "1.0.1", "tmp", authenticationRequiredProperties);

        Response response = given().when().get("repository/public/download/cdec/1.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.UNAUTHORIZED.getStatusCode());
    }

    //@Test
    public void testDownloadPublicArtifactErrorIfSubscriptionRequired() throws Exception {
        when(transport.doGetRequest("/account")).thenReturn("[{accountReference:{id:accountId}}]");
        when(transport.doGetRequest("/account/accountId/subscriptions")).thenReturn("[{serviceId:On-Premises}]");
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), "cdec", "1.0.1", "tmp", subscriptionRequiredProperties);

        Response response = given().when().get("/repository/public/download/cdec/1.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.UNAUTHORIZED.getStatusCode());
    }

    //@Test
    public void testDownloadPrivate() throws Exception {
        when(transport.doGetRequest("/account")).thenReturn("[{accountReference:{id:accountId}}]");
        when(transport.doGetRequest("/account/accountId/subscriptions")).thenReturn("[]");
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), "cdec", "1.0.1", "tmp", new Properties());

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get(JettyHttpServer.SECURE_PATH + "/repository/download/cdec/1.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
        assertEquals(IOUtils.toString(response.body().asInputStream()), "content");
    }

    //@Test
    public void testDownloadPrivateSubscriptionRequired() throws Exception {
        when(transport.doGetRequest("/account", null)).thenReturn("[{roles:[\"account/owner\"],accountReference:{id:accountId}}]");
        when(transport.doGetRequest("/account/accountId/subscriptions", null)).thenReturn("[{serviceId:On-Premises}]");
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), "cdec", "1.0.1", "tmp", subscriptionRequiredProperties);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get(JettyHttpServer.SECURE_PATH + "/repository/download/cdec/1.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
        assertEquals(IOUtils.toString(response.body().asInputStream()), "content");
    }

    //@Test
    public void testDownloadPrivateAuthenticationRequired() throws Exception {
        when(transport.doGetRequest("/account")).thenReturn("[{accountReference:{id:accountId}}]");
        when(transport.doGetRequest("/account/accountId/subscriptions")).thenReturn("[]");
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), "cdec", "1.0.1", "tmp", authenticationRequiredProperties);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get(JettyHttpServer.SECURE_PATH + "/repository/download/cdec/1.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
        assertEquals(IOUtils.toString(response.body().asInputStream()), "content");
    }


    @Test
    public void testDownloadPrivateErrorIfSubscriptionAbsent() throws Exception {
        when(transport.doGetRequest("/account", null)).thenReturn("[{roles:[\"account/owner\"],accountReference:{id:accountId}}]");
        when(transport.doGetRequest("/account/accountId/subscriptions", null)).thenReturn("[]");
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), "cdec", "1.0.1", "tmp", subscriptionRequiredProperties);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get(JettyHttpServer.SECURE_PATH + "/repository/download/cdec/1.0.1");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.FORBIDDEN.getStatusCode());
        assertEquals(response.asString(), RepositoryService.VALID_ON_PREMISES_SUBSCRIPTION_NOT_FOUND_ERROR);
    }

    //@Test
    public void testDownloadPrivateErrorIfUserDoesnotHasProperAccount() throws Exception {
        when(transport.doGetRequest("/account", null)).thenReturn("[{accountReference:{id:accountId}}]");
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), "cdec", "1.0.1", "tmp", subscriptionRequiredProperties);

        Response response = given().auth()
               .basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)
               .when()
               .get(JettyHttpServer.SECURE_PATH + "/repository/download/cdec/1.0.1");
        
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertTrue(response.asString().contains(AccountUtils.PROPER_ACOUNT_NOT_FOUND_ERROR));
    }

    //@Test
    public void testDownloadPrivateErrorIfItsImpossibleToGetPathToSubscriptions() throws Exception {
        when(transport.doGetRequest("/account", null)).thenReturn("[{roles:[\"account/owner\"],accountReference:null}]");
        artifactStorage.upload(new ByteArrayInputStream("content".getBytes()), "cdec", "1.0.1", "tmp", subscriptionRequiredProperties);

        Response response = given().auth()
               .basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)
               .when()
               .get(JettyHttpServer.SECURE_PATH + "/repository/download/cdec/1.0.1");
        
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertTrue(response.asString().contains(AccountUtils.PATH_TO_SUBSCRIPTIONS_NOT_FOUND_ERROR));
    }
    
    //@Test
    public void testDownloadPrivateErrorIfNoRolesAllowed() throws Exception {
        Response response = given().when().get("repository/download/cdec/1.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.FORBIDDEN.getStatusCode());
    }

    //@Test
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

    //@Test
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
        assertEquals(properties.get(ArtifactStorage.VERSION_PROPERTY), "1.0.1-SNAPSHOT");
        assertEquals(properties.get(ArtifactStorage.FILE_NAME_PROPERTY), "tmp-1.0.1.txt");
        assertEquals(properties.get(ArtifactStorage.ARTIFACT_PROPERTY), "cdec");
    }


    //@Test
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
        assertEquals(properties.get(ArtifactStorage.VERSION_PROPERTY), "1.0.1");
        assertEquals(properties.get(ArtifactStorage.FILE_NAME_PROPERTY), "tmp-1.0.1.txt");
        assertEquals(properties.get(ArtifactStorage.BUILD_TIME_PROPERTY), "20140930");
        assertEquals(properties.get(ArtifactStorage.ARTIFACT_PROPERTY), "cdec");
    }

    //@Test
    public void testUploadErrorIfVersionHasBadFormat() throws Exception {
        Path tmp = Paths.get("target/tmp");
        Files.copy(new ByteArrayInputStream("content".getBytes()), tmp, StandardCopyOption.REPLACE_EXISTING);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .multiPart(tmp.toFile()).post(JettyHttpServer.SECURE_PATH + "/repository/upload/cdec-1.01.1/1.01.1");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    //@Test
    public void testUploadErrorIfNoStream() throws Exception {
        Files.copy(new ByteArrayInputStream("content".getBytes()), Paths.get("target/tmp"), StandardCopyOption.REPLACE_EXISTING);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .post(JettyHttpServer.SECURE_PATH + "/repository/upload/cdec-1.01.1/1.01.1");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }
}

