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
package com.codenvy.cdec.restlet;

import java.io.IOException;

import javax.ws.rs.Path;

import org.restlet.data.ChallengeRequest;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.jaxrs.JaxRsClientResource;
import org.restlet.ext.jaxrs.internal.exceptions.IllegalPathException;
import org.restlet.ext.jaxrs.internal.exceptions.MissingAnnotationException;
import org.restlet.resource.ResourceException;

import com.codenvy.cdec.server.EmptyService;
import com.codenvy.cdec.server.InstallationManagerService;
import com.codenvy.cdec.server.ServerDescription;

/** @author Dmytro Nochevnov */
public class RestletClientFactory {
    public static <T extends EmptyService> T getServiceProxy(Class<T> resourceInterface) throws MissingAnnotationException,
                                                                                          IllegalPathException,
                                                                                          IOException {
        String resourceUri = getUri(ServerDescription.BASE_URI, InstallationManagerService.class);
        Reference reference = new Reference(resourceUri);

        JaxRsClientResource clientResource = new JaxRsClientResource(null, reference);

        T proxy = clientResource.wrap(resourceInterface);

        try {
            // perform first request to get unique and transient information from server to build the authentication credentials for the
            // next requests
            proxy.empty();

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
                                                                         ServerDescription.LOGIN,
                                                                         new String(ServerDescription.PASSWORD));
                clientResource.setChallengeResponse(authentication);
            }
        }

        return proxy;
    }

    private static String getUri(final String baseUri, Class resourceInterface) throws MissingAnnotationException, IllegalPathException {
        String path = getResourcePath(resourceInterface);

        String fullUriFromPath = baseUri + (baseUri.endsWith("/") ? "" : "/") + path;

        return fullUriFromPath;
    }


    private static String getResourcePath(Class resourceInterface) throws MissingAnnotationException, IllegalPathException {
        Path pathAnnotation = (Path)resourceInterface.getAnnotation(Path.class);
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
