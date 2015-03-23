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
package com.codenvy.im.service;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;

// TODO [AB] get rid of

/** @author Anatoliy Bazko */
public class InstallationManagerConfig {
    public static Path CONFIG_FILE = Paths.get(System.getenv("HOME"), ".codenvy", "im.properties");

    public static final String CODENVY_HOST_DNS = "codenvy_host_dns";
    public static final String PUPPET_MASTER_HOST_NAME = "puppet_master_host_name";

    public InstallationManagerConfig() {
    }

    public static void storeProperty(String property, String value) throws IOException {
        Path confFile = getConfFile();
        Properties props = readProperties(confFile);
        props.put(property, value);
        try (OutputStream out = newOutputStream(confFile)) {
            props.store(out, null);
        }
    }

    protected static Properties readProperties(Path conf) throws IOException {
        Properties props = new Properties();
        if (exists(conf)) {
            try (InputStream in = newInputStream(conf)) {
                if (in != null) {
                    props.load(in);
                } else {
                    throw new IOException("Can't store property into configuration");
                }
            }
        }
        return props;
    }

    @Nullable
    public static String readCdecHostDns() throws IOException {
        return readProperty(CODENVY_HOST_DNS);
    }

    @Nullable
    public static String readPuppetMasterNodeDns() throws IOException {
        return readProperty(PUPPET_MASTER_HOST_NAME);
    }

    @Nullable
    private static String readProperty(String property) throws IOException {
        Properties props = readProperties(CONFIG_FILE);
        return (String)props.get(property);
    }

    /** @return configuration file path. Insure directory with conf file is existed. */
    public static Path getConfFile() {
        Path confFile = CONFIG_FILE;
        if (!exists(confFile.getParent())) {
            try {
                createDirectories(confFile.getParent());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        return confFile;
    }
}
