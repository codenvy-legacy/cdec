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

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.VersionLabel;
import com.codenvy.im.utils.Version;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.nio.file.Path;

/**
 * @author Anatoliy Bazko
 */
@JsonPropertyOrder({"artifact", "version", "label", "file", "status"})
public class DownloadArtifactInfo extends BasicArtifactInfo {
    private String                 artifact;
    private String                 version;
    private String                 file;
    private VersionLabel label;
    private DownloadArtifactStatus status;

    public DownloadArtifactInfo() {
    }

    public DownloadArtifactInfo(Artifact artifact, Version version, Path file, DownloadArtifactStatus status) {
        this.artifact = artifact.getName();
        this.version = version.toString();
        this.file = (file != null) ? file.toString() : null;
        this.status = status;
    }

    public DownloadArtifactInfo(Artifact artifact, Version version, DownloadArtifactStatus status) {
        this(artifact, version, null, status);
    }

    public String getArtifact() {
        return artifact;
    }

    public String getVersion() {
        return version;
    }

    public DownloadArtifactStatus getStatus() {
        return status;
    }

    public String getFile() {
        return file;
    }

    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public void setStatus(DownloadArtifactStatus status) {
        this.status = status;
    }

    public VersionLabel getLabel() {
        return label;
    }

    public void setLabel(VersionLabel label) {
        this.label = label;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DownloadArtifactInfo)) return false;

        DownloadArtifactInfo result = (DownloadArtifactInfo)o;

        if (artifact != null ? !artifact.equals(result.artifact) : result.artifact != null) return false;
        if (file != null ? !file.equals(result.file) : result.file != null) return false;
        if (label != result.label) return false;
        if (status != result.status) return false;
        if (version != null ? !version.equals(result.version) : result.version != null) return false;

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int result = artifact != null ? artifact.hashCode() : 0;
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (file != null ? file.hashCode() : 0);
        result = 31 * result + (label != null ? label.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        return result;
    }
}
