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
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Anatoliy Bazko
 */
public enum Artifact {

    INSTALL_MANAGER("install-manager") {
        @Override
        public String getVersion() throws IOException {
            try (InputStream in = Artifact.class.getClassLoader().getResourceAsStream("codenvy/BuildInfo.properties")) {
                Properties props = new Properties();
                props.load(in);

                if (props.containsKey("version")) {
                    return (String)props.get("version");
                } else {
                    throw new IOException("Property version not found");
                }
            }
        }
    },
    CDEC("cdec") {
        @Override
        public String getVersion() {
            return null;
        }
    },
    PUPPET_MASTER("puppet-master") {
        @Override
        public String getVersion() {
            return null;
        }
    },
    PUPPET_CLIENT("puppet-client") {
        @Override
        public String getVersion() {
            return null;
        }
    };

    private final String name;

    private Artifact(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public abstract String getVersion() throws IOException;
}
