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

import com.codenvy.cdec.im.InstallationManagerImpl;
import com.codenvy.cdec.im.UpdateManager;
import com.codenvy.cdec.server.InstallationManager;
import com.codenvy.cdec.utils.BasedInjector;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

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
        } catch (Exception e) {
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
        } catch (RemoteException e) {
            LOG.error("Can't unbind RMI sever. " + e.getMessage());
        } catch (NotBoundException e) {
            // do nothing
        }
    }

    private static void start() throws RemoteException, AlreadyBoundException, SchedulerException {
        updateManager.init();

        Registry registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        registry.bind(InstallationManager.class.getSimpleName(), injector.getInstance(InstallationManager.class));
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
