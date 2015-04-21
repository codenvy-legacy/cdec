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
import com.codenvy.im.artifacts.ArtifactFactory;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.exceptions.ArtifactNotFoundException;
import com.codenvy.im.facade.UserCredentials;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.utils.Version;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Aggregated request to {@link com.codenvy.im.facade.InstallationManagerFacade}.
 *
 * @author Anatoliy Bazko
 */
public class Request {
    @ApiModelProperty(notes = "It is needed for getting and downloading 'codenvy' artifact.")
    private UserCredentials userCredentials;

    private String artifactName;
    private String version;

    @ApiModelProperty(notes = "Required for all install commands")
    private InstallOptions installOptions;

    public Request() {
    }

    public Request setUserCredentials(UserCredentials userCredentials) {
        this.userCredentials = userCredentials;
        return this;
    }

    /**
     * @return userCredentials.getToken(), or "" if userCredentials == null
     */
    @Nonnull
    public String obtainAccessToken() {
        if (userCredentials == null) {
            return "";
        } else {
            return userCredentials.getToken();
        }
    }

    /**
     * @return userCredentials.getAccountId(), or "" if userCredentials == null
     */
    @Nonnull
    public String obtainAccountId() {
        if (userCredentials == null) {
            return "";
        } else {
            return userCredentials.getAccountId();
        }
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
    public Artifact createArtifact() throws ArtifactNotFoundException {
        return artifactName == null ? null : ArtifactFactory.createArtifact(artifactName);
    }

    @ApiModelProperty(notes = "Default artifact name = 'codenvy'", allowableValues = CDECArtifact.NAME + "," + InstallManagerArtifact.NAME)
    public Request setArtifactName(String artifactName) {
        this.artifactName = artifactName;
        return this;
    }

    public String getArtifactName() {
        return artifactName;
    }

    /**
     * @return {@link com.codenvy.im.utils.Version} or null
     * @throws java.lang.IllegalArgumentException
     */
    @Nullable
    public Version createVersion() {
        return version == null ? null : Version.valueOf(version);
    }

    @ApiModelProperty(notes = "Default version could be the installed artifact version.")
    public Request setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getVersion() {
        return version;
    }

    /**
     * @return {@link com.codenvy.im.install.InstallOptions} or null
     */
    @Nullable
    public InstallOptions getInstallOptions() {
        return installOptions;
    }

    public Request setInstallOptions(InstallOptions installOptions) {
        this.installOptions = installOptions;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Request)) return false;

        Request request = (Request)o;

        if (userCredentials != null ? !userCredentials.equals(request.userCredentials) : request.userCredentials != null) return false;
        if (artifactName != null ? !artifactName.equals(request.artifactName) : request.artifactName != null) return false;
        if (version != null ? !version.equals(request.version) : request.version != null) return false;
        if (installOptions != null ? !installOptions.equals(request.installOptions) : request.installOptions != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = userCredentials != null ? userCredentials.hashCode() : 0;
        result = 31 * result + (artifactName != null ? artifactName.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (installOptions != null ? installOptions.hashCode() : 0);
        return result;
    }
}

