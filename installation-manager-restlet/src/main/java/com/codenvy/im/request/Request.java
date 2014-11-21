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

import com.codenvy.im.exceptions.ArtifactNotFoundException;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.user.UserCredentials;
import com.codenvy.im.utils.Version;

import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.resource.ResourceException;

import java.io.IOException;
import java.net.HttpURLConnection;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;

/** @author Dmytro Nochevnov */
public class Request {
    private InstallOptions  installOptions;
    private UserCredentials userCredentials;
    private String          artifactName;
    private String          version;

    public Request() {
    }

    /** Factory */
    public static Request fromRepresentation(JacksonRepresentation<Request> representation) throws ResourceException, IOException {
        if (representation == null) {
            throw new ResourceException(HttpURLConnection.HTTP_BAD_REQUEST, "Request is incomplete. Request is empty.", "", "");
        }
        return representation.getObject();
    }

    public JacksonRepresentation<Request> toRepresentation() {
        return new JacksonRepresentation<>(this);
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

    /** Validates requests depending on {@link Request.ValidationType} */
    public void validate(int validationType) throws ResourceException, ArtifactNotFoundException {
        if ((validationType & ValidationType.CREDENTIALS) == ValidationType.CREDENTIALS) {
            if (userCredentials == null) {
                throw new ResourceException(HttpURLConnection.HTTP_BAD_REQUEST, "Request is incomplete. User credentials are missed.", "", "");
            }
        }

        if ((validationType & ValidationType.ARTIFACT) == ValidationType.ARTIFACT) {
            if (artifactName == null || artifactName.isEmpty()) {
                throw new ResourceException(HttpURLConnection.HTTP_BAD_REQUEST, "Request is incomplete. Artifact name is missed.", "", "");
            } else {
                createArtifact(artifactName); // checks if artifact name is valid
            }

            if (version != null) {
                Version.valueOf(version); // checks if version is valid
            }
        }

        if ((validationType & ValidationType.INSTALL_OPTIONS) == ValidationType.INSTALL_OPTIONS) {
            if (installOptions == null) {
                throw new ResourceException(HttpURLConnection.HTTP_BAD_REQUEST, "Request is incomplete. Installation options are missed.", "", "");
            }
        }
    }

    /** Validation types */
    public static class ValidationType {
        public static final int CREDENTIALS     = 0b001;
        public static final int ARTIFACT        = 0b010;
        public static final int INSTALL_OPTIONS = 0b100;
        public static final int FULL            = 0b111;
    }
}
