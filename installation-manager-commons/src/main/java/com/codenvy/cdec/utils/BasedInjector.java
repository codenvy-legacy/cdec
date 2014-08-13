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
package com.codenvy.cdec.utils;

import com.codenvy.cdec.artifacts.Artifact;
import com.codenvy.cdec.artifacts.CDECArtifact;
import com.codenvy.cdec.artifacts.InstallManagerArtifact;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Anatoliy Bazko
 */
public class BasedInjector {

    private static final Injector INJECTOR;


    static {
        INJECTOR = createInjector();
    }

    public static Injector getInstance() {
        return INJECTOR;
    }

    private static Injector createInjector() {
        final Pattern envPattern = Pattern.compile("\\$\\{([^\\}]*)\\}"); // ${...}

        return Guice.createInjector(new Module() {
            @Override
            public void configure(Binder binder) {
                bindProperties(binder);
                Multibinder.newSetBinder(binder, Artifact.class).addBinding().to(InstallManagerArtifact.class);
                Multibinder.newSetBinder(binder, Artifact.class).addBinding().to(CDECArtifact.class);
            }

            private void bindProperties(Binder binder) {
                Properties properties = new Properties();
                try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("codenvy/installation-manager.properties")) {
                    properties.load(in);
                } catch (IOException e) {
                    throw new IllegalStateException("Can't load properties", e);
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
