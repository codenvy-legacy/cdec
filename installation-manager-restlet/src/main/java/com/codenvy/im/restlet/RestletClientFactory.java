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
package com.codenvy.im.restlet;

import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Reference;
import org.restlet.ext.jaxrs.JaxRsClientResource;
import org.restlet.ext.jaxrs.internal.exceptions.IllegalPathException;
import org.restlet.ext.jaxrs.internal.exceptions.MissingAnnotationException;

import javax.ws.rs.Path;

import static com.codenvy.im.utils.Commons.combinePaths;

/**
 * @author Dmytro Nochevnov
 */
public class RestletClientFactory {

    /**
     * Utility class so no public constructor.
     */
    private RestletClientFactory() {
    }
    
    /**
     * Creates proxy service to communicate with server.
     */
    public static synchronized <T> T createServiceProxy(Class<T> resourceInterface) throws MissingAnnotationException,
                                                                                                                     IllegalPathException {

        String resourceUri = combinePaths(ServerDescription.SERVER_URL, getResourcePath(InstallationManagerService.class));
        Reference reference = new Reference(resourceUri);

        JaxRsClientResource clientResource = new JaxRsClientResource(null, reference);
        T proxy = clientResource.wrap(resourceInterface);
        doBasicAuthentication(clientResource);

        return proxy;
    }

    private static void doBasicAuthentication(JaxRsClientResource clientResource) {
        ChallengeResponse authentication = new ChallengeResponse(ChallengeScheme.HTTP_BASIC,
                                                                 ServerDescription.LOGIN,
                                                                 new String(ServerDescription.PASSWORD));
        clientResource.setChallengeResponse(authentication);
    }

    private static String getResourcePath(Class resourceInterface) throws MissingAnnotationException, IllegalPathException {
        Path pathAnnotation = (Path)resourceInterface.getAnnotation(Path.class);
        if (pathAnnotation == null) {
            throw new MissingAnnotationException("The resource interface must have the JAX-RS path annotation.");
        }

        String path = pathAnnotation.value();
        if (path == null || path.isEmpty()) {
            throw new IllegalPathException(pathAnnotation, "The path annotation must have a value.");
        }

        return path;
    }
}
