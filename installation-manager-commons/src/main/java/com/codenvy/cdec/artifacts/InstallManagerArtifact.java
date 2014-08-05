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
package com.codenvy.cdec.artifacts;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class InstallManagerArtifact extends AbstractArtifact {

    public static final String NAME = "installation-manager";

    @Inject
    public InstallManagerArtifact() {
        super(NAME);
    }

    @Override
    public String getCurrentVersion() throws IOException {
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

    @Override
    public boolean isValidSubscriptionRequired() {
        return false;
    }
}
