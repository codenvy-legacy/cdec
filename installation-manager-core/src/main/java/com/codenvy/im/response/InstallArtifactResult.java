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

package com.codenvy.im.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * @author Anatoliy Bazko
 */
@JsonPropertyOrder({"artifact", "version", "status"})
public class InstallArtifactResult {
    private String                artifact;
    private String                version;
    private InstallArtifactStatus status;

    public InstallArtifactResult() {
    }

    public String getArtifact() {
        return artifact;
    }

    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public InstallArtifactStatus getStatus() {
        return status;
    }

    public void setStatus(InstallArtifactStatus status) {
        this.status = status;
    }

    public InstallArtifactResult withArtifact(String artifact) {
        this.artifact = artifact;
        return this;
    }

    public InstallArtifactResult withVersion(String version) {
        this.version = version;
        return this;
    }

    public InstallArtifactResult withStatus(InstallArtifactStatus status) {
        this.status = status;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InstallArtifactResult)) return false;

        InstallArtifactResult that = (InstallArtifactResult)o;

        if (artifact != null ? !artifact.equals(that.artifact) : that.artifact != null) return false;
        if (status != that.status) return false;
        if (version != null ? !version.equals(that.version) : that.version != null) return false;

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int result = artifact != null ? artifact.hashCode() : 0;
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        return result;
    }
}
