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
package com.codenvy.cdec.im;

import com.codenvy.cdec.Daemon;
import com.codenvy.cdec.artifacts.Artifact;
import com.codenvy.cdec.artifacts.CDECArtifact;
import com.codenvy.cdec.artifacts.InstallManagerArtifact;
import com.codenvy.cdec.im.restlet.TestResource;
import com.codenvy.cdec.im.restlet.TestRestlet;
import com.codenvy.cdec.server.InstallationManager;
import com.codenvy.cdec.utils.HttpTransport;

import org.apache.commons.io.FileUtils;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restlet.Component;
import org.restlet.Server;
import org.restlet.data.ChallengeRequest;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.jaxrs.JaxRsClientResource;
import org.restlet.ext.jaxrs.internal.exceptions.IllegalPathException;
import org.restlet.ext.jaxrs.internal.exceptions.MissingAnnotationException;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author Anatoliy Bazko
 */
public class TestInstallationManager {

    private Artifact CDEC_ARTIFACT;
    private Artifact INSTALL_MANAGER_ARTIFACT;
    
    protected static final Logger LOG      = LoggerFactory.getLogger(TestRestlet.class);

    private String                login    = "login";
    private char[]                password = "secret".toCharArray();

    private static int            port     = 8182;
    private static String         baseUri  = "http://localhost:" + port;

    InstallationManager           managerProxy;
    private HttpTransport         transport;
    
    @BeforeTest
    public void setUp() throws Exception {   
        transport = mock(HttpTransport.class);
        
        Daemon.start();
        
        initServiceClient();
    }

    private void initServiceClient() throws MissingAnnotationException, IllegalPathException, IOException {
        String resourceUri = getUri(baseUri, InstallationManager.class);
        Reference reference = new Reference(resourceUri);

        JaxRsClientResource clientResource = new JaxRsClientResource(null, reference);

        managerProxy = clientResource.wrap(InstallationManagerImpl.class);

        try {
            // perform first request to get unique and transient information from server to build the authentication credentials for the
            // next requests
            managerProxy.checkNewVersions();
            
        } catch (ResourceException re) {
            if (Status.CLIENT_ERROR_UNAUTHORIZED.equals(re.getStatus())) {
                ChallengeRequest digestChallenge = null;

                // Retrieve HTTP Digest hints
                for (ChallengeRequest challengeRequest : clientResource.getChallengeRequests()) {
                    if (ChallengeScheme.HTTP_DIGEST.equals(challengeRequest.getScheme())) {
                        digestChallenge = challengeRequest;

                        break;
                    }
                }

                // Configure authentication credentials
                ChallengeResponse authentication = new ChallengeResponse(digestChallenge, 
                                                                         clientResource.getResponse(), 
                                                                         login,
                                                                         new String(password));
                clientResource.setChallengeResponse(authentication);
            }
        }
    }

    @AfterTest
    public void tearDown() throws Exception {
        Daemon.stop();
    }

    @Test
    public void testDownloadUpdates() throws Exception {
        final Path file1 = Paths.get("target", "download", CDEC_ARTIFACT.getName(), "file1");
        final Path file2 = Paths.get("target", "download", INSTALL_MANAGER_ARTIFACT.getName(), "file2");

        stub(transport.download(eq("update/endpoint/repository/public/download/" + InstallManagerArtifact.NAME + "/1.0.1"), any(Path.class)))
                .toAnswer(new Answer<Path>() {
                    @Override
                    public Path answer(InvocationOnMock invocationOnMock) throws Throwable {
                        Files.createDirectories(file2.getParent());
                        Files.createFile(file2);
                        return file2;
                    }
                });
        stub(transport.download(eq("update/endpoint/repository/download/" + CDECArtifact.NAME + "/2.10.5"), any(Path.class)))
                .toAnswer(new Answer<Path>() {
                    @Override
                    public Path answer(InvocationOnMock invocationOnMock) throws Throwable {
                        Files.createDirectories(file1.getParent());
                        Files.createFile(file1);
                        return file1;
                    }
                });
        when(managerProxy.getNewVersions()).thenReturn(new HashMap<Artifact, String>() {{
            put(CDEC_ARTIFACT, "2.10.5");
            put(INSTALL_MANAGER_ARTIFACT, "1.0.1");
        }});
        doReturn(true).when(managerProxy).isValidSubscription();

        managerProxy.downloadUpdates();

        Map<Artifact, Path> m = managerProxy.getDownloadedArtifacts();
        assertEquals(m.size(), 2);
        assertEquals(m.get(CDEC_ARTIFACT), file1);
        assertEquals(m.get(INSTALL_MANAGER_ARTIFACT), file2);
    }

    @Test
    public void testGetInstalledArtifacts() throws Exception {
        when(transport.doGetRequest("update/endpoint/repository/info/" + CDECArtifact.NAME)).thenReturn("{version:2.10.4}");

        Map<Artifact, String> m = managerProxy.getInstalledArtifacts();
        assertEquals(m.get(CDEC_ARTIFACT), "2.10.4");
        assertNotNull(m.get(INSTALL_MANAGER_ARTIFACT));
    }

    @Test
    public void testGetAvailable2DownloadArtifacts() throws Exception {
        when(transport.doGetRequest("update/endpoint/repository/version/" + InstallManagerArtifact.NAME)).thenReturn("{version:1.0.1}");
        when(transport.doGetRequest("update/endpoint/repository/version/" + CDECArtifact.NAME)).thenReturn("{version:2.10.5}");
        Map<Artifact, String> m = managerProxy.getAvailable2DownloadArtifacts();

        assertEquals(m.size(), 2);
        assertEquals(m.get(INSTALL_MANAGER_ARTIFACT), "1.0.1");
        assertEquals(m.get(CDEC_ARTIFACT), "2.10.5");
    }

    @Test
    public void testGetDownloadedArtifacts() throws Exception {
        Path file1 = Paths.get("target", "download", CDEC_ARTIFACT.getName(), "file1");
        Path file2 = Paths.get("target", "download", INSTALL_MANAGER_ARTIFACT.getName(), "file2");
        Files.createDirectories(file1.getParent());
        Files.createDirectories(file2.getParent());
        Files.createFile(file1);
        Files.createFile(file2);

        Map<Artifact, Path> m = managerProxy.getDownloadedArtifacts();
        assertEquals(m.size(), 2);
        assertEquals(m.get(CDEC_ARTIFACT), file1);
        assertEquals(m.get(INSTALL_MANAGER_ARTIFACT), file2);
    }

    @Test
    public void testGetDownloadedArtifactsReturnsEmptyMap() throws Exception {
        Map<Artifact, Path> m = managerProxy.getDownloadedArtifacts();
        assertTrue(m.isEmpty());
    }

    @Test(expectedExceptions = IOException.class)
    public void testGetDownloadedArtifactsErrorIfMoreThan1File() throws Exception {
        Path file1 = Paths.get("target", "download", CDEC_ARTIFACT.getName(), "file1");
        Path file2 = Paths.get("target", "download", CDEC_ARTIFACT.getName(), "file2");
        Files.createDirectories(file1.getParent());
        Files.createFile(file1);
        Files.createFile(file2);

        managerProxy.getDownloadedArtifacts();
    }
    
    private static String getUri(final String baseUri, Class resourceInterface) throws MissingAnnotationException, IllegalPathException {
        String path = getResourcePath(resourceInterface);

        String fullUriFromPath = baseUri + (baseUri.endsWith("/") ? "" : "/") + path;

        return fullUriFromPath;
    }


    private static String getResourcePath(Class resourceInterface) throws MissingAnnotationException, IllegalPathException {
        javax.ws.rs.Path pathAnnotation = (javax.ws.rs.Path) resourceInterface.getAnnotation(Path.class);
        if (pathAnnotation == null) {
            throw new MissingAnnotationException("The resource interface must have the JAX-RS path annotation.");
        }

        String path = pathAnnotation.value();
        if (path == null || path.length() == 0) {
            throw new IllegalPathException(pathAnnotation,
                                           "The path annotation must have a value.");
        }
        return path;
    }
}
