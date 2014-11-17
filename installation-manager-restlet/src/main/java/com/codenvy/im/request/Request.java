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
package com.codenvy.im.request;

import com.codenvy.commons.json.JsonParseException;
import com.codenvy.im.installer.InstallOptions;
import com.codenvy.im.user.UserCredentials;

import org.restlet.ext.jackson.JacksonRepresentation;

import java.io.IOException;

/**
 * @author Dmytro Nochevnov
 */
public class Request {
    private InstallOptions  installOptions;
    private UserCredentials userCredentials;
    private String          artifactName;
    private String          version;

    public Request() {
    }

    public static Request fromRepresentation(JacksonRepresentation<Request> representation) throws JsonParseException, IOException {
        return representation.getObject();
    }

    public JacksonRepresentation<Request> toRepresentation() {
        return new JacksonRepresentation<Request>(this);
    }

    public InstallOptions getInstallOptions() {
        return installOptions;
    }

    public Request setInstallOptions(InstallOptions installOptions) {
        this.installOptions = installOptions;
        return this;
    }

    public UserCredentials getUserCredentials() {
        return userCredentials;
    }

    public Request setUserCredentials(UserCredentials userCredentials) {
        this.userCredentials = userCredentials;
        return this;
    }

    public String getArtifactName() {
        return artifactName;
    }

    public Request setArtifactName(String artifactName) {
        this.artifactName = artifactName;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public Request setVersion(String version) {
        this.version = version;
        return this;
    }
}
