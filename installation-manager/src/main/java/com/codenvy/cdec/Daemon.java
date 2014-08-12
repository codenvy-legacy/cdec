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

import java.io.IOException;
import java.nio.file.Paths;

import org.quartz.SchedulerException;
import org.restlet.Component;
import org.restlet.Server;
import org.restlet.data.Protocol;
import org.restlet.ext.crypto.DigestAuthenticator;
import org.restlet.ext.jaxrs.JaxRsApplication;
import org.restlet.security.Authenticator;
import org.restlet.security.MapVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codenvy.cdec.artifacts.InstallManagerArtifact;
import com.codenvy.cdec.im.InstallationManagerImpl;
import com.codenvy.cdec.im.UpdateManager;
import com.codenvy.cdec.im.restlet.TestApplication;
import com.codenvy.cdec.server.InstallationManager;
import com.codenvy.cdec.utils.BasedInjector;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * @author Anatoliy Bazko
 */
public class Daemon {

    private static final Logger LOG = LoggerFactory.getLogger(Daemon.class);

    private static final UpdateManager updateManager;
    private static final Injector      injector;
    
    static private Component             component;
    static private Server                server;

    static private String                login    = "login";
    static private char[]                password = "secret".toCharArray();
    static private String                realm = "im-realm";
    static private String                serverDigestKey = "imSecretServerKey";
        
    static private String                baseUri;
    static private int                   port = 8182;
    static private String                serverAddress = "localhost";

    static {
        injector = createInjector();
        updateManager = injector.getInstance(UpdateManager.class);
    }

    public static void main(String[] args) {
        if (args.length != 0) {
            update(args); // TODO testing purpose
        }

        try {
            daemonize();
        } catch (Throwable e) {
            LOG.error("Startup failed. " + e.getMessage());
        }

        try {
            start();
        } catch (Exception e) {
            LOG.error("Can't start daemon. " + e.getMessage());
            stop();
        }
    }

    private static void update(String[] args) {
        InstallManagerArtifact artifact = injector.getInstance(InstallManagerArtifact.class);
        try {
            artifact.install(Paths.get(args[0]));
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private static void daemonize() throws IOException {
        System.in.close();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                Daemon.stop();
            }
        });
    }

    public static void stop() {
        try {
            updateManager.destroy();
        } catch (SchedulerException e) {
            LOG.error("Can't stop Update Manager. ", e);
        }

        try {
            component.stop();
        } catch (Exception e) {
            LOG.error("Can't stop server. ", e);
        }
    }

    public static void start() {
        try {
            updateManager.init();
        } catch (SchedulerException e) {
            LOG.error("Can't initialize Update Manager. ", e);
        }

        startServer();
    }

    private static void startServer() {
        component = new Component();
        server = component.getServers().add(Protocol.HTTP, port);

        baseUri = "http://" + serverAddress + ":" + server.getPort();
        
        try {
            createApplicationWithDigestAuth();
        } catch (Exception e) {
            LOG.error("Can't create application. ", e);
            return;
        }
        
        try {
            component.start();
        } catch (Exception e) {
            LOG.error("Can't start server. ", e);
            return;
        }

        LOG.info("Server started on port " + server.getPort());
    }

    private static Injector createInjector() {
        return BasedInjector.getInstance().createChildInjector(new Module() {
            @Override
            public void configure(Binder binder) {
                binder.bind(InstallationManager.class).to(InstallationManagerImpl.class);
            }
        });
    }
    
    private static void createApplicationWithDigestAuth() throws Exception {
        // create JAX-RS runtime environment
        JaxRsApplication application = new JaxRsApplication(component.getContext());

        // attach Application
        application.add(new TestApplication());

        Authenticator authenticator = getDigestAuthenticator(application);

        application.setAuthenticator(authenticator);

        // Attach the application to the component
        component.getDefaultHost().attach(application);
    }

    private static Authenticator getDigestAuthenticator(JaxRsApplication application) {
        // Create authenticator
        DigestAuthenticator authenticator = new DigestAuthenticator(application.getContext(), realm, serverDigestKey);

        // Load a single static login/secret pair
        MapVerifier mapVerifier = new MapVerifier();
        mapVerifier.getLocalSecrets().put(login, password);
        authenticator.setWrappedVerifier(mapVerifier);

        return authenticator;
    }
}
