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

import com.codenvy.cdec.artifacts.Artifact;
import com.codenvy.cdec.artifacts.InstallManagerArtifact;
import com.codenvy.cdec.im.InstallationManagerImpl;
import com.codenvy.cdec.im.UpdateManager;
import com.codenvy.cdec.server.InstallationManager;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Anatoliy Bazko
 */
public class Daemon {

    private static final Logger LOG = LoggerFactory.getLogger(Daemon.class);

    private static final UpdateManager updateManager;
    private static final Injector      injector;

    static {
        injector = createInjector();
        updateManager = injector.getInstance(UpdateManager.class);
    }

    public static void main(String[] args) {
        try {
            daemonize();
        } catch (Throwable e) {
            LOG.error("Startup failed. " + e.getMessage());
        }

        try {
            start();
        } catch (SchedulerException | RemoteException | AlreadyBoundException e) {
            LOG.error("Can't start daemon. " + e.getMessage());
            stop();
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

    private static void stop() {
        try {
            updateManager.destroy();
        } catch (SchedulerException e) {
            LOG.error("Can't stop Update Manager.", e);
        }

        try {
            Registry registry = LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
            registry.unbind(InstallationManager.class.getSimpleName());
        } catch (RemoteException | NotBoundException e) {
            LOG.error("Can't unbind RMI sever. " + e.getMessage());
        }
    }

    private static void start() throws SchedulerException, RemoteException, AlreadyBoundException {
        Registry registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        registry.bind(InstallationManager.class.getSimpleName(), injector.getInstance(InstallationManager.class));

        updateManager.init();
    }

    private static Injector createInjector() {
        final Pattern envPattern = Pattern.compile("\\$\\{([^\\}]*)\\}"); // ${...}

        return Guice.createInjector(new Module() {
            @Override
            public void configure(Binder binder) {
                bindProperties(binder);
                binder.bind(InstallationManager.class).to(InstallationManagerImpl.class);
                Multibinder.newSetBinder(binder, Artifact.class).addBinding().to(InstallManagerArtifact.class);
            }

            private void bindProperties(Binder binder) {
                Properties properties = new Properties();
                try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("codenvy/installation-manager.properties")) {
                    properties.load(in);
                } catch (IOException e) {
                    LOG.error("Can't load properties. " + e.getMessage());
                }

                for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                    String key = (String)entry.getKey();
                    String value = replaceEnvVariables((String)entry.getValue());

                    binder.bindConstant().annotatedWith(Names.named(key)).to(value);
                }
            }

            /**
             * Replaces environment variables by them actual values using ${...} template.
             */
            private String replaceEnvVariables(String value) {
                Matcher matcher = envPattern.matcher(value);
                if (matcher.find()) {
                    String envVar = matcher.group(1);
                    return value.substring(0, matcher.start(1) - 2) + System.getenv(envVar) + value.substring(matcher.end(1) + 1);
                } else {
                    return value;
                }
            }
        });
    }
}
