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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codenvy.cdec.artifacts.InstallManagerArtifact;
import com.codenvy.cdec.im.InstallationManagerApplication;
import com.codenvy.cdec.im.InstallationManagerImpl;
import com.codenvy.cdec.im.UpdateManager;
import com.codenvy.cdec.restlet.RestletServerFactory;
import com.codenvy.cdec.server.InstallationManager;
import com.codenvy.cdec.utils.BasedInjector;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * @author Anatoliy Bazko
 */
public class Daemon {

    private static final Logger        LOG = LoggerFactory.getLogger(Daemon.class);

    private static final UpdateManager                      updateManager;
    private static final Injector                           injector;
    private static final RestletServerFactory.RestletServer installationManager;

    static {
        injector = createInjector();
        updateManager = injector.getInstance(UpdateManager.class);
        installationManager = RestletServerFactory.getServer(new InstallationManagerApplication());
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
            installationManager.stop();
            LOG.info("Server stopped.");
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

        try {
            installationManager.start();
        } catch (Exception e) {
            LOG.error("Can't start server. ", e);
            return;
        }
    }

    private static Injector createInjector() {
        return BasedInjector.getInstance().createChildInjector(new Module() {
            @Override
            public void configure(Binder binder) {
                binder.bind(InstallationManager.class).to(InstallationManagerImpl.class);
            }
        });
    }
    
}
