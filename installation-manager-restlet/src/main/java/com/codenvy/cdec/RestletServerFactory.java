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
package com.codenvy.cdec;

import org.restlet.Component;
import org.restlet.Server;
import org.restlet.data.Protocol;
import org.restlet.ext.crypto.DigestAuthenticator;
import org.restlet.ext.jaxrs.JaxRsApplication;
import org.restlet.security.Authenticator;
import org.restlet.security.MapVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Application;

/**
 * @author Dmytro Nochevnov
 */
public class RestletServerFactory {
    private static final Logger LOG = LoggerFactory.getLogger(RestletServerFactory.class);

    private static RestletServer serverInstance;

    public static RestletServer getServer(Application application) {
        if (serverInstance == null) {
            serverInstance = new RestletServer(application);
        }
        
        return serverInstance;
    }
            
    public static class RestletServer {
        private Component    component;
        private Server       server;
        
        private RestletServer(Application application) {
            component = new Component();
            server = component.getServers().add(Protocol.HTTP, ServerDescription.PORT);
            
            try {
                createApplicationWithDigestAuth(application);
            } catch (Exception e) {
                LOG.error("Can't create application. ", e);
                return;
            }            
        }
        
        public void start() throws Exception {
            component.start();

            LOG.info("Server started on port " + ServerDescription.PORT);
        }
        
        public void stop() throws Exception {
            component.stop();
        }

        private void createApplicationWithDigestAuth(Application application) throws Exception {
            // create JAX-RS runtime environment
            JaxRsApplication applicationContainer = new JaxRsApplication(component.getContext());

            // attach Application
            applicationContainer.add(application);

            Authenticator authenticator = getDigestAuthenticator(applicationContainer);

            applicationContainer.setAuthenticator(authenticator);

            // Attach the application to the component
            component.getDefaultHost().attach(applicationContainer);
        }

        private Authenticator getDigestAuthenticator(JaxRsApplication applicationContainer) {
            // Create authenticator
            DigestAuthenticator authenticator = new DigestAuthenticator(applicationContainer.getContext(), 
                                                                        ServerDescription.REALM, 
                                                                        ServerDescription.SERVER_DIGEST_KEY);

            // Load a single static login/secret pair
            MapVerifier mapVerifier = new MapVerifier();
            mapVerifier.getLocalSecrets().put(ServerDescription.LOGIN, ServerDescription.PASSWORD);
            authenticator.setWrappedVerifier(mapVerifier);

            return authenticator;
        }

    }
}
