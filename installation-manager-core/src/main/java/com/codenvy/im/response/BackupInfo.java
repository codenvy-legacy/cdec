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

import com.codenvy.im.backup.BackupConfig;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** @author Dmytro Nochevnov */
@JsonPropertyOrder({"file", "artifactInfo", "status"})
public class BackupInfo {
    private ArtifactInfo artifactInfo;
    private String       file;
    private Status       status;

    public Status getStatus() {
        return status;
    }

    public BackupInfo setStatus(Status status) {
        this.status = status;
        return this;
    }

    public ArtifactInfo getArtifactInfo() {
        return artifactInfo;
    }

    public BackupInfo setArtifactInfo(ArtifactInfo artifactInfo) {
        this.artifactInfo = artifactInfo;
        return this;
    }

    public String getFile() {
        return file;
    }

    public BackupInfo setFile(String file) {
        this.file = file;
        return this;
    }

    public static BackupInfo createSuccessInfo(BackupConfig config) {
        ArtifactInfo artifactInfo = new ArtifactInfo().setArtifact(config.getArtifactName()).setVersion(config.getArtifactVersion());
        return new BackupInfo().setArtifactInfo(artifactInfo)
                               .setFile(config.getBackupFile() != null ? config.getBackupFile().toString() : null)
                               .setStatus(Status.SUCCESS);
    }

    public static BackupInfo createFailureInfo(BackupConfig config) {
        ArtifactInfo artifactInfo = new ArtifactInfo().setArtifact(config.getArtifactName()).setVersion(config.getArtifactVersion());
        return new BackupInfo().setArtifactInfo(artifactInfo)
                               .setFile(config.getBackupFile() != null ? config.getBackupFile().toString() : null)
                               .setStatus(Status.FAILURE);
    }

}
