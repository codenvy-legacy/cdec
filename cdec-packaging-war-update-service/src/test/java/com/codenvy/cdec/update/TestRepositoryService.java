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

import com.jayway.restassured.response.Response;

import org.apache.commons.io.FileUtils;
import org.everrest.assured.EverrestJetty;
import org.everrest.assured.JettyHttpServer;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

import static com.jayway.restassured.RestAssured.given;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Anatoliy Bazko
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class TestRepositoryService extends BaseTest {

    private final ArtifactHandler   artifactHandler;
    private final RepositoryService repositoryService;

    {
        try {
            artifactHandler = new ArtifactHandler(DOWNLOAD_DIRECTORY.toString());
            repositoryService = new RepositoryService(artifactHandler);
        } catch (IOException e) {
            throw new IllegalArgumentException();
        }
    }

    @Test
    public void testVersion() throws Exception {
        artifactHandler.upload(new ByteArrayInputStream("content".getBytes()), RepositoryService.INSTALL_MANAGER, "1.0.1", "tmp", new Properties());
        artifactHandler.upload(new ByteArrayInputStream("content".getBytes()), RepositoryService.INSTALL_MANAGER, "1.0.2", "tmp", new Properties());

        Response response = given().when().get("repository/version/install-manager");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
        assertEquals(response.body().asString(), "1.0.2");
    }

    @Test
    public void testDownloadInstallManager() throws Exception {
        artifactHandler.upload(new ByteArrayInputStream("content".getBytes()), RepositoryService.INSTALL_MANAGER, "1.0.1", "tmp", new Properties());

        Response response = given().when().get("repository/download/" + RepositoryService.INSTALL_MANAGER + "/1.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        InputStream in = response.body().asInputStream();

        Path tmp = Paths.get("target/tmp");
        Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        assertEquals(FileUtils.readFileToString(tmp.toFile()), "content");
    }

    @Test
    public void testDownloadArtifact() throws Exception {
        artifactHandler.upload(new ByteArrayInputStream("content".getBytes()), "cdec", "1.0.1", "tmp", new Properties());

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get(JettyHttpServer.SECURE_PATH + "/repository/download/cdec/1.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        InputStream in = response.body().asInputStream();

        Path tmp = Paths.get("target/tmp");
        Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        assertEquals(FileUtils.readFileToString(tmp.toFile()), "content");
    }

    @Test
    public void testDownloadArtifactNoRolesAllowed() throws Exception {
        Response response = given().when().get("repository/download/cdec/1.0.1");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    public void testDownloadNotExistedArtifact() throws Exception {
        Response response = given().when().get("repository/download/install-manager/1.0.2");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testUpload() throws Exception {
        Path tmp = Paths.get("target/tmp-1.0.1.txt");
        Files.copy(new ByteArrayInputStream("content".getBytes()), tmp, StandardCopyOption.REPLACE_EXISTING);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .multiPart(tmp.toFile()).post(JettyHttpServer.SECURE_PATH + "/repository/upload/cdec/1.0.1?revision=abcd&buildtime=20140930");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        Path artifact = Paths.get("target", "download", "cdec", "1.0.1", "tmp-1.0.1.txt");
        assertEquals(FileUtils.readFileToString(artifact.toFile()), "content");
        assertTrue(Files.exists(artifact));

        Path propertiesFile = Paths.get("target", "download", "cdec", "1.0.1", ArtifactHandler.ARTIFACT_PROPERTIES);
        assertTrue(Files.exists(propertiesFile));

        Properties properties = new Properties();
        properties.load(Files.newInputStream(propertiesFile));
        assertEquals(properties.size(), 4);
        assertEquals(properties.get(ArtifactHandler.VERSION_PROPERTY), "1.0.1");
        assertEquals(properties.get(ArtifactHandler.FILE_NAME_PROPERTY), "tmp-1.0.1.txt");
        assertEquals(properties.get("revision"), "abcd");
        assertEquals(properties.get("buildtime"), "20140930");
    }

    @Test
    public void testUploadWithoutVersionInFileName() throws Exception {
        Path tmp = Paths.get("target/tmp");
        Files.copy(new ByteArrayInputStream("content".getBytes()), tmp, StandardCopyOption.REPLACE_EXISTING);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .multiPart(tmp.toFile()).post(JettyHttpServer.SECURE_PATH + "/repository/upload/cdec/1.0.1");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testUploadArtifactWithBadFormatVersion() throws Exception {
        Path tmp = Paths.get("target/tmp");
        Files.copy(new ByteArrayInputStream("content".getBytes()), tmp, StandardCopyOption.REPLACE_EXISTING);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .multiPart(tmp.toFile()).post(JettyHttpServer.SECURE_PATH + "/repository/upload/cdec-1.01.1/1.01.1");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testUploadNothing() throws Exception {
        Path tmp = Paths.get("target/tmp");
        Files.copy(new ByteArrayInputStream("content".getBytes()), tmp, StandardCopyOption.REPLACE_EXISTING);

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .post(JettyHttpServer.SECURE_PATH + "/repository/upload/cdec-1.01.1/1.01.1");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }
}

