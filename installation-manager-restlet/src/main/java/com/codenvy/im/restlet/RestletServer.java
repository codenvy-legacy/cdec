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

import org.restlet.Component;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.engine.Engine;
import org.restlet.ext.crypto.DigestAuthenticator;
import org.restlet.ext.jaxrs.JaxRsApplication;
import org.restlet.ext.slf4j.Slf4jLoggerFacade;
import org.restlet.security.Authenticator;
import org.restlet.security.MapVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Application;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Anatoliy Bazko
 */
public class RestletServer {
    private static final Logger LOG = LoggerFactory.getLogger(RestletServer.class);

    private final Component component;
    private final Context   context;
    private final URL       url;

    public RestletServer(Application application) throws MalformedURLException {
        url = new URL(ServerDescription.SERVER_URL);
        component = new Component();
        component.getServers().add(Protocol.HTTP, url.getPort());
        context = component.getContext().createChildContext();

        // use org.slf4j logger in restlet server 
        Engine.getInstance().setLoggerFacade(new Slf4jLoggerFacade());

        try {
            createApplicationWithDigestAuth(application);
        } catch (Exception e) {
            LOG.error("Can't create application. ", e);
        }
    }

    public void start() throws Exception {
        component.start();
        LOG.info("Restlet server started on port " + url.getPort());
    }

    public void stop() throws Exception {
        component.stop();
    }

    private void createApplicationWithDigestAuth(Application application) throws Exception {
        // create JAX-RS runtime environment
        JaxRsApplication applicationContainer = new JaxRsApplication(context);

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
