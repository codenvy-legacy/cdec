/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
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
package com.codenvy.im.utils;

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.service.InstallationManager;
import com.codenvy.im.service.InstallationManagerImpl;
import com.codenvy.im.service.InstallationManagerService;
import com.codenvy.im.service.InstallationManagerServiceImpl;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Anatoliy Bazko
 */
public class InjectorBootstrap {

    public static final  Injector            INJECTOR;
    private static final Map<String, String> boundProperties;

    static {
        boundProperties = new HashMap<>();
        INJECTOR = createInjector();
    }

    private static Injector createInjector() {
        final Pattern envPattern = Pattern.compile("\\$\\{([^\\}]*)\\}"); // ${...}

        return Guice.createInjector(new Module() {
            @Override
            public void configure(Binder binder) {
                bindProperties(binder);

                binder.bind(InstallationManagerService.class).to(InstallationManagerServiceImpl.class);
                binder.bind(InstallationManager.class).to(InstallationManagerImpl.class);
                Multibinder.newSetBinder(binder, Artifact.class).addBinding().to(InstallManagerArtifact.class);
                Multibinder.newSetBinder(binder, Artifact.class).addBinding().to(CDECArtifact.class);
            }

            private void bindProperties(Binder binder) {
                bindProperties("codenvy/update-server.properties");
                bindProperties("codenvy/installation-manager.properties");

                // TODO [AB]
//                Path conf = InstallationManagerConfig.getConfFile();
//                if (exists(conf)) {
//                    try (InputStream in = newInputStream(conf)) {
//                        doBindProperties(in);
//                    } catch (IOException e) {
//                        throw new IllegalStateException("Can't load properties", e);
//                    }
//                }

                for (Map.Entry<String, String> e : boundProperties.entrySet()) {
                    binder.bindConstant().annotatedWith(Names.named(e.getKey())).to(e.getValue());
                }
            }

            private void bindProperties(String fileProperties) {
                try (InputStream in = InjectorBootstrap.class.getClassLoader().getResourceAsStream(fileProperties)) {
                    if (in != null) {
                        doBindProperties(in);
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Can't load properties", e);
                }
            }

            private void doBindProperties(InputStream in) {
                Properties properties = new Properties();
                try {
                    properties.load(in);
                } catch (IOException e) {
                    throw new IllegalStateException("Can't load properties", e);
                }

                for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                    String key = (String)entry.getKey();
                    String value = replaceEnvVariables((String)entry.getValue());

                    boundProperties.put(key, value);
                }
            }

            /** Replaces environment variables by them actual values using ${...} template. */
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
