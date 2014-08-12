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
package com.codenvy.cdec.im.restlet;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import javax.ws.rs.Path;

import org.restlet.Component;
import org.restlet.Server;
import org.restlet.data.ChallengeRequest;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.crypto.DigestAuthenticator;
import org.restlet.ext.jaxrs.JaxRsApplication;
import org.restlet.ext.jaxrs.JaxRsClientResource;
import org.restlet.ext.jaxrs.internal.exceptions.IllegalPathException;
import org.restlet.ext.jaxrs.internal.exceptions.JaxRsException;
import org.restlet.ext.jaxrs.internal.exceptions.MissingAnnotationException;
import org.restlet.resource.ResourceException;
import org.restlet.security.Authenticator;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.MapVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Dmytro Nochevnov
 */
public class TestRestlet {

    protected static final Logger LOG      = LoggerFactory.getLogger(TestRestlet.class);

    private Component             component;
    private Server                server;

    private String                login    = "login";
    private char[]                password = "secret".toCharArray();

    private String                baseUri;

    @BeforeMethod
    public void setUp() throws Exception {
        // http://restlet.com/learn/guide/2.2/extensions/jaxrs
        // create Component (as ever for Restlet)
        component = new Component();
        server = component.getServers().add(Protocol.HTTP, 8182);

        baseUri = "http://localhost:" + server.getPort();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        component.stop();
        LOG.info("Server stopped");
    }

    @Test
    public void testRestlet() throws Exception, JaxRsException {
        createApplication();
        startRestletServer();
        
        TestResource service = createJaxRsClient(baseUri, TestResource.class);

        String expectedHtml = "<html><head></head><body>\nThis is an easy resource (as html text).\n</body></html>";
        assertEquals(service.getHtml(), expectedHtml);
    }

    @Test
    public void testRestletWithBasicAuth() throws Exception {
        createApplicationWithBasicAuth();
        startRestletServer();

        TestResource service = createJaxRsClientWithBasicAuth(baseUri,
                                                                         TestResource.class,
                                                                         login,
                                                                         new String(password));

        String expectedHtml = "<html><head></head><body>\nThis is an easy resource (as html text).\n</body></html>";

        String serviceResponse = null;
        try {
            serviceResponse = service.getHtml();
        } catch (ResourceException re) {
            fail(re.getMessage());
        }

        assertEquals(serviceResponse, expectedHtml);

        service = createJaxRsClientWithBasicAuth(baseUri,
                                                 TestResource.class,
                                                 login,
                                                 "wrongPassword");

        serviceResponse = null;
        try {
            serviceResponse = service.getHtml();
        } catch (ResourceException re) {
            LOG.error(re.toString());
        }

        assertEquals(serviceResponse, null);
    }


    @Test
    public void testRestletWithDigestAuth() throws Exception {
        createApplicationWithDigestAuth();
        startRestletServer();

        String expectedHtml = "<html><head></head><body>\nThis is an easy resource (as html text).\n</body></html>";

        String resourceUri = getUri(baseUri, TestResource.class);
        Reference reference = new Reference(resourceUri);

        JaxRsClientResource clientResource = new JaxRsClientResource(null, reference);

        TestResource service = clientResource.wrap(TestResource.class);

        String serviceResponse = null;
        try {
            // perform first request to get unique and transient information from server to build the authentication credentials for the
            // next requests
            serviceResponse = service.getHtml();
            
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
                                                                         "wrongPassword");
                clientResource.setChallengeResponse(authentication);

                try {
                    serviceResponse = service.getHtml();
                } catch (ResourceException re2) {
                    // Configure authentication credentials
                    authentication = new ChallengeResponse(digestChallenge, 
                                                           clientResource.getResponse(), 
                                                           login, 
                                                           new String(password));
                    clientResource.setChallengeResponse(authentication);

                    try {
                        serviceResponse = service.getHtml();
                    } catch (ResourceException re3) {
                        fail(re3.getMessage());
                    }
                }
            }
        }

        assertEquals(serviceResponse, expectedHtml);
    }

    private void startRestletServer() throws Exception {
        component.start();

        LOG.info("Server started on port " + server.getPort());
    }

    private void createApplication() throws Exception {
        // http://restlet.com/learn/guide/2.2/extensions/jaxrs
        // create JAX-RS runtime environment
        JaxRsApplication application = new JaxRsApplication(component.getContext());

        // attach Application
        application.add(new TestApplication());

        // Attach the application to the component
        component.getDefaultHost().attach(application);
    }

    private void createApplicationWithBasicAuth() throws Exception {
        // create JAX-RS runtime environment
        JaxRsApplication application = new JaxRsApplication(component.getContext());

        // attach Application
        application.add(new TestApplication());

        Authenticator authenticator = getBasicAuthenticator(application);

        application.setAuthenticator(authenticator);

        // Attach the application to the component
        component.getDefaultHost().attach(application);
    }

    private Authenticator getBasicAuthenticator(JaxRsApplication application) {
        // Create authenticator
        ChallengeAuthenticator authenticator = new ChallengeAuthenticator(application.getContext(), ChallengeScheme.HTTP_BASIC, "test-realm");

        // Load a single static login/secret pair
        MapVerifier mapVerifier = new MapVerifier();
        mapVerifier.getLocalSecrets().put(login, password);
        authenticator.setVerifier(mapVerifier);

        return authenticator;
    }

    private void createApplicationWithDigestAuth() throws Exception {
        // create JAX-RS runtime environment
        JaxRsApplication application = new JaxRsApplication(component.getContext());

        // attach Application
        application.add(new TestApplication());

        Authenticator authenticator = getDigestAuthenticator(application);

        application.setAuthenticator(authenticator);

        // Attach the application to the component
        component.getDefaultHost().attach(application);
    }

    private Authenticator getDigestAuthenticator(JaxRsApplication application) {
        // Create authenticator
        DigestAuthenticator authenticator = new DigestAuthenticator(application.getContext(), "test-realm", "mySecretServerKey");

        // Load a single static login/secret pair
        MapVerifier mapVerifier = new MapVerifier();
        mapVerifier.getLocalSecrets().put(login, password);
        authenticator.setWrappedVerifier(mapVerifier);

        return authenticator;
    }

    /**
     * Creates a client resource that proxy calls to the given Java interface into Restlet method calls.
     * 
     * @param <T>
     * @param baseUri The target URI.
     * @param resourceInterface The annotated resource interface class to proxy.
     * @return The proxy instance.
     */
    public static <T> T createJaxRsClient(final String baseUri,
                                          final Class< ? extends T> resourceInterface) throws JaxRsException {
        String resourceUri = getUri(baseUri, resourceInterface);

        return JaxRsClientResource.createJaxRsClient(null, new Reference(resourceUri), resourceInterface);
    }

    /**
     * Creates a client resource with authentication that proxy calls to the given Java interface into Restlet method calls.
     * 
     * @param <T>
     * @param baseUri The target URI.
     * @param resourceInterface The annotated resource interface class to proxy.
     * @return The proxy instance.
     */
    public static <T> T createJaxRsClientWithBasicAuth(final String baseUri,
                                                       final Class< ? extends T> resourceInterface, String identifier, String psswd) throws JaxRsException {
        String resourceUri = getUri(baseUri, resourceInterface);

        Reference reference = new Reference(resourceUri);
        JaxRsClientResource clientResource = new JaxRsClientResource(null, reference);
        clientResource.setChallengeResponse(ChallengeScheme.HTTP_BASIC, identifier, psswd); // Set credentials

        return clientResource.wrap(resourceInterface);
    }

    private static String getUri(final String baseUri, Class resourceInterface) throws MissingAnnotationException, IllegalPathException {
        String path = getResourcePath(resourceInterface);

        String fullUriFromPath = baseUri + (baseUri.endsWith("/") ? "" : "/") + path;

        return fullUriFromPath;
    }


    private static String getResourcePath(Class resourceInterface) throws MissingAnnotationException, IllegalPathException {
        Path pathAnnotation = (Path) resourceInterface.getAnnotation(Path.class);
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
