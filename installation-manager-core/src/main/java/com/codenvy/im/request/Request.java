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
package com.codenvy.im.request;

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.exceptions.ArtifactNotFoundException;
import com.codenvy.im.facade.UserCredentials;
import com.codenvy.im.utils.Version;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;

/**
 * Aggregated request to {@link com.codenvy.im.facade.InstallationManagerFacade} from CLI.
 * Artifact name and version are user's entered options.
 *
 * @author Anatoliy Bazko
 */
public class Request {
    private UserCredentials userCredentials;
    private String          artifactName;
    private String          version;

    public Request() {
    }

    public Request setUserCredentials(UserCredentials userCredentials) {
        this.userCredentials = userCredentials;
        return this;
    }

    @Nonnull
    public String getAccessToken() {
        return userCredentials.getToken();
    }

    @Nonnull
    public String getAccountId() {
        return userCredentials.getAccountId();
    }

    @Nonnull
    public UserCredentials getUserCredentials() {
        return userCredentials;
    }

    /**
     * @return {@link com.codenvy.im.artifacts.Artifact} or null
     * @throws ArtifactNotFoundException
     *         if artifact name is wrong
     */
    @Nullable
    public Artifact getArtifact() throws ArtifactNotFoundException {
        return artifactName == null ? null : createArtifact(artifactName);
    }

    public Request setArtifactName(String artifactName) {
        this.artifactName = artifactName;
        return this;
    }

    /**
     * @return {@link com.codenvy.im.utils.Version} or null
     * @throws java.lang.IllegalArgumentException
     */
    @Nullable
    public Version getVersion() {
        return version == null ? null : Version.valueOf(version);
    }

    public Request setVersion(String version) {
        this.version = version;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Request)) return false;

        Request request = (Request)o;

        if (artifactName != null ? !artifactName.equals(request.artifactName) : request.artifactName != null) return false;
        if (!userCredentials.equals(request.userCredentials)) return false;
        if (version != null ? !version.equals(request.version) : request.version != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = userCredentials.hashCode();
        result = 31 * result + (artifactName != null ? artifactName.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }
}

